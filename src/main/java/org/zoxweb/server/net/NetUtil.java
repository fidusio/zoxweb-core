/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.server.net;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.util.ServerUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.net.*;
import org.zoxweb.shared.net.InetProp.*;
import org.zoxweb.shared.security.SecConst;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SUS;

import java.io.Closeable;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Server side networking helpers: wildcard and private addresses, network interface
 * lookups (IPv4/IPv6 addresses, CIDR blocks, status, Linux boot protocol), network
 * and netmask arithmetic, ICMP ping, inbound security checks against an
 * {@link InetFilterRulesManager}, Linux routing/bonding script generation and
 * {@link SelectionKey} cleanup for the NIO stack.
 * <p>
 * All the methods are static and the class can't be instantiated. Pure address
 * arithmetic that needs no JVM networking lives in {@link SharedNetUtil}.
 */
public class NetUtil {

    public final static LogWrapper log = new LogWrapper(NetUtil.class).setEnabled(true);

    private NetUtil() {
    }

    private static volatile InetAddress INET_V4_ZERO = null;
    private static volatile InetAddress INET_V6_ZERO = null;

    /**
     * Returns the wildcard address, <code>0.0.0.0</code> for IPv4 or <code>::</code> for IPv6.
     * <p>
     * Both addresses are resolved once on first use and cached, so the call is cheap afterwards.
     *
     * @param ipv6 true for the IPv6 wildcard, false for the IPv4 one
     * @return the wildcard address
     * @throws IOException if the literal address can't be converted
     */
    public static InetAddress inetAddressZero(boolean ipv6) throws IOException {

        // for fast response
        if (ipv6 && INET_V6_ZERO != null) {
            return INET_V6_ZERO;
        }
        // for fast response
        if (!ipv6 && INET_V4_ZERO != null) {
            return INET_V4_ZERO;
        }
        if (INET_V4_ZERO == null || INET_V6_ZERO == null) {
            ServerUtil.LOCK.lock();

            try {
                if (INET_V4_ZERO == null)
                    INET_V4_ZERO = InetAddress.getByName("0.0.0.0");
                if (INET_V6_ZERO == null)
                    INET_V6_ZERO = InetAddress.getByName("::");
            } finally {
                ServerUtil.LOCK.unlock();
            }
        }

        return ipv6 ? INET_V6_ZERO : INET_V4_ZERO;
    }

    /**
     * Checks a host against the inbound filter rules and closes the connection if it is not allowed.
     * <p>
     * A null rules manager allows everything. The closeable is closed only when the verdict is
     * not {@link SecConst.SecAction#ALLOW}; a null closeable is tolerated.
     *
     * @param ifrm the filter rules manager, null to allow
     * @param host the host name or IP address to check
     * @param sc   the connection to close on denial, can be null
     * @return the security action for the host
     * @throws IOException if the host can't be resolved
     */
    public static SecConst.SecAction checkSecurityStatus(InetFilterRulesManager ifrm, String host, Closeable sc) throws IOException {

        SecConst.SecAction ret = SecConst.SecAction.ALLOW;
        if (ifrm != null) {
            ret = ifrm.lookupSecurityStatus(host);

            if (ret != SecConst.SecAction.ALLOW) {
                SharedIOUtil.close(sc);
            }
        }


        return ret;
    }

    /**
     * Returns all the network interfaces of this machine, up or down, loopback included.
     *
     * @return the interfaces in the order the system reports them
     * @throws SocketException if the interfaces can't be read
     */
    public static NetworkInterface[] allInterfaces() throws SocketException {
        List<NetworkInterface> ret = new ArrayList<>();
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
            ret.add(en.nextElement());
        }
        return ret.toArray(new NetworkInterface[0]);
    }


    /**
     * Checks a socket address against the inbound filter rules and closes the connection if it is not allowed.
     * <p>
     * A null rules manager allows everything. The closeable is closed only when it is not null
     * and the verdict is not {@link SecConst.SecAction#ALLOW}.
     *
     * @param ifrm the filter rules manager, null to allow
     * @param host the socket address to check
     * @param sc   the connection to close on denial, can be null
     * @return the security action for the address
     */
    public static SecConst.SecAction checkSecurityStatus(InetFilterRulesManager ifrm, SocketAddress host, Closeable sc) {
        SecConst.SecAction ret = SecConst.SecAction.ALLOW;
        if (ifrm != null) {
            ret = ifrm.lookupSecurityStatus(host);
            if (sc != null && ret != SecConst.SecAction.ALLOW) {
                SharedIOUtil.close(sc);
            }
        }

        return ret;
    }

    /**
     * Checks the remote peer of a socket channel against the inbound filter rules and closes
     * the connection if it is not allowed.
     * <p>
     * A null rules manager allows everything. The closeable is closed only when it is not null
     * and the verdict is not {@link SecConst.SecAction#ALLOW}.
     *
     * @param ifrm    the filter rules manager, null to allow
     * @param channel the connected channel whose remote address is checked
     * @param sc      the connection to close on denial, can be null
     * @return the security action for the remote peer
     * @throws IOException          if the remote address can't be read
     * @throws NullPointerException if channel is null and ifrm is not
     */
    public static SecConst.SecAction checkSecurityStatus(InetFilterRulesManager ifrm, SocketChannel channel, Closeable sc) throws IOException {
        SecConst.SecAction ret = SecConst.SecAction.ALLOW;
        if (ifrm != null) {
            ret = ifrm.lookupSecurityStatus(channel.getRemoteAddress());
            if (sc != null && ret != SecConst.SecAction.ALLOW) {
                SharedIOUtil.close(sc);
            }
        }

        return ret;
    }

    /**
     * Checks an address against the inbound filter rules and closes the connection if it is not allowed.
     * <p>
     * A null rules manager allows everything. The closeable is closed only when it is not null
     * and the verdict is not {@link SecConst.SecAction#ALLOW}.
     *
     * @param ifrm the filter rules manager, null to allow
     * @param host the address to check
     * @param sc   the connection to close on denial, can be null
     * @return the security action for the address
     */
    public static SecConst.SecAction checkSecurityStatus(InetFilterRulesManager ifrm, InetAddress host, Closeable sc) {
        SecConst.SecAction ret = SecConst.SecAction.ALLOW;
        if (ifrm != null) {
            ret = ifrm.lookupSecurityStatus(host);
            if (sc != null && ret != SecConst.SecAction.ALLOW) {
                SharedIOUtil.close(sc);
            }
        }


        return ret;
    }

    /**
     * Returns the main IPv4 address of a network interface looked up by name.
     * <p>
     * The main address is the last IPv4 address the interface reports.
     *
     * @param ni the network interface name (e.g. "eth0")
     * @return the main IPv4 address, null if the interface has no IPv4 address
     * @throws IOException          if the interface can't be read
     * @throws NullPointerException if ni is null or no interface has that name
     * @see #getIPV4MainAddress(NetworkInterface)
     */
    public static Inet4Address getIPV4MainAddress(String ni) throws IOException {
        return getIPV4MainAddress(NetworkInterface.getByName(ni));

    }


    /**
     * Resolves a host name and returns its first IPv4 address.
     *
     * @param host the host name or IP literal to resolve
     * @return the first IPv4 address of the host, null if it only resolves to IPv6 addresses
     * @throws UnknownHostException if the host can't be resolved
     */
    public static Inet4Address getInet4Address(String host) throws UnknownHostException {
        //log.info("Host:" + host);
        InetAddress[] all = InetAddress.getAllByName(host);
        for (InetAddress ip : all) {
            if (ip instanceof Inet4Address) {
                return (Inet4Address) ip;
            }
        }

        //log.info("no IP v4 for host:" + host);
        return null;
    }

    /**
     * Returns the main IPv4 address of a network interface.
     * <p>
     * The main address is the last IPv4 address the interface reports.
     *
     * @param ni the network interface
     * @return the main IPv4 address, null if the interface has no IPv4 address
     * @throws IOException          if the interface can't be read
     * @throws NullPointerException if ni is null
     * @see #getIPV4AllAddresses(NetworkInterface)
     */
    public static Inet4Address getIPV4MainAddress(NetworkInterface ni)
            throws IOException {
        Inet4Address[] addresses = getIPV4AllAddresses(ni);

        if (addresses != null && addresses.length > 0) {
            return addresses[addresses.length - 1];
        }

        return null;
    }


    /**
     * Checks whether an IP address belongs to the network described by a filter entry.
     * <p>
     * The address is masked with the entry's network mask and compared to the entry's
     * network. Without a mask the address itself is compared to the network.
     *
     * @param ipf       the filter entry holding the network and its mask
     * @param ipAddress the IP address to check
     * @return true if the address is inside the filter's network
     * @throws IOException          if the address or the mask can't be resolved
     * @throws NullPointerException if ipf is null
     * @see #getNetworkIPV4(String, String)
     */
    public static boolean belongsToNetwork(InetFilterInfo ipf, String ipAddress)
            throws IOException {
        String tempNetwork = getNetworkIPV4(ipAddress, ipf.getNetworkMask());
        return tempNetwork.equals(ipf.getNetwork());
    }


    /**
     * Checks whether an IP address belongs to a network given as raw bytes.
     * <p>
     * The resolved address is masked with the network mask and compared byte for byte to
     * the network. Without a mask the address itself is compared to the network.
     *
     * @param network     the network address bytes
     * @param networkMask the network mask bytes, null to compare the address as is
     * @param ipAddress   the IP address to check
     * @return true if the address is inside the network
     * @throws IOException          if the address can't be resolved
     * @throws NullPointerException if network or ipAddress is null
     * @see SharedNetUtil#getNetwork(byte[], byte[])
     */
    public static boolean belongsToNetwork(byte[] network, byte[] networkMask, String ipAddress)
            throws IOException {
        SUS.checkIfNulls("Network or IP adress can't be null", network, ipAddress);
        byte[] tempNetwork = null;
        if (networkMask != null) {
            tempNetwork = SharedNetUtil.getNetwork(InetAddress.getByName(ipAddress).getAddress(), networkMask);
        } else {
            tempNetwork = InetAddress.getByName(ipAddress).getAddress();
        }
        return Arrays.equals(network, tempNetwork);

    }


    /**
     * Maps the shared {@link ProxyType} to the JDK {@link Proxy.Type}.
     *
     * @param pt the proxy type, can be null
     * @return the matching JDK proxy type, {@link Proxy.Type#DIRECT} if pt is null
     */
    public static Proxy.Type lookup(ProxyType pt) {
        if (pt != null) {
            switch (pt) {
                case DIRECT:
                    return Proxy.Type.DIRECT;
                case HTTP:
                    return Proxy.Type.HTTP;
                case SOCKS:
                    return Proxy.Type.SOCKS;


            }
        }

        return Proxy.Type.DIRECT;
    }


    /**
     * Returns all the IPv4 addresses of a network interface looked up by name.
     *
     * @param ni the network interface name (e.g. "eth0")
     * @return the IPv4 addresses in the order the interface reports them, empty array if none
     * @throws IOException          if the interface can't be read
     * @throws NullPointerException if ni is null or no interface has that name
     * @see #getIPV4AllAddresses(NetworkInterface)
     */
    public static Inet4Address[] getIPV4AllAddresses(String ni)
            throws IOException {
        return getIPV4AllAddresses(NetworkInterface.getByName(ni));

    }

    /**
     * Returns all the IPv4 addresses of a network interface, IPv6 addresses are skipped.
     *
     * @param ni the network interface
     * @return the IPv4 addresses in the order the interface reports them, empty array if none
     * @throws IOException          if the interface can't be read
     * @throws NullPointerException if ni is null
     */
    public static Inet4Address[] getIPV4AllAddresses(NetworkInterface ni)
            throws IOException {
        SUS.checkIfNull("Network or IP address can't be null", ni);
        ArrayList<Inet4Address> v = new ArrayList<Inet4Address>();
        for (Enumeration<InetAddress> e = ni.getInetAddresses(); e.hasMoreElements(); ) {
            InetAddress addr = e.nextElement();
            if (addr instanceof Inet4Address)
                v.add((Inet4Address) addr);
        }

        return v.toArray(new Inet4Address[0]);
    }


    /**
     * Returns the main IPv6 address of a network interface looked up by name.
     * <p>
     * The main address is the last IPv6 address the interface reports.
     *
     * @param ni the network interface name (e.g. "eth0")
     * @return the main IPv6 address, null if the interface has no IPv6 address
     * @throws IOException          if the interface can't be read
     * @throws NullPointerException if ni is null or no interface has that name
     * @see #getIPV6MainAddress(NetworkInterface)
     */
    public static Inet6Address getIPV6MainAddress(String ni) throws IOException {
        return getIPV6MainAddress(NetworkInterface.getByName(ni));

    }

    /**
     * Returns the main IPv6 address of a network interface.
     * <p>
     * The main address is the last IPv6 address the interface reports.
     *
     * @param ni the network interface
     * @return the main IPv6 address, null if the interface has no IPv6 address
     * @throws IOException          if the interface can't be read
     * @throws NullPointerException if ni is null
     * @see #getIPV6AllAddresses(NetworkInterface)
     */
    public static Inet6Address getIPV6MainAddress(NetworkInterface ni)
            throws IOException {
        Inet6Address[] addresses = getIPV6AllAddresses(ni);

        if (addresses != null && addresses.length > 0) {
            return addresses[addresses.length - 1];
        }

        return null;
    }

    /**
     * Parses an <code>address:port</code> string, or a URL, into a socket address.
     * <p>
     * The text is parsed by {@link IPAddress#parse(String)}, so every form it accepts
     * (host:port, host:port:proxyType, URL with scheme default port) is accepted here.
     * The host part is resolved when the socket address is created.
     *
     * @param addressPort the address and port to parse
     * @return the socket address, unresolved if the host name is unknown
     * @throws IllegalArgumentException if the text is malformed or the port is out of range
     * @throws NullPointerException     if addressPort is null
     */
    public static InetSocketAddress parse(String addressPort) {
        IPAddress ipAddress = IPAddress.parse(addressPort);
        return new InetSocketAddress(ipAddress.getInetAddress(), ipAddress.getPort());
    }

    /**
     * Returns all the IPv6 addresses of a network interface looked up by name.
     *
     * @param ni the network interface name (e.g. "eth0")
     * @return the IPv6 addresses in the order the interface reports them, empty array if none
     * @throws IOException          if the interface can't be read
     * @throws NullPointerException if ni is null or no interface has that name
     * @see #getIPV6AllAddresses(NetworkInterface)
     */
    public static Inet6Address[] getIPV6AllAddresses(String ni)
            throws IOException {
        return getIPV6AllAddresses(NetworkInterface.getByName(ni));

    }

    /**
     * Returns all the IPv6 addresses of a network interface, IPv4 addresses are skipped.
     *
     * @param ni the network interface
     * @return the IPv6 addresses in the order the interface reports them, empty array if none
     * @throws IOException          if the interface can't be read
     * @throws NullPointerException if ni is null
     */
    public static Inet6Address[] getIPV6AllAddresses(NetworkInterface ni)
            throws IOException {
        SUS.checkIfNull("Network or IP address can't be null", ni);
        ArrayList<Inet6Address> v = new ArrayList<Inet6Address>();
        for (Enumeration<InetAddress> e = ni.getInetAddresses(); e.hasMoreElements(); ) {
            InetAddress addr = e.nextElement();
            if (addr instanceof Inet6Address)
                v.add((Inet6Address) addr);
        }

        return v.toArray(new Inet6Address[0]);


    }

    /**
     * Returns the IPv4 CIDR blocks a network interface is attached to, one
     * <code>network/prefix</code> entry per subnet (e.g. "192.168.1.0/24").
     * <p>
     * Every IPv4 address bound to the interface is converted to its block, IPv6
     * addresses are skipped. Addresses of the same subnet yield a single block and
     * the blocks are returned in the order the interface reports its addresses.
     * <p>
     * The interface must be up and must not be a loopback.
     *
     * @param nif the network interface to look up
     * @return the IPv4 CIDR blocks of the interface, <code>null</code> if there is nothing
     * on the network interface (no IPv4 address is bound to it)
     * @throws IOException          if the interface is not up, is a loopback, or its state can't be read
     * @throws NullPointerException if nif is null
     * @see SharedNetUtil#toCIDRBlock(byte[], short)
     */
    public static String[] toV4CIDRBlocks(NetworkInterface nif) throws IOException {
        SUS.checkIfNull("Network or IP address can't be null", nif);
        if (!nif.isUp() || nif.isLoopback()) throw new IOException(nif + " is not up or it is a loopback");

        Set<String> cidrBlocks = _toV4CIDRBlocks(nif, new LinkedHashSet<>());

        return cidrBlocks.isEmpty() ? null : cidrBlocks.toArray(new String[0]);
    }

    /**
     * Returns the IPv4 CIDR blocks of all the network interfaces of this machine, one
     * <code>network/prefix</code> entry per subnet (e.g. "192.168.1.0/24").
     * <p>
     * Interfaces that are down or loopback are skipped, as are IPv6 addresses. The
     * same block reached through several interfaces or addresses appears once, and the
     * blocks are returned in the order the interfaces and their addresses are reported.
     *
     * @return the IPv4 CIDR blocks of the machine, <code>null</code> if no up, non-loopback
     * interface has an IPv4 address bound to it
     * @throws IOException if the interfaces or their state can't be read
     * @see #toV4CIDRBlocks(NetworkInterface)
     * @see SharedNetUtil#toCIDRBlock(byte[], short)
     */
    public static String[] allV4CIDRBlocks() throws IOException {
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        Set<String> cidrBlocks = new LinkedHashSet<>();
        while (enumeration.hasMoreElements()) {
            NetworkInterface ni = enumeration.nextElement();
            cidrBlocks = _toV4CIDRBlocks(ni, cidrBlocks);
        }

        return cidrBlocks.isEmpty() ? null : cidrBlocks.toArray(new String[0]);
    }

    private static Set<String> _toV4CIDRBlocks(NetworkInterface nif, Set<String> ret)
            throws IOException {
        if (ret == null)
            ret = new LinkedHashSet<>();

        if (!nif.isUp() || nif.isLoopback())
            return ret;

        for (InterfaceAddress a : nif.getInterfaceAddresses()) {
            if (!(a.getAddress() instanceof Inet4Address)) continue;

            ret.add(SharedNetUtil.toCIDRBlock(a.getAddress().getAddress(), a.getNetworkPrefixLength()));
        }

        return ret;
    }


    /**
     * Converts an IPv4 prefix length to its netmask address (e.g. 24 to 255.255.255.0).
     *
     * @param netPrefix the prefix length (0-32)
     * @return the netmask as an address
     * @throws IOException              if the mask bytes can't be converted to an address
     * @throws IllegalArgumentException if netPrefix is negative or greater than 32
     * @see SharedNetUtil#toNetmaskIPV4(short)
     */
    public static InetAddress toNetmaskIPV4(short netPrefix) throws IOException {
        return InetAddress.getByAddress(SharedNetUtil.toNetmaskIPV4(netPrefix));
    }

    /**
     * Returns the IPv4 network address of an interface address by clearing the host bits
     * according to its prefix length (e.g. 192.168.1.100/24 to 192.168.1.0).
     * <p>
     * Only IPv4 interface addresses are supported, the result for an IPv6 interface address
     * is undefined.
     *
     * @param ia the interface address
     * @return the network address
     * @throws IOException          if the masked bytes can't be converted to an address
     * @throws NullPointerException if ia is null
     */
    public static InetAddress getNetworkIPV4(InterfaceAddress ia) throws IOException {
        InetAddress address = ia.getAddress();
        byte[] addressBytes = address.getAddress();
        short mask = ia.getNetworkPrefixLength();

        long maskLong = (addressBytes.length == 4) ? 0xffffffffL : 0xffffffffffffL;

        maskLong = maskLong << ((addressBytes.length == 4) ? 32 - mask : 48 - mask);

        for (int i = 0; i < addressBytes.length; i++) {
            addressBytes[addressBytes.length - (1 + i)] = (byte) (addressBytes[addressBytes.length - (1 + i)] & maskLong);

            maskLong = maskLong >> 8;
        }


        return InetAddress.getByAddress(addressBytes);
    }


    /**
     * Returns the network address of an IPv4 address under a netmask, both given as text.
     * <p>
     * A null mask returns the address unchanged.
     *
     * @param address the IPv4 address (e.g. "192.168.1.100")
     * @param mask    the netmask (e.g. "255.255.255.0"), null to return the address as is
     * @return the network address as text (e.g. "192.168.1.0")
     * @throws IOException if the address or the mask can't be resolved
     * @see #getNetworkIPV4(InetAddress, InetAddress)
     */
    public static String getNetworkIPV4(String address, String mask)
            throws IOException {

        InetAddress ret = null;
        if (mask != null) {
            ret = getNetworkIPV4(InetAddress.getByName(address), InetAddress.getByName(mask));
        } else {
            return address;
        }
        return (ret != null) ? ret.getHostAddress() : null;
    }

    /**
     * Returns the network address of an IPv4 address under a netmask.
     *
     * @param address the IPv4 address
     * @param mask    the netmask
     * @return the network address
     * @throws IOException          if the masked bytes can't be converted to an address
     * @throws NullPointerException if address or mask is null
     * @see SharedNetUtil#getNetwork(byte[], byte[])
     */
    public static InetAddress getNetworkIPV4(InetAddress address, InetAddress mask)
            throws IOException {
        return InetAddress.getByAddress(SharedNetUtil.getNetwork(address.getAddress(), mask.getAddress()));
    }


    /**
     * Ping a host using default settings.
     *
     * @param host the hostname or IP address to ping
     * @return true if the host is reachable
     * @throws IOException              if a network error occurs
     * @throws IllegalArgumentException if host is null or empty
     */
    public static boolean ping(String host) throws IOException {
        return ping(host, null);
    }

    /**
     * Ping a host through a specific network interface.
     *
     * @param host   the hostname or IP address to ping
     * @param niName the network interface name (can be null for default)
     * @return true if the host is reachable
     * @throws IOException              if a network error occurs
     * @throws IllegalArgumentException if host is null or empty
     */
    public static boolean ping(String host, String niName) throws IOException {
        SUS.checkIfNulls("Host cannot be null", host);
        return ping(InetAddress.getByName(host), (niName != null) ? NetworkInterface.getByName(niName) : null);
    }

    /**
     * Ping an address with default TTL (255) and timeout (5000ms).
     *
     * @param addr the InetAddress to ping
     * @param ni   the network interface (can be null for default)
     * @return true if the address is reachable
     * @throws IOException              if a network error occurs
     * @throws IllegalArgumentException if addr is null
     */
    public static boolean ping(InetAddress addr, NetworkInterface ni) throws IOException {
        return ping(addr, ni, 255, 5000);
    }

    /**
     * Ping an address with statistics enabled.
     *
     * @param addr    the InetAddress to ping
     * @param ni      the network interface (can be null for default)
     * @param ttl     the time-to-live (max hops)
     * @param timeout the timeout in milliseconds
     * @return true if the address is reachable
     * @throws IOException              if a network error occurs
     * @throws IllegalArgumentException if addr is null
     */
    public static boolean ping(InetAddress addr, NetworkInterface ni, int ttl, int timeout) throws IOException {
        return ping(addr, ni, ttl, timeout, true);
    }

    /**
     * Ping an address with full control over parameters.
     *
     * @param addr    the InetAddress to ping
     * @param ni      the network interface (can be null for default)
     * @param ttl     the time-to-live (max hops)
     * @param timeout the timeout in milliseconds
     * @param logToOS if true, print timing statistics to stdout
     * @return true if the address is reachable
     * @throws IOException              if a network error occurs
     * @throws IllegalArgumentException if addr is null
     */
    public static boolean ping(InetAddress addr, NetworkInterface ni, int ttl, int timeout, boolean logToOS) throws IOException {
        SUS.checkIfNulls("Address cannot be null", addr);
        long delta = 0;
        if (logToOS) {
            delta = System.currentTimeMillis();
        }
        boolean ret = addr.isReachable(ni, ttl, timeout);

        if (logToOS) {
            delta = System.currentTimeMillis() - delta;
            if (log.isEnabled()) {
                if (ret)
                    log.getLogger().info("it took " + Const.TimeInMillis.toString(delta) + " ping: " + addr.getHostName());
                else
                    log.getLogger().info("ping timed out after " + timeout + " millis to ping: " + addr.getHostName());
            }
        }

        return ret;
    }


    /**
     * Check if the input is a private IP address and return as inet address, null otherwise.
     * Private IP ranges: 10.x.x.x, 192.168.x.x, 172.16-31.x.x
     *
     * @param input the string to check
     * @return InetAddress if it's a private IP, null otherwise
     */
    public static InetAddress toPrivateIP(String input) {

        try {
            // Check for private IP patterns
            if (SharedNetUtil.isPrivateIP(input)) {
                return InetAddress.getByName(input);
            }
        } catch (UnknownHostException e) {
            // Not a valid IP format
        }
        return null;
    }


    /**
     * Generates a Linux shell script that installs a multipath default route over the WAN
     * interfaces.
     * <p>
     * The script deletes the default route and adds one <code>nexthop</code> per interface of
     * type {@link NIType#WAN} that is up and has status {@link NIStatus#OK}, weighted by the
     * interface bandwidth capacity, then flushes the route cache. The gateway of each
     * interface is read from the Linux interface configuration through {@link IPInfo}.
     *
     * @param nis the connection properties of the interfaces to consider
     * @return the script text
     * @throws IOException if an interface state or configuration can't be read
     * @see #generateBondingScript(ConnectionPropDAO[])
     */
    public static String generateRoutingScript(ConnectionPropDAO[] nis) throws IOException {
        StringBuilder sb = new StringBuilder("#!/bin/sh\n");
        sb.append("# Automatically generated bonding script " + new Date() + "\n");
        sb.append("# The bonding script will apply to the following network interfaces:\n");

        for (ConnectionPropDAO ni : nis) {
            if (ni.getType() == NIType.WAN && ni.currentStatus() == NIStatus.OK)
                sb.append("# * " + ni + "\n");
        }
        sb.append("# delete the default route\n");
        sb.append("route del default\n\n");

        Formatter frm = new Formatter(sb);
        frm.format("ip route add default scope global");
        for (ConnectionPropDAO ni : nis) {
            if (ni.getType() == NIType.WAN && isActive(ni.getName()) && ni.currentStatus() == NIStatus.OK) {
                IPInfo ii = IPInfo.getV4IPInfo(ni.getNIName());
                frm.format(" nexthop via %s dev %s weight %s", ii.getGateway().getHostAddress(), ni.getNIName(), "" + ni.getBandwidthCapacity());
            }
        }
        sb.append("\n");
        sb.append("ip route flush cache\n");
        frm.close();
        return sb.toString();


    }


    /**
     * Returns the boot protocol of a Linux network interface, static or DHCP, read from its
     * <code>ifcfg</code> file.
     *
     * @param niName the network interface name (e.g. "eth0")
     * @return {@link InetProto#STATIC} or {@link InetProto#DHCP}
     * @throws IOException              if the configuration file can't be read or the protocol is
     *                                  neither static nor dhcp
     * @throws NullPointerException     if niName is null or the file has no BOOTPROTO entry
     * @throws IllegalArgumentException if the BOOTPROTO value is not a known protocol
     * @see IPInfo#LINUX_NI_CFG_PREFIX
     */
    public static InetProto getProtoType(String niName) throws IOException {
        SUS.checkIfNull("Network interface name can't be null", niName);
        Properties ifcfg = new Properties();
        FileReader fr = null;
        try {
            fr = new FileReader(IPInfo.LINUX_NI_CFG_PREFIX + niName);
            ifcfg.load(fr);
        } finally {
            SharedIOUtil.close(fr);
        }

        String proto = ifcfg.getProperty(IPInfo.LINUX_BOOTPROTO);
        InetProto pt = InetProto.valueOf(proto.toUpperCase());
        switch (pt) {
            case STATIC:
            case DHCP:
                return pt;

            default:
                throw new IOException("Network " + niName + " has unsupported BOOTPROTO" + proto);
        }


    }


    /**
     * Looks up a network interface by name.
     *
     * @param niName the network interface name (e.g. "eth0")
     * @return the network interface, null if no interface has that name
     * @throws IOException          if the interfaces can't be read
     * @throws NullPointerException if niName is null
     */
    public static NetworkInterface toNI(String niName) throws IOException {
        return NetworkInterface.getByName(niName);
    }

    /**
     * Tells whether a network interface looked up by name is up.
     *
     * @param niName the network interface name, can be null
     * @return true if the interface exists and is up, false if niName is null, the interface
     * does not exist or it is down
     * @throws IOException if the interface state can't be read
     */
    public static boolean isActive(String niName) throws IOException {
        if (niName == null) {
            return false;
        }
        NetworkInterface ni = toNI(niName);
        if (ni != null) {
            return ni.isUp();
        }

        return false;
    }


    /**
     * Converts an IP address text to an {@link InetAddressInfo}, detecting its IP version.
     * <p>
     * The text is stored as given, it is resolved only to tell IPv4 from IPv6.
     *
     * @param str the IP address or host name
     * @return the address info with its IP version set
     * @throws IOException          if the address can't be resolved
     * @throws NullPointerException if str is null
     */
    public static InetAddressInfo toInetAddressInfo(String str) throws IOException {
        SUS.checkIfNulls("Null address", str);
        InetAddressInfo ret = new InetAddressInfo();
        ret.setInetAddress(str);
        ret.setIPVersion(InetAddress.getByName(str) instanceof Inet4Address ? IPVersion.V4 : IPVersion.V6);

        return ret;
    }

    /**
     * Converts an address to an {@link InetAddressInfo}, with its textual form and IP version.
     *
     * @param addr the address
     * @return the address info with its IP version set
     * @throws IOException          declared for symmetry with {@link #toInetAddressInfo(String)}, not thrown
     * @throws NullPointerException if addr is null
     */
    public static InetAddressInfo toInetAddressInfo(InetAddress addr) throws IOException {
        SUS.checkIfNulls("Null address", addr);
        InetAddressInfo ret = new InetAddressInfo();
        ret.setInetAddress(addr.getHostAddress());
        ret.setIPVersion(addr instanceof Inet4Address ? IPVersion.V4 : IPVersion.V6);

        return ret;
    }

    /**
     * Tells whether a network interface is up.
     *
     * @param ni the network interface, can be null
     * @return true if the interface is up, false if it is null or down
     * @throws IOException if the interface state can't be read
     */
    public static boolean isActive(NetworkInterface ni) throws IOException {

        if (ni != null) {
            return ni.isUp();
        }

        return false;
    }

    /**
     * Generates a Linux shell script that sets up source based policy routing over the WAN
     * interfaces.
     * <p>
     * For every interface of type {@link NIType#WAN} that is up, the script rebuilds the
     * interface routing table (identified by the connection route id): the interface network
     * route, a default route via the interface gateway, and a rule sending traffic sourced
     * from the interface address to that table. The {@link NIType#LAN} interface of category
     * {@link NICategory#MAIN} gets its network route added to the main table. Network,
     * address and gateway are read from the Linux interface configuration through {@link IPInfo}.
     *
     * @param nis the connection properties of the interfaces to consider
     * @return the script text
     * @throws IOException if an interface state or configuration can't be read
     * @see #generateRoutingScript(ConnectionPropDAO[])
     */
    public static String generateBondingScript(ConnectionPropDAO[] nis) throws IOException {
        StringBuilder sb = new StringBuilder("#!/bin/sh\n");
        sb.append("# Automatically generated bonding script " + new Date() + "\n");
        sb.append("# The bonding script will apply to the following network interfaces:\n");

        for (ConnectionPropDAO ni : nis)
            sb.append("# * " + ni + "\n");
        sb.append("# delete the default route\n");
        sb.append("route del default\n\n");
        Formatter frm = new Formatter(sb);
        for (ConnectionPropDAO ni : nis) {
            //dbg("Generating for " + ni);
            if (ni.getType() == NIType.WAN && isActive(ni.getName())) {
                frm.format("# Generating routing tables info for %s\n", ni);
                frm.format("ip route del table %d\n", ni.getRouteID());
                IPInfo ii = IPInfo.getV4IPInfo(ni.getNIName());
                //dbg("IPInfo " + ii);
                // ip route add $P1_NET dev $IF1 src $IP1 table $T1
                frm.format("ip route add %s dev %s src %s table %d\n",
                        ii.getNetwork().getHostAddress(),
                        ii.getNetworkInterface().getName(),
                        ii.getIPAddress().getHostAddress(),
                        ni.getRouteID());
                //ip route add default via $P1 table $T1
                frm.format("ip route add default via %s table %s\n",
                        ii.getGateway().getHostAddress(),
                        ni.getRouteID());
                // ip rule add from $IP1 table $T1
                frm.format("ip rule add from %s table %d\n\n",
                        ii.getIPAddress().getHostAddress(),
                        ni.getRouteID());
            }
        }


//		// to be removed just for testing now
//		//ip route add default scope global nexthop via $P1 dev $IF1 weight 1 nexthop via $P2 dev $IF2 weight 1
//		frm.format("ip route add default scope global");
//		for ( NIProp ni : nis)
//		{
//			if ( ni.getType() == NIType.WAN && ni.isActive())
//			{
//				IPInfo ii = IPInfo.getV4IPInfo(ni.getNIName());
//				frm.format(" nexthop via %s dev %s weight 1", ii.getGateway().getHostAddress(), ni.getNIName());
//			}
//		}
//		sb.append("\n");

        //ip route add $LAN_NET dev $LAN_IF src 0.0.0.0
        for (ConnectionPropDAO ni : nis) {
            if (ni.getType() == NIType.LAN && ni.getCategory() == NICategory.MAIN) {
                IPInfo ii = IPInfo.getV4IPInfo(ni.getNIName());
                frm.format("ip route add %s dev %s src 0.0.0.0\n",
                        ii.getNetwork().getHostAddress(),
                        ni.getNIName());
            }
        }
        sb.append("\n");
        frm.close();
        return sb.toString();
    }


    /**
     * Compares two IP addresses by resolved address and port.
     * <p>
     * The host names are resolved, so "localhost" and "127.0.0.1" on the same port are equal.
     *
     * @param isad1 the first address, can be null
     * @param isad2 the second address, can be null
     * @return true if both are not null, both carry an address, the addresses resolve to the same
     * host and the ports are equal
     * @throws UnknownHostException if one of the host names can't be resolved
     */
    public static boolean areIPAddressEquals(IPAddress isad1, IPAddress isad2) throws UnknownHostException {
        if (isad1 != null && isad2 != null) {
            if (isad1.getInetAddress() != null && isad2.getInetAddress() != null) {
                boolean ret = InetAddress.getByName(isad1.getInetAddress()).equals(InetAddress.getByName(isad2.getInetAddress()));

                return (ret && (isad1.getPort() == isad2.getPort()));

            }
        }

        return false;
    }

    /**
     * Cancels a selection key and optionally closes its channel.
     * <p>
     * The cancel and the close run while holding the key's monitor. A null key is ignored and
     * closing never throws.
     *
     * @param sk             the selection key, can be null
     * @param includeChannel true to also close the key's channel
     */
    public static void cancelSelectionKey(SelectionKey sk, boolean includeChannel) {
        if (sk != null) {
            synchronized (sk) {
                Channel channel = sk.channel();
                sk.cancel();
                if (includeChannel)
                    SharedIOUtil.close(channel);
            }
        }
    }

    /**
     * Cancels every selection key of a set and optionally closes their channels.
     * <p>
     * The set is copied first, so a selector's live key set can be passed.
     *
     * @param skSet          the selection keys, can be null
     * @param includeChannel true to also close the channels
     * @see #cancelSelectionKey(SelectionKey, boolean)
     */
    public static void cancelSelectionKeySet(Set<SelectionKey> skSet, boolean includeChannel) {
        if (skSet != null) {
            SelectionKey[] all = skSet.toArray(new SelectionKey[0]);
            for (SelectionKey sk : all) {
                cancelSelectionKey(sk, includeChannel);
            }
        }
    }

}
