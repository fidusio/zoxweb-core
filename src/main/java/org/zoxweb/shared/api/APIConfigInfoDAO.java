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
package org.zoxweb.shared.api;

import java.util.ArrayList;
import java.util.List;

import org.zoxweb.shared.data.TimeStampDAO;
import org.zoxweb.shared.security.KeyMaker;
import org.zoxweb.shared.util.*;

/**
 * This class implements API configuration information interface to 
 * create an API configuration information data access object.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class APIConfigInfoDAO 
	extends TimeStampDAO 
	implements APIConfigInfo
{

	/**
	 * This enum contains the following variables:
	 * service type, version and configuration parameters
	 * @author mzebib
	 *
	 */
	public enum Params
		implements GetNVConfig
	{
		API_TYPE_NAME(NVConfigManager.createNVConfig("api_type_name", "The name of the API type.","APITypeName", false, false, String.class)),
		OAUTH_VERSION(NVConfigManager.createNVConfig("oauth_version", "OAuth version","OAuthVersion", false, false, OAuthVersion.class)),
		//DESCRIPTION(NVConfigManager.createNVConfig("description", null, "Description", false, true, String.class)),
		SERVICE_TYPE(NVConfigManager.createNVConfig("service_type", "Type of service", "ServiceType", true, false, APIServiceType[].class)),
		VERSION(NVConfigManager.createNVConfig("api_version", "API version", "CurrentVersion", false, false, String.class)),
		DEFAULT_LOCATION(NVConfigManager.createNVConfig("default_location", "Default storage location", "DefaultLocation", false, true, String.class)),
		CONFIGURATION_PARAMETERS(NVConfigManager.createNVConfig("configuration_parameters", "API configuration parameters", "APIConfigurationParameters", true, true, false, String[].class, null)),
		PROPERTIES(NVConfigManager.createNVConfig("properties", "Configuration properties", "Properties", false, true, NVGenericMap.class)),
		//API_KEY_REF_ID(NVConfigManager.createNVConfig("api_key_ref_id", "API key reference id", "APIKeyRefID", true, true, false, true, String.class, null)),
		;
		
		private final NVConfig cType;
		
		Params(NVConfig c)
		{
			cType = c;
		}
		
		public NVConfig getNVConfig() 
		{
			return cType;
		}

	}
	
	// This variable should not be part of the NVConfig definition;
	private APIConfigStatus status = APIConfigStatus.INACTIVE;	
	private volatile KeyMaker keyMaker = null;
	private volatile APISecurityManager<?> apiSecurityManager = null;
	/**
	 * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on API ConfigInfoDAO.
	 */
	public static final NVConfigEntity NVC_API_CONFIG_INFO_DAO = new NVConfigEntityLocal
																	(
																		"api_config_info_dao",
																		null, 
																		"APIConfigInfoDAO", 
																		true, 
																		false,
																		false,
																		false,
																		APIConfigInfoDAO.class,
																		SharedUtil.extractNVConfigs(Params.values()),
																		null, 
																		false,
																		TimeStampDAO.NVC_TIME_STAMP_DAO
																	);
	
	
	/**
	 * This is the default constructor.
	 */
	public APIConfigInfoDAO()
	{
		super(NVC_API_CONFIG_INFO_DAO);
	}

	/**
	 * This constructor instantiates API configuration information DAOO based on list of NVConfigEntity type.
	 * @param list
	 */
//	protected APIConfigInfoDAO(List<NVConfigEntity> list)
//	{
//		super(SharedUtil.merge(list, NVC_API_CONFIG_INFO_DAO));
//	}
	
	/**
	 * @return the API type name.
	 */
	@Override
	public String getAPITypeName()
	{
		return lookupValue(Params.API_TYPE_NAME);
	}

	/**
	 * This method sets the API type name.
	 * @param name
	 */
	@Override
	public void setAPITypeName(String name) 
	{
		setValue(Params.API_TYPE_NAME, name);
	}
	
	/**
	 * @return the supported service types.
	 */
	@Override
	public APIServiceType[] getServiceTypes() 
	{
		List<APIServiceType > list = lookupValue(Params.SERVICE_TYPE);
		
		return list.toArray(new APIServiceType[0]);
	}

	/**
	 * This method sets the service type.
	 * @param serviceTypes
	 */
	@Override
	public void setServiceTypes(APIServiceType[] serviceTypes)
	{
		List<APIServiceType > list = new ArrayList<APIServiceType>();
		
		if (serviceTypes != null)
		{
			for (APIServiceType serviceType : serviceTypes)
			{
				list.add(serviceType);
			}
		}
		
		setValue(Params.SERVICE_TYPE, list);
	}

	/**
	 * @return The configuration parameters
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ArrayValues<NVPair> getConfigParameters()
	{
		return (ArrayValues<NVPair>) lookup(Params.CONFIGURATION_PARAMETERS);
	}

	/**
	 * This method sets the service configuration parameters.
	 * @param configParams
	 */
	@Override
	public void setConfigParameters(List<NVPair> configParams) 
	{
		getConfigParameters().add(configParams.toArray(new NVPair[0]), true);
	}
	
	/**
	 * 
	 */
	@Override
	public void setConfigParameters(ArrayValues<NVPair> configParams)
	{
		getConfigParameters().add(configParams.values(), true);
	}

	/**
	 * Get the configuration properties
	 * @return
	 */
	@Override
	public NVGenericMap getProperties()
	{
		return (NVGenericMap)lookup(Params.PROPERTIES);
	}

	/**
	 * @return The version
	 */
	@Override
	public String getVersion()
	{
		return lookupValue(Params.VERSION);
	}

	/**
	 * This method sets the version.
	 * @param version
	 */
	@Override
	public void setVersion(String version) 
	{
		setValue(Params.VERSION, version);	
	}
	
	/**
	 * This method returns a string representation of the class.
	 * (return domain ID/account ID/user ID or profile).
	 * @return the canonical identifier
	 */
	@Override
	public String toCanonicalID()
	{
		return null;
	}

	/**
	 * This method checks if the service type is supported.
	 * @param type
	 * @return true if type is supported
	 */
	@Override
	public boolean isServiceTypeSupported(APIServiceType type) 
	{
		List<APIServiceType > list = lookupValue(Params.SERVICE_TYPE);
		
		for (APIServiceType serviceType : list)
		{
			if (serviceType == type)
			{
				return true;
			}
		}
		
		return false;
	}

	/**
	 * @see org.zoxweb.shared.api.APIConfigInfo#getStatus()
	 */
	@Override
	public APIConfigStatus getStatus()
	{
		return status;
	}

	/**
	 * @see org.zoxweb.shared.api.APIConfigInfo#setStatus(org.zoxweb.shared.api.APIConfigStatus)
	 */
	@Override
	public void setStatus(APIConfigStatus status)
	{
		this.status = status;
	}
	
	/**
	 * @see org.zoxweb.shared.api.APIConfigInfo#getStatus()
	 */
	@Override
	public OAuthVersion getOAuthVersion()
	{
		return lookupValue(Params.OAUTH_VERSION);
	}

	
	/**
	 * @see org.zoxweb.shared.api.APIConfigInfo#setStatus(org.zoxweb.shared.api.APIConfigStatus)
	 */
	@Override
	public void setOAuthVersion(OAuthVersion version)
	{
		setValue(Params.OAUTH_VERSION, version);
	}

	/**
	 * @see org.zoxweb.shared.api.APIConfigInfo#getDefaultLocation()
	 */
	@Override
	public String getDefaultLocation()
    {
		return lookupValue(Params.DEFAULT_LOCATION);
	}

	/**
	 * @see org.zoxweb.shared.api.APIConfigInfo#setDefaultLocation(java.lang.String)
	 */
	@Override
	public void setDefaultLocation(String location) 
	{
		setValue(Params.DEFAULT_LOCATION, location);
	}

	@Override
	public void setKeyMaker(KeyMaker keyMaker)
	{
		this.keyMaker = keyMaker;
	}

	@Override
	public KeyMaker getKeyMaker()
	{
		return keyMaker;
	}

	@Override
	public void setAPISecurityManager(APISecurityManager<?> apiSM)
	{
		apiSecurityManager = apiSM;
	}

	@Override
	public APISecurityManager<?> getAPISecurityManager()
	{
		return apiSecurityManager;
	}

//	/**
//	 * @see org.zoxweb.shared.api.APIConfigInfo#getAPIKeyRefID()
//	 */
//	@Override
//	public String getAPIKeyRefID()
//	{
//		return lookupValue(Params.API_KEY_REF_ID);
//	}
//
//	/**
//	 * @see org.zoxweb.shared.api.APIConfigInfo#setAPIKeyRefID(java.lang.String)
//	 */
//	@Override
//	public void setAPIKeyRefID(String apiKeyRefID)
//	{
//		setValue(Params.API_KEY_REF_ID, apiKeyRefID);
//		
//	}


}