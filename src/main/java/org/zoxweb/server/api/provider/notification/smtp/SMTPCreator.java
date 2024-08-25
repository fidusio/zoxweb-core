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
package org.zoxweb.server.api.provider.notification.smtp;

import java.util.List;

import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.filters.GetValueFilter;
import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.api.APIConfigInfo;
import org.zoxweb.shared.api.APIConfigInfoDAO;
import org.zoxweb.shared.api.APIDataStore;
import org.zoxweb.shared.api.APIException;
import org.zoxweb.shared.api.APIExceptionHandler;

import org.zoxweb.shared.api.APIServiceProvider;
import org.zoxweb.shared.api.APIServiceProviderCreator;
import org.zoxweb.shared.api.APIServiceType;
import org.zoxweb.shared.api.APITokenManager;

/**
 * The Simple Mail Transfer Protocol (SMTP) creator class is used to set up the 
 * SMTP service provider.
 */
public class SMTPCreator 
	implements APIServiceProviderCreator
{

	public final static String API_NAME = "SMTP";
//	public static final DynamicEnumMap AUTHENTICATION = new DynamicEnumMap("Authentication", new NVPair("NONE", "None"), new NVPair("SSL","SSL"), new NVPair("TLS", "TLS"));
//	public static final DynamicEnumMap MESSAGE_FORMAT = new DynamicEnumMap("MessageFormat", new NVPair("TEXT", "Text"), new NVPair("HTML", "Html"));


//	public static final NVGenericMap SMTP_PARAM = new NVGenericMap("SMTP_API").
//			build("Username", null).
//			build(new NVPair("Password", null, FilterType.ENCRYPT_MASK)).
//			build("Host", null).
//			build(new NVInt("Port", 465)).
//			build()
	/**
	 * Contains SMTP provider variables.
	 */
	public enum Param 
		implements GetNameValue<String>, GetValueFilter<String, String>
	{
		USERNAME("Username", null, FilterType.CLEAR),
		PASSWORD("Password", null, FilterType.ENCRYPT_MASK),
		HOST("Host", null, FilterType.CLEAR),
		PORT("Port", "465", FilterType.INTEGER),
		AUTHENTICATION("Authentication", null, new DynamicEnumMap("Authentication", new NVPair("NONE", "None"), new NVPair("SSL","SSL"), new NVPair("TLS", "TLS"))),
		FORMAT("Message Format", null, new DynamicEnumMap("MessageFormat", new NVPair("TEXT", "Text"), new NVPair("HTML", "Html")))
		
		;

		private final String name;
		private final String value;
		private final ValueFilter<String, String> vf;
		
		Param(String name, String value, ValueFilter<String, String> vf)
		{
			this.name = name;
			this.value = value;
			this.vf = vf;
		}
		
	
		public String getName() 
		{
			return name;
		}

		
		public String getValue() 
		{
			return value;
		}
		
		public ValueFilter<String, String> getValueFilter()
		{
			return vf;
		}
	}
	
	/**
	 * Creates an empty configuration information parameters.
	 * @return APIConfigInfo
	 */
	@SuppressWarnings("unchecked")
	public APIConfigInfo createEmptyConfigInfo()
	{
		APIConfigInfoDAO configInfo = new APIConfigInfoDAO();
		List<NVPair> list = (List<NVPair>) SharedUtil.toNVPairs(Param.values());
		configInfo.setAPITypeName(API_NAME);
		configInfo.setDescription("SMTP (Simple Mail Transfer Protocol) configuration is used for email set up to send and receive emails.");
		configInfo.setVersion("2020");
		configInfo.setConfigParameters(list);
		
		APIServiceType[] types = {APIServiceType.EMAIL_NOTIFICATION};
		//APIServiceType.SMS_NOTIFICATION, APIServiceType.VOICE_NOTIFCATION
		configInfo.setServiceTypes(types);
		
		NVPairList nvpl = (NVPairList) configInfo.lookup(APIConfigInfoDAO.Params.CONFIGURATION_PARAMETERS.getNVConfig().getName());
		nvpl.setFixed(true);
		
		return configInfo;
	}

	/**
	 * Returns the exception handler.
	 * @return APIExceptionHandler
	 */

	public APIExceptionHandler getExceptionHandler()
	{
		return SMTPExceptionHandler.SINGLETON;
	}

	/**
	 * Creates API based on configuration information parameters.
	 * @param apiConfig
	 * @return APIServiceProvider
	 */
	
	public APIServiceProvider<Void> createAPI(APIDataStore<?> dataStore, APIConfigInfo apiConfig)
        throws APIException
	{
		APIServiceProvider<Void> serviceProvider = new SMTPProvider();
		serviceProvider.setAPIConfigInfo(apiConfig);
		serviceProvider.setAPIExceptionHandler(SMTPExceptionHandler.SINGLETON);
		return serviceProvider;
	}


	public String getName() 
	{
		return API_NAME;
	}


	public APITokenManager getAPITokenManager() 
	{
		return null;
	}

//	@Override
//	public APISecurityManager<?> getAPISecurityManager() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public void setAPISecurityManager(APISecurityManager<?> apiSecurityManager) {
//		// TODO Auto-generated method stub
//		
//	}

}