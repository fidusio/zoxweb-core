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
package org.zoxweb.shared.data;

import org.zoxweb.shared.net.NetworkInterfaceDAO;
import org.zoxweb.shared.util.ArrayValues;
import org.zoxweb.shared.util.DomainID;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.SystemID;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 * This class defines the system information data access object used 
 * to set up systems.
 * @author mnael
 * @version $Revision: 1.25 $
 * {@code @lastModifiedBy} $Author: mnael $
 * {@code @lastModifiedAt} $Date: 2025/4/5 17:18:30 $
 */
@SuppressWarnings("serial")
public class SystemInfoDAO
	extends NVEntityContainerDAO
	implements  SystemID<String>, DomainID<String>
{
	
	public static final NVConfig SYSTEM_ID =  NVConfigManager.createNVConfig("canonical_id", "The system identifier","SystemID", true, false, true, String.class, null);
	public static final NVConfig DOMAIN_ID =  NVConfigManager.createNVConfig("domain_id", "The domain identifier","DomainID", false, true, String.class);
	public static final NVConfig SYSTEM_PROPS =  NVConfigManager.createNVConfig("system_properties", "The system properties","SystemProperties", false, true, true, String[].class, null);
	public static final NVConfig NETWORK_INTERFACES =  NVConfigManager.createNVConfigEntity("network_interfaces", "The network interfaces","NetworkInterfaces", false, true, true, false, NetworkInterfaceDAO.NVC_NETWORK_INTERFACE_DAO, ArrayType.GET_NAME_MAP);
	public static final NVConfig APPLICATION_PROPS =  NVConfigManager.createNVConfig("application_properties", "The application properties","ApplicationProperties", false, true, true, String[].class, null);
	//public static final NVConfig APPLICATION_DAOS =  NVConfigManager.createNVConfigEntity("content", "The application DAOs","ApplicationDAOs", false, true, NVEntity[].class, ArrayType.GET_NAME_MAP);
	public static final NVConfigEntity  NVC_SYSTEM_INFO_DAO = new NVConfigEntityLocal("system_info_dao", null , "SystemInfoDAO", true, false, false, false, SystemInfoDAO.class, SharedUtil.toNVConfigList(SYSTEM_ID, DOMAIN_ID, SYSTEM_PROPS, NETWORK_INTERFACES, APPLICATION_PROPS), null, false, NVEntityContainerDAO.NVC_NVENTITY_CONTAINER_DAO);
	
	/**
	 * The default constructor.
	 */
	public SystemInfoDAO()
	{
		//super(SharedUtil.merge( null, NVC_SYSTEM_INFO_DAO));
		super(NVC_SYSTEM_INFO_DAO);
	}
	
	/**
	 * Returns the system properties.
	 * @return system properties
	 */
	@SuppressWarnings("unchecked")
	public ArrayValues<NVPair> getSystemProperties() 
	{
		return lookup(SYSTEM_PROPS);
	}
	
	/**
	 * Sets the system properties.
	 * @param system
	 */
	public void setSystemProperties(ArrayValues<NVPair> system)
	{	
		getSystemProperties().add(system.values(), true);
	}
	
	/**
	 * Returns the ApplicationDAO objects.
	 * @return application daos
	 */
	public ArrayValues<NVEntity> getApplicationDAOs() 
	{
		return getContent();
	}
	
	/**
	 * Sets the ApplicationDAO objects.
	 * @param daos
	 */
	public void setApplicationDAOs(ArrayValues<NVEntity> daos)
	{
		getContent().add(daos.values(), true);
	}

	/**
	 * Returns the network interfaces.
	 * @return network interfaces array
	 */
	@SuppressWarnings("unchecked")
	public ArrayValues<NVEntity> getNetworkInterfaces()
	{
		return  (ArrayValues<NVEntity>) lookup(NETWORK_INTERFACES.getName());
	}

	/**
	 * Sets the network interfaces.
	 * @param networkInterfaces
	 */
//	public void setNetworkInterfaces(ArrayValues<NetworkInterfaceDAO>)
//	{
//		setValue(NVC_NETWORK_INTERFACES, nvem);
//	}

	/**
	 * Returns the system assigned ID (the ID is a globally assigned identifier).
	 * @return system id
	 */
	public String getSystemID() 
	{
		return getCanonicalID();
	}

	/**
	 * Sets the system assigned ID.
	 * @param systemID
	 */
	public void setSystemID(String systemID) 
	{
		setCanonicalID(systemID);
	}

	/**
	 * Returns canonical ID.
	 * @return canonical id
	 */
	@Override
	public String toCanonicalID() 
	{
		return SharedUtil.toCanonicalID( '-', getDomainID(), getName(), getSystemID());
	}

	/**
	 * Returns the application properties.
	 * @return properties
	 */
	@SuppressWarnings("unchecked")
	public ArrayValues<NVPair> getApplicationProperties() 
	{
		return (ArrayValues<NVPair>) lookup(APPLICATION_PROPS);
	}

	/**
	 * Sets the application properties.
	 * @param applicationProperties
	 */
	public void setApplicationProperties(ArrayValues<NVPair> applicationProperties) 
	{
		getApplicationProperties().add(applicationProperties.values(), true);
	}

	/**
	 * Returns the domain ID.
	 */
	@Override
	public String getDomainID()
	{
		return lookupValue(DOMAIN_ID);
	}

	/**
	 * Sets the domain ID.
	 * @param domainID
	 */
	@Override
	public void setDomainID(String domainID)
	{
		setValue(DOMAIN_ID, domainID);
	}
	
}