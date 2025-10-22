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

import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.*;

import java.util.List;

/**
 *
 */
@SuppressWarnings("serial")
public class APICredentialsDAO
        extends SetNameDescriptionDAO {

    public enum Params
            implements GetNVConfig {
        NVC_NAME(NVConfigManager.createNVConfig("name", "The reference id of the Object", "Name", true, true, true, false, String.class, null)),
        API_KEY(NVConfigManager.createNVConfig("api_key", "The API key.", "APIKey", true, true, String.class)),
        API_SECRET(NVConfigManager.createNVConfig("api_secret", "The API secret", "APISercret", true, true, false, String.class, FilterType.ENCRYPT)),
        EXTRA_PARAMETERS(NVConfigManager.createNVConfig("extra_parameters", "Extra configuration parameters", "ExtraParameters", false, true, true, String[].class, null)),
        ;

        private final NVConfig cType;

        Params(NVConfig c) {
            cType = c;
        }

        public NVConfig getNVConfig() {
            return cType;
        }

    }

    /**
     * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on API ConfigInfoDAO.
     */
    public static final NVConfigEntity NVC_CREDENTIALS_DAO = new NVConfigEntityPortable("api_credentials_dao",
            "API Key and Secret",
            "APICredentialsDAO",
            true,
            false,
            false,
            false,
            APICredentialsDAO.class,
            SharedUtil.extractNVConfigs(Params.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );

    /**
     * default constructor
     *
     */
    public APICredentialsDAO() {
        super(NVC_CREDENTIALS_DAO);
        // TODO Auto-generated constructor stub
    }


    public String getAPIKey() {
        return lookupValue(Params.API_KEY);
    }

    public void setAPIKey(String apiKey) {
        setValue(Params.API_KEY, apiKey);
    }


    public String getAPISecret() {
        return lookupValue(Params.API_SECRET);
    }

    public void setAPISecret(String apiSecret) {
        setValue(Params.API_SECRET, apiSecret);
    }


    @SuppressWarnings("unchecked")
    public ArrayValues<NVPair> getExtraParameters() {
        return (ArrayValues<NVPair>) lookup(Params.EXTRA_PARAMETERS);
    }

    /**
     * This method sets the service configuration parameters.
     *
     * @param configParams
     */
    public void setExtraParameters(List<NVPair> configParams) {
        getExtraParameters().add(configParams.toArray(new NVPair[0]), true);
    }


    public void setExtraParameters(ArrayValues<NVPair> configParams) {
        // TODO Auto-generated method stub
        getExtraParameters().add(configParams.values(), true);
    }

}
