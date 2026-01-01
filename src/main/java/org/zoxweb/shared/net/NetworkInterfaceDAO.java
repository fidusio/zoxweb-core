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
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.util.List;

/**
 * Data access object for network interface information.
 * Stores network interface details including display name, MAC address,
 * and associated IP addresses.
 *
 * @author mnael
 * @see InetAddressDAO
 */
@SuppressWarnings("serial")
public class NetworkInterfaceDAO
        extends SetNameDAO {

    private static final NVConfig DISPLAY_NAME = NVConfigManager.createNVConfig("display_name", "The network interface display name", "DisplayName", true, false, String.class);
    private static final NVConfig MAC_ADDRESS = NVConfigManager.createNVConfig("mac_address", "The network interface mac address", "MACAddress", true, false, String.class);
    private static final NVConfig INET_ADDRESSES = NVConfigManager.createNVConfigEntity("inet_addresses", "The inet address associated with the network interface", "InetAddresses", true, false, InetAddressDAO[].class, ArrayType.LIST);

    /** NVConfigEntity definition for NetworkInterfaceDAO */
    public static final NVConfigEntity NVC_NETWORK_INTERFACE_DAO = new NVConfigEntityPortable(
            "network_interface_dao",
            null,
            "NetworkInterfaceDAO",
            true,
            false,
            false,
            false,
            NetworkInterfaceDAO.class,
            SharedUtil.toNVConfigList(DISPLAY_NAME, MAC_ADDRESS, INET_ADDRESSES),
            null,
            false,
            SetNameDAO.NVC_NAME_DAO
    );

    /**
     * Default constructor.
     */
    public NetworkInterfaceDAO() {
        super(NVC_NETWORK_INTERFACE_DAO);
    }

    /**
     * Returns the list of IP addresses associated with this interface.
     *
     * @return list of InetAddressDAO objects
     */
    public List<InetAddressDAO> getInetAddresses() {
        return lookupValue(INET_ADDRESSES);
    }

    /**
     * Sets the list of IP addresses associated with this interface.
     *
     * @param inetAddresses the list of IP addresses to set
     */
    public void setInetAddresses(List<InetAddressDAO> inetAddresses) {
        setValue(INET_ADDRESSES, inetAddresses);
    }

    /**
     * Returns the MAC address of this interface.
     *
     * @return the MAC address string
     */
    public String getMACAddress() {
        return lookupValue(MAC_ADDRESS);
    }

    /**
     * Sets the MAC address of this interface.
     *
     * @param macAddress the MAC address to set
     */
    public synchronized void setMACAddress(String macAddress) {
        setValue(MAC_ADDRESS, macAddress);
    }

    /**
     * Returns the display name of this interface.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return lookupValue(DISPLAY_NAME);
    }

    /**
     * Sets the display name of this interface.
     *
     * @param displayName the display name to set
     */
    public void setDisplayName(String displayName) {
        setValue(DISPLAY_NAME, displayName);
    }


}