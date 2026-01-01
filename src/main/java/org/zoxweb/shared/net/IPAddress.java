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

import org.zoxweb.shared.data.SetNameDAO;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

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

                if (getPort() == ((IPAddress) o).getPort()) {
                    return true;
                }
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
     * Parses an address string.
     *
     * @param addressPort the address string to parse
     * @return the parsed IPAddress
     */
    public static IPAddress parse(String addressPort) {
        return new IPAddress(addressPort);
    }

}