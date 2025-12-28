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
 * The ip filter object is used to mask and filter ip sets
 *
 * @author mnael
 */
@SuppressWarnings("serial")
public class InetFilterDAO
        extends SetNameDescriptionDAO {

    public enum Params
            implements GetNVConfig {
        IP(NVConfigManager.createNVConfig("ip_address", "IP Address", "IP", false, true, String.class)),
        //FILTER(NVConfigManager.createNVConfig("filter", "Filter", "Filter", false, true, String.class)),
        NET_MASK(NVConfigManager.createNVConfig("network_mask", "Network Mask", "NetMask", false, true, String.class)),
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

    public static final NVConfigEntity NVC_INET_FILTER_DAO = new NVConfigEntityPortable("inet_filter_dao", null, "InetFilterDAO", true, false, false, false, InetFilterDAO.class, SharedUtil.extractNVConfigs(Params.values()), null, false, SetNameDescriptionDAO.NVC_NAME_DAO);

    /**
     * Default constructor for the bean consistency
     *
     */
    public InetFilterDAO() {
        super(NVC_INET_FILTER_DAO);
    }

    /**
     * Create a filter based on the filter object
     *
     * @param filter
     */
//	public InetFilterDAO(String filter)
//	{
//		this();
//		setFilter(filter);
//	}


    /**
     * Create a filter based on the ip address and the nework mask
     * @param ip
     * @param mask
     */
    public InetFilterDAO(String ip, String mask) {
        this();
        setIP(ip);
        setNetworkMask(mask);
    }

    /**
     * Get the current up filter
     *
     * @return
     */
//	public String getFilter() 
//	{
//		return lookupValue(Params.FILTER);
//	}

    /**
     * Set the filter
     *
     * @param filter
     */
//	public void setFilter(String filter)
//	{
//		setValue(Params.FILTER, filter);
//	}

    /**
     * Get the ip, null if no set
     * @return the ip
     */
    public String getIP() {
        return lookupValue(Params.IP);
    }

    /**
     * Set the ip
     * @param ip
     */
    public void setIP(String ip) {
        setValue(Params.IP, ip);
    }

    /**
     * Get the network mask, null if not set
     * @return network mask
     */
    public String getNetworkMask() {
        return lookupValue(Params.NET_MASK);
    }

    /**
     * Set the network mask.
     * @param mask
     */
    public void setNetworkMask(String mask) {
        setValue(Params.NET_MASK, mask);
    }

    public String getNetwork() {
        return lookupValue(Params.NETWORK);
    }

    public void setNetwork(String network) {
        setValue(Params.NETWORK, network);

    }

}
