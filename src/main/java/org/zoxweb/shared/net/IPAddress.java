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
package org.zoxweb.shared.net;

import org.zoxweb.shared.data.Range;
import org.zoxweb.shared.data.SetNameDAO;
import org.zoxweb.shared.http.URLInfo;
import org.zoxweb.shared.util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data access object representing an IP address with port and connection settings.
 * Stores socket address information including IP address, port number,
 * connection backlog, and optional proxy type.
 *
 * @author mnael
 * @see ProxyType
 */
@SuppressWarnings("serial")
public class IPAddress
        extends SetNameDAO {

    private static final NVConfig INET_ADDRESS = NVConfigManager.createNVConfig("inet_address", "The ip address", "InetAddress", true, false, String.class);
    private static final NVConfig PORT = NVConfigManager.createNVConfig("port", "The port number", "Port", true, false, int.class);
    private static final NVConfig BACKLOG = NVConfigManager.createNVConfig("backlog", "How many pending connections", "BackLog", true, false, int.class);
    private static final NVConfig PROXY_TYPE = NVConfigManager.createNVConfig("proxy_type", "proxy type", "ProxyType", false, false, ProxyType.class);

    /** NVConfigEntity definition for IPAddress */
    public static final NVConfigEntity NVC_IP_ADDRESS = new NVConfigEntityPortable("ip_address", null, "IPAddress", true, false, false, false, IPAddress.class, SharedUtil.toNVConfigList(INET_ADDRESS, PORT, BACKLOG, PROXY_TYPE), null, false, SetNameDAO.NVC_NAME_DAO);


    /**
     * Default constructor.
     */
    public IPAddress() {
        super(NVC_IP_ADDRESS);
    }

    /**
     * Constructor that parses an address string.
     * Supports formats: "address", "port", "address:port", "address:port:proxyType"
     *
     * @param addressPort the address string to parse
     * @throws IllegalArgumentException if the format is invalid
     */
    public IPAddress(String addressPort) {
        this();
        String[] params = addressPort.split(":");
        switch (params.length) {
            case 1:
                // 2 possibilities: just port or address
                try {
                    setPort(Integer.parseInt(params[0]));
                } catch (Exception e) {
                    setInetAddress(params[0]);
                    setPort(-1);
                }
                break;
            case 3:
                setProxyType(params[2]);
            case 2:
                setInetAddress(params[0]);
                setPort(Integer.parseInt(params[1]));
                break;
            default:
                throw new IllegalArgumentException("Invalid address " + addressPort);
        }
    }

    /**
     * Constructor with address and port.
     *
     * @param address the IP address
     * @param port the port number
     */
    public IPAddress(String address, int port) {
        this(address, port, 128, null);
    }

    /**
     * Constructor with address, port, and proxy type.
     *
     * @param address the IP address
     * @param port the port number
     * @param pt the proxy type
     */
    public IPAddress(String address, int port, ProxyType pt) {
        this(address, port, 128, pt);
    }

    /**
     * Constructor with all parameters.
     *
     * @param address the IP address
     * @param port the port number
     * @param backlog the connection backlog
     * @param pt the proxy type
     */
    public IPAddress(String address, int port, int backlog, ProxyType pt) {
        this();
        setInetAddress(address);
        setPort(port);
        setProxyType(pt);
        setBacklog(backlog);
    }

    /**
     * Returns the IP address.
     *
     * @return the IP address string
     */
    public String getInetAddress() {
        return lookupValue(INET_ADDRESS);
    }

    /**
     * Sets the IP address.
     *
     * @param address the IP address to set
     */
    public void setInetAddress(String address) {
        setValue(INET_ADDRESS, address);
    }

    /**
     * Returns the port number.
     *
     * @return the port number
     */
    public int getPort() {
        return lookupValue(PORT);
    }

    /**
     * Sets the proxy type.
     *
     * @param pt the proxy type to set
     */
    public void setProxyType(ProxyType pt) {
        setValue(PROXY_TYPE, pt);
    }

    /**
     * Returns the proxy type.
     *
     * @return the proxy type
     */
    public ProxyType getProxyType() {
        return lookupValue(PROXY_TYPE);
    }

    /**
     * Sets the port number.
     *
     * @param port the port number to set (must be >= -1)
     * @throws IllegalArgumentException if port is less than -1
     */
    public void setPort(int port) {
        if (port < -1) {
            throw new IllegalArgumentException("Invalid port:" + port + " < 0 ");
        }

        setValue(PORT, port);
    }


    public boolean isPrivateIP() {
        return SharedNetUtil.isPrivateIP(getInetAddress());
    }

    /**
     * Sets the connection backlog.
     *
     * @param blog the backlog value (must be >= -1)
     * @throws IllegalArgumentException if backlog is less than -1
     */
    public void setBacklog(int blog) {
        if (blog < -1) {
            throw new IllegalArgumentException("Invalid backlog:" + blog + " < 0 ");
        }

        setValue(BACKLOG, blog);
    }

    /**
     * Returns the connection backlog.
     *
     * @return the backlog value
     */
    public int getBacklog() {
        return lookupValue(BACKLOG);
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o instanceof IPAddress) {
            if (getInetAddress() != null && getInetAddress().equalsIgnoreCase(((IPAddress) o).getInetAddress())) {
                return (getPort() == ((IPAddress) o).getPort());
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        String val = "InetSocketAddressDAO:" + getInetAddress() + getPort();
        return val.hashCode();
    }

    /**
     * Sets the proxy type from a string value.
     *
     * @param pType the proxy type string
     */
    public void setProxyType(String pType) {
        ProxyType pt = SharedUtil.lookupEnum(pType, ProxyType.values());
        setProxyType(pt);
    }

    /**
     * Returns a string representation in format "address:port".
     *
     * @return the canonical string representation
     */
    @Override
    public String toString() {
        return SharedUtil.toCanonicalID(':', getInetAddress(), getPort());
    }

    /**
     * Parses an address string with a specified proxy type.
     *
     * @param addressPortProxyType the address string
     * @param pt the proxy type to use if not specified in the string
     * @return the parsed IPAddress
     */
    public static IPAddress parse(String addressPortProxyType, ProxyType pt) {
        if (addressPortProxyType.toUpperCase().indexOf(pt.name()) == -1) {
            addressPortProxyType = addressPortProxyType + ":" + pt;
        }
        return new IPAddress(addressPortProxyType);
    }


    /**
     * Parse ipaddress of format ipaddress:[0,1025]
     * @param ips to be parsed
     * @return IPAddress[] of the specified range
     */
    public static IPAddress[] parseList(String... ips) {

        List<IPAddress> ipAddresses = new ArrayList<IPAddress>();
        for (String ip : ips) {
            try {
                ipAddresses.add(URLDecoder.decode(ip));
            } catch (Exception e) {
                try {
                    IPAddress[] rangeIPs = RangeDecoder.decode(ip);

                    if (rangeIPs != null && rangeIPs.length > 0) {
                        Collections.addAll(ipAddresses, rangeIPs);
                    }
                } catch (Exception ex) {
                }
            }

//            String[] parsed = ip.split(":");
//            try {
//                int singlePort = Integer.parseInt(parsed[1]);
//                ipAddresses.add(new IPAddress(parsed[0], singlePort));
//
//            } catch (Exception e) {
//            }
        }


        return ipAddresses.toArray(new IPAddress[0]);

    }

    /**
     * Parses an address string.
     *
     * @param addressPort the address string to parse
     * @return the parsed IPAddress
     */
    public static IPAddress parse(String addressPort) {
        try {
            return URLDecoder.decode(addressPort);
        } catch (Exception e) {
        }
        return new IPAddress(addressPort);
    }


    /**
     * Parse ipaddress of format ipaddress:[0,1025]
     * input ipAddress to be parsed
     * return IPAddress[] of the specified range
     */
    public static final DataDecoder<String, IPAddress[]> RangeDecoder = (ipAddress) -> {
        String[] parsed = ipAddress.split(":");
        String address = parsed[0];

        List<IPAddress> ipAddresses = new ArrayList<IPAddress>();
        try {
            int singlePort = Integer.parseInt(parsed[1]);
            ipAddresses.add(new IPAddress(address, singlePort));

        } catch (Exception e) {

            Range<Integer> ports = Range.toRange(parsed[1]);
            for (int i = ports.getStart(); i <= ports.getEnd(); i++) {

                if (ports.within(i))
                    ipAddresses.add(new IPAddress(address, i));
            }
        }


        return ipAddresses.toArray(new IPAddress[0]);
    };


    /**
     * Decodes a URL string into an IPAddress.
     * Supports formats:
     * <ul>
     *   <li>Full URL: "http://host:port/path" or "https://host:port"</li>
     *   <li>URL without port: "http://host" (uses default port 80 for http, 443 for https)</li>
     *   <li>Simple format: "host:port"</li>
     * </ul>
     */
    public static final DataDecoder<String, IPAddress> URLDecoder = (urlString) -> {
        return URLInfo.parse(urlString).ipAddress;

//        if (SUS.isEmpty(urlString)) {
//            throw new IllegalArgumentException("URL string cannot be null or empty");
//        }
//
//        String input = urlString.trim();
//        String host;
//        int port = -1;
//
//        // Check for scheme and get default port
//        URIScheme uriScheme = URIScheme.match(input);
//        if (uriScheme == null) {
//            throw new IllegalArgumentException("Invalid URL scheme: " + input);
//        }
//
//        // Strip scheme (e.g., "http://")
//        input = input.substring(uriScheme.getName().length() + 3); // +3 for "://"
//        port = uriScheme.getValue(); // default port
//
//
//        // Remove path/query (everything after first /)
//        int pathIndex = input.indexOf('/');
//        if (pathIndex != -1) {
//            input = input.substring(0, pathIndex);
//        }
//
//        // Remove query string if no path but has query (e.g., host:port?query)
//        int queryIndex = input.indexOf('?');
//        if (queryIndex != -1) {
//            input = input.substring(0, queryIndex);
//        }
//
//        // Parse host:port - use lastIndexOf to handle IPv6 addresses
//        int colonIndex = input.lastIndexOf(':');
//        if (colonIndex != -1) {
//            // Check if this is a port or part of IPv6 address
//            String potentialPort = input.substring(colonIndex + 1);
//            try {
//                int parsedPort = Integer.parseInt(potentialPort);
//                host = input.substring(0, colonIndex);
//                port = parsedPort;
//            } catch (NumberFormatException e) {
//                // Not a port number, treat entire string as host
//                host = input;
//            }
//        } else {
//            host = input;
//        }
//
//        if (port == -1) {
//            port = uriScheme.getValue();
//        }
//
//        if (SUS.isEmpty(host)) {
//            throw new IllegalArgumentException("Invalid URL: no host found in " + urlString);
//        }
//
//        return new IPAddress(host, port);
    };

}