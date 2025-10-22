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

import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.util.List;

/**
 * The APITokenDAO contain the APIKeyDAO the APIConfigInfoDAO and the Generated token
 *
 */
@SuppressWarnings("serial")
public class APITokenDAO
        extends APIAccessTokenDAO
        implements DoNotExpose {

    public enum Params
            implements GetNVConfig {
        API_CREDENTIALS_DAO(NVConfigManager.createNVConfigEntity("api_credentials_dao", "The Crendentials.", "APICredentialsDAO", true, true, APICredentialsDAO.class, ArrayType.NOT_ARRAY)),
        API_CONFIG_INFO_DAO(NVConfigManager.createNVConfigEntity("api_config_info_dao", "The API config info", "APIConfigInfoDAO", true, true, APIConfigInfoDAO.class, ArrayType.NOT_ARRAY)),
        API_TOKEN(NVConfigManager.createNVConfig("api_token", "The api token", "APIToken", false, false, String.class)),
        //API_TOKEN_EXPIRATION(NVConfigManager.createNVConfig("api_token_expiration", "The api token expiration in real time","APITokenExpiration", false, false, Long.class)),
        EXTRA_PARAMETERS(NVConfigManager.createNVConfig("extra_parameters", "Extra configuration parameters", "ExtraParameters", false, true, true, String[].class, null)),
        ;

        private final NVConfig nvc;

        Params(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }

    }

    /**
     * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on API ConfigInfoDAO.
     */
    public static final NVConfigEntity NVC_TOKEN_DAO = new NVConfigEntityPortable("api_token_dao",
            "APITokenDAO",
            "APITokenDAO",
            true,
            false,
            false,
            false,
            APITokenDAO.class,
            SharedUtil.extractNVConfigs(Params.values()),
            null,
            false,
            APIAccessTokenDAO.NVC_API_ACCESS_TOKEN_DAO);

    public APITokenDAO() {
        super(NVC_TOKEN_DAO);
    }

    public APICredentialsDAO getAPICredentialsDAO() {
        return lookupValue(Params.API_CREDENTIALS_DAO);
    }

    public void setAPICredentialsDAO(APICredentialsDAO apiCredentials) {
        setValue(Params.API_CREDENTIALS_DAO, apiCredentials);
    }

    /**
     * Returns the APIConfigInfo.
     *
     * @return
     */
    public APIConfigInfo getAPIConfigInfo() {
        return lookupValue(Params.API_CONFIG_INFO_DAO);
    }

    /**
     * Sets the APIConfigInfo.
     *
     * @param configInfo
     */
    public void setAPIConfigInfo(APIConfigInfo configInfo) {
        setValue(Params.API_CONFIG_INFO_DAO, configInfo);
    }

    /**
     * Returns the API token.
     *
     * @return the api token
     * @Deprecated
     */
    public String getAPIToken() {
        return lookupValue(Params.API_TOKEN);
    }

    /**
     * Sets the API token.
     *
     * @param apiToken
     * @Deprecated
     */
    public void setAPIToken(String apiToken) {
        setValue(Params.API_TOKEN, apiToken);
    }

    /**
     * Returns extra configurations.
     *
     * @return configuration parameters
     */
    @SuppressWarnings("unchecked")
    public ArrayValues<NVPair> getExtraParameters() {
        return (ArrayValues<NVPair>) lookup(Params.EXTRA_PARAMETERS);
    }

    /**
     * Sets extra parameters.
     *
     * @param val
     */
    public void setExtraParameters(List<NVPair> val) {
        getExtraParameters().add(val.toArray(new NVPair[0]), true);
    }


}