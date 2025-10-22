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

import org.zoxweb.shared.util.GetName;

/**
 * The API service provider creator interface.
 *
 * @author mzebib
 */
public interface APIServiceProviderCreator
        extends GetName {

    /**
     * Creates empty configuration information parameters.
     *
     * @return APIConfigInfo
     */
    APIConfigInfo createEmptyConfigInfo();

    /**
     * Returns the exception handler.
     *
     * @return APIExceptionHandler
     */
    APIExceptionHandler getExceptionHandler();

    /**
     * Creates the API based on the configuration parameters.
     *
     * @param dataStore
     * @param apiConfig
     * @return APIServiceProvider
     * @throws APIException
     */
    APIServiceProvider<?, ?> createAPI(APIDataStore<?, ?> dataStore, APIConfigInfo apiConfig)
            throws APIException;

    /**
     * Returns the API token manager.
     *
     * @return the api token manager
     */
    APITokenManager getAPITokenManager();

    /**
     * Get the APISecurityManager
     * @return
     */
    //APISecurityManager<?> getAPISecurityManager();

    /**
     *
     * @param apiSecurityManager
     */
    //void setAPISecurityManager(APISecurityManager<?> apiSecurityManager);
}
