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

import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.security.KeyMaker;
import org.zoxweb.shared.security.SecurityController;
import org.zoxweb.shared.util.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements API configuration information interface to 
 * create an API configuration information data access object.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class APIConfigInfoDAO 
	extends PropertyDAO
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
		ACCOUNT_ID(NVConfigManager.createNVConfig(MetaToken.ACCOUNT_ID.getName(), "The account id","AccountID", true, false, false, true, true, String.class, null)),
		API_TYPE_NAME(NVConfigManager.createNVConfig("api_type_name", "The name of the API type.","APITypeName", false, false, String.class)),
		OAUTH_VERSION(NVConfigManager.createNVConfig("oauth_version", "OAuth version","OAuthVersion", false, false, OAuthVersion.class)),
		//DESCRIPTION(NVConfigManager.createNVConfig("description", null, "Description", false, true, String.class)),
		SERVICE_TYPE(NVConfigManager.createNVConfig("service_type", "Type of service", "ServiceType", true, false, APIServiceType[].class)),
		VERSION(NVConfigManager.createNVConfig("api_version", "API version", "CurrentVersion", false, false, String.class)),
		DEFAULT_LOCATION(NVConfigManager.createNVConfig("default_location", "Default storage location", "DefaultLocation", false, true, String.class)),
		CONFIGURATION_PARAMETERS(NVConfigManager.createNVConfig("configuration_parameters", "API configuration parameters", "APIConfigurationParameters", true, true, false, String[].class, null)),
//		PROPERTIES(NVConfigManager.createNVConfig("properties", "Configuration properties", "Properties", false, true, NVGenericMap.class)),
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
	//private volatile APISecurityManager<?, ?, ?> apiSecurityManager = null;
	private volatile SecurityController securityController = null;
	/**
	 * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on API ConfigInfoDAO.
	 */
	public static final NVConfigEntity NVC_API_CONFIG_INFO_DAO = new NVConfigEntityPortable
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
																		PropertyDAO.NVC_PROPERTY_DAO
																	);
	
	
	/**
	 * This is the default constructor.
	 */
	public APIConfigInfoDAO()
	{
		super(NVC_API_CONFIG_INFO_DAO);
	}

	
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
	 * @param apiTypeName type name
	 */
	@Override
	public void setAPITypeName(String apiTypeName)
	{
		setValue(Params.API_TYPE_NAME, apiTypeName);
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
	 * @param serviceTypes used
	 */
	@Override
	public void setServiceTypes(APIServiceType[] serviceTypes)
	{
		List<APIServiceType> list = new ArrayList<APIServiceType>();
		
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
	public ArrayValues<NVPair> getConfigParameters()
	{
		return lookup(Params.CONFIGURATION_PARAMETERS);
	}

	/**
	 * This method sets the service configuration parameters.
	 * @param configParams
	 */
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

//	@Override
//	public void setAPISecurityManager(APISecurityManager<?, ?, ?> apiSM)
//	{
//		apiSecurityManager = apiSM;
//	}
//
//	@Override
//	public APISecurityManager<?, ?, ?> getAPISecurityManager()
//	{
//		return apiSecurityManager;
//	}

	/**
	 * Returns the account ID.
	 * @return account id
	 */
//	@Column(name = "account_id")
	@Override
	public String getAccountID()
	{
		return lookupValue(Params.ACCOUNT_ID);
	}

	/**
	 * Sets the account ID.
	 * @param accountID
	 */
	@Override
	public void setAccountID(String accountID)
	{
		setValue(Params.ACCOUNT_ID, accountID);
	}


	/**
	 * @return
	 */
	@Override
	public SecurityController getSecurityController() {
		return securityController;
	}

	/**
	 * @param securityController
	 */
	@Override
	public void setSecurityController(SecurityController securityController) {
		this.securityController = securityController;
	}


}