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

import org.zoxweb.shared.data.SetNameDescriptionImpl;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SUS;

/**
 * Describes an IP address range.
 * Contains information about a network interface, starting IP address,
 * network mask, and count of addresses in the range.
 *
 * @author mnael
 */
@SuppressWarnings("serial")
public class IPRangeInfo
        extends SetNameDescriptionImpl {

    /**
     * Configuration parameters for IPRangeInfo.
     */
    public enum Params
            implements GetNVConfig {
        /**
         * The system network interface name
         */
        IFACE(NVConfigManager.createNVConfig("interface", "The system interface name", "Interface", true, true, String.class)),
        /**
         * The starting IP address of the range
         */
        NET_ADDRESS(NVConfigManager.createNVConfig("address", "The ip address", "InetAddress", true, true, String.class)),
        /**
         * The network mask for the IP range
         */
        NET_MASK(NVConfigManager.createNVConfig("netmask", "The network mask", "NetworkMask", true, true, String.class)),
        /**
         * The count of IP addresses in the range
         */
        COUNT(NVConfigManager.createNVConfig("count", "Count", "count", false, true, Integer.class)),

        ;

        private final NVConfig cType;

        Params(NVConfig c) {
            cType = c;
        }

        public NVConfig getNVConfig() {
            return cType;
        }

    }

    /**
     * NVConfigEntity definition for IPRangeInfo
     */
    public static final NVConfigEntity NVC_IP_RANGE_INFO = new NVConfigEntityPortable("ip_range_info", null, "IPRangeInfo", true, false, false, false, IPRangeInfo.class, SUS.extractNVConfigs(Params.values()), null, false, SetNameDescriptionImpl.NVC_NAME_DESCRIPTION_IMPL);

    /**
     * Default constructor.
     */
    public IPRangeInfo() {
        super(NVC_IP_RANGE_INFO);
    }

    /**
     * Constructor with all parameters.
     *
     * @param inet    the network interface name
     * @param startIP the starting IP address
     * @param mask    the network mask
     * @param count   the number of IP addresses in the range
     */
    public IPRangeInfo(String inet, String startIP, String mask, int count) {
        this();
        setNetworkInterface(inet);
        setStartingIP(startIP);
        setNetworkMask(mask);
        setIPCount(count);
    }

    /**
     * Returns the network interface name.
     *
     * @return the network interface name
     */
    public String getNetworkInterface() {
        return lookupValue(Params.IFACE);
    }

    /**
     * Sets the network interface name.
     *
     * @param iface the network interface name to set
     */
    public void setNetworkInterface(String iface) {
        setValue(Params.IFACE, iface);
    }

    /**
     * Returns the starting IP address of the range.
     *
     * @return the starting IP address
     */
    public String getStartingIP() {
        return lookupValue(Params.NET_ADDRESS);
    }

    /**
     * Sets the starting IP address of the range.
     *
     * @param ip the starting IP address to set
     */
    public void setStartingIP(String ip) {
        setValue(Params.NET_ADDRESS, ip);
    }

    /**
     * Returns the network mask.
     *
     * @return the network mask string
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
     * Returns the IP address count.
     *
     * @return the number of IP addresses in the range
     */
    public int getIPCount() {
        return lookupValue(Params.COUNT);
    }

    /**
     * Sets the IP address count.
     *
     * @param count the number of IP addresses in the range
     */
    public void setIPCount(int count) {
        setValue(Params.COUNT, count);
    }

//	public String toString() {
//		return "Interface " + netInterface + " IP " + startingIP + " mask "
//				+ networkMask + " count " + ipCounts;
//	}
//
//	// private static java.io.PrintStream o = System.out;
//	private int ipCounts;
//
//	private String startingIP;
//
//	private String networkMask;
//
//	private String netInterface;
}
