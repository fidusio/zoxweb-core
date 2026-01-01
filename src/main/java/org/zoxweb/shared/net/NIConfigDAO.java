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

import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.net.InetProp.InetProto;
import org.zoxweb.shared.util.*;

import java.io.IOException;

;

/**
 * Data access object for network interface configuration.
 * Stores network interface settings including address, netmask, gateway,
 * DNS servers, and protocol-specific configurations like PPPoE and WPA.
 *
 * @author mnael
 * @see InetProp
 * @see SharedNetUtil
 */
@SuppressWarnings("serial")
public class NIConfigDAO
        extends PropertyDAO {
    /**
     * Configuration parameters for NIConfigDAO.
     */
    public enum Param
            implements GetNVConfig, GetName {
        /** The physical network interface name (e.g., eth0, wlan0) */
        NI_NAME(NVConfigManager.createNVConfig("ni_name", "Network interface physical name", "NIName", false, true, String.class)),
        /** The internet protocol type (DHCP, Static, PPPoE, etc.) */
        INET_PROTO(NVConfigManager.createNVConfig("inet_proto", "Inet protocol", "Proto", false, true, InetProto.class)),
        /** The IP address assigned to the interface */
        ADDRESS(NVConfigManager.createNVConfig("address", "Address", "Address", false, true, String.class)),
        /** The network mask */
        NETMASK(NVConfigManager.createNVConfig("netmask", "Network Mask", "NetMask", false, true, String.class)),
        /** The default gateway address */
        GATEWAY(NVConfigManager.createNVConfig("gateway", "Gateway", "Gateway", false, true, String.class)),
        /** The network address */
        NETWORK(NVConfigManager.createNVConfig("network", "Network", "Network", false, true, String.class)),
        /** Username for PPPoE authentication */
        USERNAME(NVConfigManager.createNVConfig("username", "PPPoE user name", "UserName", false, true, String.class)),
        /** Password for PPPoE authentication */
        PASSWORD(NVConfigManager.createNVConfig("password", "PPPoE password", "Password", false, true, String.class)),
        /** DNS name servers */
        DNS_SERVERS(NVConfigManager.createNVConfig("dns-nameservers", "DNS Name Servers", "DNSNameServers", false, true, String.class)),
        /** WPA configuration file path */
        WPA_CONF(NVConfigManager.createNVConfig("wpa-conf", "WAP configuration file", "WPA-CONF", false, true, String.class)),

        ;

        private final NVConfig cType;

        Param(NVConfig c) {
            cType = c;
        }

        public NVConfig getNVConfig() {
            return cType;
        }

        @Override
        public String getName() {
            return cType.getName();
        }

    }

    /** NVConfigEntity definition for NIConfigDAO */
    public static final NVConfigEntity NVC_NI_CONFIG_DAO = new NVConfigEntityPortable("ni_config_dao", null, "NIConfigDAO", true, false, false, false, NIConfigDAO.class, SharedUtil.extractNVConfigs(Param.NI_NAME, Param.INET_PROTO), null, false, PropertyDAO.NVC_PROPERTY_DAO);

    /**
     * Default constructor.
     */
    public NIConfigDAO() {
        super(NVC_NI_CONFIG_DAO);
    }

    /**
     * Returns the network interface name.
     *
     * @return the physical network interface name
     */
    public String getNIName() {
        return lookupValue(Param.NI_NAME.getName());
    }

    /**
     * Sets the network interface name.
     *
     * @param niName the network interface name to set
     */
    public void setNIName(String niName) {
        setValue(Param.NI_NAME, niName);
    }

    /**
     * Returns the internet protocol type.
     *
     * @return the inet protocol (DHCP, Static, PPPoE, etc.)
     */
    public InetProto getInetProtocol() {
        return lookupValue(Param.INET_PROTO.getName());
    }

    /**
     * Sets the internet protocol type.
     *
     * @param proto the inet protocol to set
     */
    public void setInteProtocol(InetProto proto) {
        setValue(Param.INET_PROTO, proto);
    }

    /**
     * Returns the IP address.
     *
     * @return the IP address string
     */
    public String getAddress() {
        return getProperties().getValue(Param.ADDRESS.getName());
    }

    /**
     * Sets the IP address.
     *
     * @param address the IP address to set
     */
    public void setAddress(String address) {
        getProperties().add(Param.ADDRESS, address);
    }

    /**
     * Returns the network mask.
     *
     * @return the network mask string
     */
    public String getNetmask() {
        return getProperties().getValue((GetName) Param.NETMASK);
    }

    /**
     * Sets the network mask.
     *
     * @param netmask the network mask to set
     */
    public void setNetmask(String netmask) {
        getProperties().add(Param.NETMASK, netmask);
    }

    /**
     * Returns the default gateway address.
     *
     * @return the gateway address string
     */
    public String getGateway() {
        return getProperties().getValue((GetName) Param.GATEWAY);
    }

    /**
     * Sets the default gateway address.
     *
     * @param gateway the gateway address to set
     */
    public void setGateway(String gateway) {
        getProperties().add(Param.GATEWAY, gateway);
    }

    /**
     * Returns the DNS name servers.
     *
     * @return the DNS servers string
     */
    public String getDNSServers() {
        return getProperties().getValue((GetName) Param.DNS_SERVERS);
    }

    /**
     * Sets the DNS name servers.
     *
     * @param dnsServers the DNS servers to set
     */
    public void setDNSServers(String dnsServers) {
        getProperties().add(Param.DNS_SERVERS, dnsServers);
    }

    /**
     * Returns the network address.
     *
     * @return the network address string
     */
    public String getNetwork() {
        return getProperties().getValue((GetName) Param.NETWORK);
    }

    /**
     * Sets the network address.
     *
     * @param network the network address to set
     */
    public void setNetwork(String network) {
        getProperties().add(Param.NETWORK, network);
    }

    /**
     * Returns the PPPoE username.
     *
     * @return the username string
     */
    public String getUsername() {
        return getProperties().getValue((GetName) Param.USERNAME);
    }

    /**
     * Sets the PPPoE username.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
        getProperties().add(Param.USERNAME, username);
    }

    /**
     * Returns the PPPoE password.
     *
     * @return the password string
     */
    public String getPassword() {
        return getProperties().getValue((GetName) Param.PASSWORD);
    }

    /**
     * Sets the PPPoE password.
     *
     * @param password the password to set
     */
    public void setPassword(String password) {
        getProperties().add(Param.PASSWORD, password);
    }

    /**
     * Returns the WPA configuration file path.
     *
     * @return the WPA config file path
     */
    public String getWPAConfig() {
        return getProperties().getValue((GetName) Param.WPA_CONF);
    }

    /**
     * Sets the WPA configuration file path.
     *
     * @param wpaConfig the WPA config file path to set
     */
    public void setWPACondfig(String wpaConfig) {
        getProperties().add(Param.WPA_CONF, wpaConfig);
    }

    /**
     * Returns the address with netmask in CIDR notation.
     *
     * @return the address/netmask in CIDR format (e.g., 192.168.1.1/24)
     * @throws IOException if the netmask is invalid
     */
    public String getAddressNetmask() throws IOException {
        return getAddress() + "/" + SharedNetUtil.toNetmaskIPV4(SharedNetUtil.getV4Address(getNetmask()));
    }

}
