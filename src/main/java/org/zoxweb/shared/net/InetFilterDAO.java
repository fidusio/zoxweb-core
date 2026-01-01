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

import org.zoxweb.shared.data.SetNameDescriptionDAO;

import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

/**
 * Data access object for IP address filtering.
 * Used to mask and filter IP address sets based on
 * IP address, network mask, and network address.
 *
 * @author mnael
 */
@SuppressWarnings("serial")
public class InetFilterDAO
        extends SetNameDescriptionDAO {

    /**
     * Configuration parameters for InetFilterDAO.
     */
    public enum Params
            implements GetNVConfig {
        /** The IP address to filter */
        IP(NVConfigManager.createNVConfig("ip_address", "IP Address", "IP", false, true, String.class)),
        /** The network mask for filtering */
        NET_MASK(NVConfigManager.createNVConfig("network_mask", "Network Mask", "NetMask", false, true, String.class)),
        /** The network address */
        NETWORK(NVConfigManager.createNVConfig("network_address", "Network", "Network", false, true, String.class)),
        ;

        private final NVConfig cType;

        Params(NVConfig c) {
            cType = c;
        }

        public NVConfig getNVConfig() {
            return cType;
        }

    }

    /** NVConfigEntity definition for InetFilterDAO */
    public static final NVConfigEntity NVC_INET_FILTER_DAO = new NVConfigEntityPortable("inet_filter_dao", null, "InetFilterDAO", true, false, false, false, InetFilterDAO.class, SharedUtil.extractNVConfigs(Params.values()), null, false, SetNameDescriptionDAO.NVC_NAME_DAO);

    /**
     * Default constructor.
     */
    public InetFilterDAO() {
        super(NVC_INET_FILTER_DAO);
    }

    /**
     * Constructor with IP address and network mask.
     *
     * @param ip the IP address
     * @param mask the network mask
     */
    public InetFilterDAO(String ip, String mask) {
        this();
        setIP(ip);
        setNetworkMask(mask);
    }

    /**
     * Returns the IP address.
     *
     * @return the IP address, or null if not set
     */
    public String getIP() {
        return lookupValue(Params.IP);
    }

    /**
     * Sets the IP address.
     *
     * @param ip the IP address to set
     */
    public void setIP(String ip) {
        setValue(Params.IP, ip);
    }

    /**
     * Returns the network mask.
     *
     * @return the network mask, or null if not set
     */
    public String getNetworkMask() {
        return lookupValue(Params.NET_MASK);
    }

    /**
     * Sets the network mask.
     *
     * @param mask the network mask to set
     */
    public void setNetworkMask(String mask) {
        setValue(Params.NET_MASK, mask);
    }

    /**
     * Returns the network address.
     *
     * @return the network address
     */
    public String getNetwork() {
        return lookupValue(Params.NETWORK);
    }

    /**
     * Sets the network address.
     *
     * @param network the network address to set
     */
    public void setNetwork(String network) {
        setValue(Params.NETWORK, network);

    }

}
