/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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

import java.util.List;

import org.zoxweb.shared.data.SetNameDAO;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

@SuppressWarnings("serial")
public class NetworkInterfaceDAO
    extends SetNameDAO
{

	
	
	private static final NVConfig DISPLAY_NAME = NVConfigManager.createNVConfig("display_name", "The network interface display name","DisplayName",true, false, String.class);
	private static final NVConfig MAC_ADDRESS  = NVConfigManager.createNVConfig("mac_address", "The network interface mac address","MACAddress",true, false, String.class);
	private static final NVConfig INET_ADDRESSES  = NVConfigManager.createNVConfigEntity("inet_addresses", "The inet address associated with the network interface","InetAddresses",true, false, InetAddressDAO[].class, ArrayType.LIST);
	
	public static final NVConfigEntity NVC_NETWORK_INTERFACE_DAO = new NVConfigEntityPortable(
	        "network_interface_dao",
            null ,
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

	public NetworkInterfaceDAO()
    {
		super(NVC_NETWORK_INTERFACE_DAO);
	}

	public List<InetAddressDAO> getInetAddresses()
    {
		return lookupValue(INET_ADDRESSES);
	}

	public void setInetAddresses(List<InetAddressDAO> inetAddresses)
    {
		setValue(INET_ADDRESSES,inetAddresses );
	}
	
	public String getMACAddress()
    {
		return lookupValue(MAC_ADDRESS);
	}
	
	public synchronized void setMACAddress(String macAddress)
    {
		setValue(MAC_ADDRESS, macAddress);
	}
	
	public String getDisplayName()
    {
		return lookupValue(DISPLAY_NAME);
	}
	
	public void setDisplayName(String displayName)
    {
		setValue(DISPLAY_NAME, displayName);
	}

	

}