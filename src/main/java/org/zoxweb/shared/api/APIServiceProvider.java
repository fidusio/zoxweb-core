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

import org.zoxweb.shared.util.CanonicalID;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.SetDescription;
import org.zoxweb.shared.util.SetName;

/**
 * The API service provider interface.
 * @author mzebib
 */
public interface APIServiceProvider<P, S>
	extends SetDescription, 
			SetName, 
			CanonicalID {

	/**
	 * Returns the data store configuration info.
	 * @return APIConfigInfo
	 */
	APIConfigInfo getAPIConfigInfo();
	
	/**
	 * Sets the data store configuration info.
	 * @param configInfo object
	 */
	void setAPIConfigInfo(APIConfigInfo configInfo);

	/*
	 * Connects to the data store
	 * @return native connection 
	 * @throws APIException in case of connection issues
	 */
	S connect()
			throws APIException;
	
	/**
	 * Create a new connection
	 * @return native connection 
	 * @throws APIException in case of connection issues
	 */
	P newConnection()
			throws APIException;
	
	/**
	 * Shuts down the data store.
	 * @throws APIException in case of closure issue
	 */
	void close()
			throws APIException;
	
	/**
	 * Checks if the store is active.
	 * @return true if active
	 */
	boolean isProviderActive();

	/**
	 * Returns the exception handler.
	 * @returnAPIExceptionHandler
	 */
	APIExceptionHandler getAPIExceptionHandler();
	
	/**
	 * Sets the exception handler.
	 * @param exceptionHandler object to map native exceptions to APIException
	 */
	void setAPIExceptionHandler(APIExceptionHandler exceptionHandler);
	
	/**
	 * Lookup the property type based on the GetName property.
	 * @param propertyName looking for 
	 * @return typed value
	 */
	<T> T lookupProperty(GetName propertyName);

	/**
	 * Returns last time it was used or accessed
	 * @return time in millis last time accessed
	 */
	long lastTimeAccessed();

	/**
	 * Returns the delta between NOW and last time the object was used
	 * @return time in millis for inactivity
	 */
	long inactivityDuration();
	
	/**
	 * @return true if the current api instance is busy
	 */
	boolean isBusy();

}