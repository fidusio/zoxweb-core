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
package org.zoxweb.shared.api;

import org.zoxweb.shared.security.KeyMaker;
import org.zoxweb.shared.security.SecurityControllerHolder;
import org.zoxweb.shared.util.*;

/**
 * The API configuration information interface.
 *
 * @author mzebib
 *
 */
public interface APIConfigInfo
        extends ReferenceID<String>,
        SetName,
        SetDescription,
        TimeStampInterface,
        GetNVProperties,
        CanonicalID,
        SecurityControllerHolder {

     enum OAuthVersion {
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
     *
     * @param name
     */
    void setAPITypeName(String name);

    /**
     * @return service types.
     */
    APIServiceType[] getServiceTypes();

    /**
     * This method sets service types.
     *
     * @param serviceType
     */
    void setServiceTypes(APIServiceType[] serviceType);

    /**
     * @return the version.
     */
    String getVersion();

    /**
     * This method sets the version.
     *
     * @param version
     */
    void setVersion(String version);


    /**
     * This method sets the configuration parameters.
     *
     * @param configParams the parameters as NVPair
     */
    @Deprecated
    void setConfigParameters(ArrayValues<NVPair> configParams);


    /**
     * The default location where to store or get information, this parameter is optional and depend on the API
     *
     * @return the default location
     */
    String getDefaultLocation();

    /**
     * Set the default location
     *
     * @param location where the api exists
     */
    void setDefaultLocation(String location);

    /**
     * This method checks if the specified service type is supported.
     *
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
     *
     * @param status
     */
    void setStatus(APIConfigStatus status);

    /**
     * @return the oauth version
     */
    OAuthVersion getOAuthVersion();

    /**
     * Set the oauth version
     *
     * @param version
     */
    void setOAuthVersion(OAuthVersion version);

    void setKeyMaker(KeyMaker keyMaker);

    KeyMaker getKeyMaker();


}
