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

import org.zoxweb.shared.security.KeyMaker;
import org.zoxweb.shared.util.*;

import java.util.List;

/**
 * The API configuration information interface.
 * @author mzebib
 *
 */
public interface APIConfigInfo
    extends ReferenceID<String>,
            AccountID<String>,
            SubjectGUID<String>,
            SetName,
            SetDescription,
            TimeStampInterface,
            CanonicalID
{
	
	public enum OAuthVersion
	{
		NONE,
		OAUTH_1,
		OAUTH_2
	}
	
	
	
	/**
	 * @return returns the API type name.
	 */
	String getAPITypeName();

	
	/**
	 * This method sets the API type name.
	 * @param name
	 */
	void setAPITypeName(String name);
	
	/**
	 * @return service types.
	 */
	APIServiceType[] getServiceTypes();
	
	/**
	 * This method sets service types.
	 * @param serviceType
	 */
	void setServiceTypes(APIServiceType[] serviceType);

	/**
	 * @return the version.
	 */
	String getVersion();
	
	/**
	 * This method sets the version.
	 * @param version
	 */
	void setVersion(String version);
	
	/**
	 * @return the configuration parameters.
	 * @deprecated
	 */
	ArrayValues<NVPair> getConfigParameters();
	
	/**
	 * This method sets the configuration parameters.
	 * @param configParams
	 * @deprecated
	 */
	void setConfigParameters(List<NVPair> configParams);
	
	/**
	 * This method sets the configuration parameters.
	 * @param configParams
	 * @deprecated
	 */
	void setConfigParameters(ArrayValues<NVPair> configParams);
	
	/**
	 * The default location where to store or get information, this parameter is optional and depend on the  
	 * the API 
	 * @return the default location
	 */
	String getDefaultLocation();
	/**
	 * Set the default location
	 * 
	 * @param location
	 */
	void setDefaultLocation(String location);
	
	/**
	 * This method checks if the specified service type is supported.
	 * @param type
	 * @return true is the type is supported.
	 */
	boolean isServiceTypeSupported(APIServiceType type);
	
	/**
	 * @return status
	 */
	APIConfigStatus getStatus();
	
	/**
	 * Set the current status.
	 * @param status
	 */
	void setStatus(APIConfigStatus status);
	
	/**
	 * @return the oauth version
	 */
	OAuthVersion getOAuthVersion();
	
	/**
	 * Set the oauth version
	 * @param version
	 */
	void setOAuthVersion(OAuthVersion version);
	
	void setKeyMaker(KeyMaker keyMaker);
	KeyMaker getKeyMaker();
	
	void setAPISecurityManager(APISecurityManager<?> apiSM);
	APISecurityManager<?> getAPISecurityManager();


	/**
	 * Get the native configuration parameters
	 * @return
	 */
	NVGenericMap getProperties();
	
	
	
	

}
