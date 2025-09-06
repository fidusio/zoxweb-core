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

import org.zoxweb.shared.data.DataConst.DocumentStatus;
import org.zoxweb.shared.util.*;

import java.util.List;

@SuppressWarnings("serial")
abstract public class DocumentInfoDAO
        extends PropertyDAO {

    public enum Param
            implements GetNVConfig {
        DOCUMENT_STATUS(NVConfigManager.createNVConfig("document_status", "Document status", "DocumentStatus", true, true, DocumentStatus.class)),
        //API_CONFIG_INFO(NVConfigManager.createNVConfigEntity("api_config_info", "The APIConfigInfolocator","APIConfigInfo", true, false, APIConfigInfoDAO.NVC_API_CONFIG_INFO_DAO)),
        API_CONFIG_INFO_REF_ID(NVConfigManager.createNVConfig("api_config_info_ref_id", "The APIConfigLocator", "APIConfigInfoReferenceID", true, false, String.class)),
        IS_LINK(NVConfigManager.createNVConfig("is_link", "True is it is symbolic link", "SymbolicLink", true, false, Boolean.class)),
        HASH_SHA_256(NVConfigManager.createNVConfig("hash_sha_256", "The SHA-256 hash", "HashSha256", true, false, String.class)),
//		PROPERTIES(NVConfigManager.createNVConfig("properties", "Document unique properties", "DocumentExtraProperties", false, true, true, false, String[].class, null)),

        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_DOCUMENT_INFO_DAO = new NVConfigEntityPortable(
            "document_info_dao",
            null,
            "DocumentInfoDAO",
            true,
            false,
            false,
            false,
            PropertyDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            TimeStampDAO.NVC_TIME_STAMP_DAO
    );//,SharedUtil.extractNVConfigs( new Params[]{Params.REFERENCE_ID, Params.NAME, Params.LENGTH}));


    protected DocumentInfoDAO(NVConfigEntity nvce) {
        super(nvce);
    }
//	
//	/**
//	 * Set the URL location where the file is stored.
//	 * @param location
//	 */
//	public void setAPIConfigInfo(APIConfigInfo aci) 
//	{
//		setValue(Params.API_CONFIG_INFO, aci);
//	}
//	
//	/**
//	 * Get the URL location where the file is stored.
//	 * @return
//	 */
//	public APIConfigInfo getAPIConfigInfo() 
//	{
//		return lookupValue( Params.API_CONFIG_INFO);
//	}

    /**
     * Returns the document status.
     * @return status
     */
    public DocumentStatus getStatus() {
        return lookupValue(Param.DOCUMENT_STATUS);
    }

    /**
     * Sets the document status.
     * @param status
     */
    public void setStatus(DocumentStatus status) {
        setValue(Param.DOCUMENT_STATUS, status);
    }

    /**
     * Returns the URL location where the document is stored.
     * @return the api config reference id
     */
    public String getAPIConfigInfoReferenceID() {
        return lookupValue(Param.API_CONFIG_INFO_REF_ID);
    }

    /**
     * Sets the URL location where the document is stored.
     * @param aci
     */
    public void setAPIConfigInfoReferenceID(String aci) {
        setValue(Param.API_CONFIG_INFO_REF_ID, aci);
    }

//	/**
//	 * Returns the document properties.
//	 * @return config properties
//	 */
//	@SuppressWarnings("unchecked")
//	public ArrayValues<NVPair> getProperties()
//	{
//		return (ArrayValues<NVPair>) lookup(Param.PROPERTIES.getNVConfig().getName());
//	}

    /**
     * Sets the document properties.
     * @param properties
     */
    public void setProperties(ArrayValues<NVPair> properties) {

        getProperties().clear();
        for (NVPair nvp : properties.values())
            getProperties().build(nvp);
//		ArrayValues<NVPair> arrayValues = (ArrayValues<NVPair>) lookup(Param.PROPERTIES.getNVConfig().getName());
//		arrayValues.add(properties.values(), true);
        //setValue( Params.PROPERTIES, fileProps);
    }

    /**
     * Sets the document properties.
     * @param properties
     */
    public void setProperties(List<NVPair> properties) {
        getProperties().clear();
        for (NVPair nvp : properties)
            getProperties().build(nvp);

//
//		@SuppressWarnings("unchecked")
//		ArrayValues<NVPair> arrayValues = (ArrayValues<NVPair>) lookup(Param.PROPERTIES.getNVConfig().getName());
//		arrayValues.add(properties.toArray(new NVPair[0]), true);
        //setValue( Params.PROPERTIES, fileProps);
    }

    /**
     * Check if link.
     * @return true if a link
     */
    public boolean isLink() {
        return lookupValue(Param.IS_LINK);
    }

    /**
     * Sets if link.
     * @param link
     */
    public void setLink(boolean link) {
        setValue(Param.IS_LINK, link);
    }

    /**
     * Returns the HashSHA256.
     * @return sha256
     */
    public String getHashSHA256() {
        return lookupValue(Param.HASH_SHA_256);
    }

    /**
     * Sets the HashSHA256.
     * @param sha256
     */
    public void setHashSHA256(String sha256) {
        setValue(Param.HASH_SHA_256, sha256);
    }

}