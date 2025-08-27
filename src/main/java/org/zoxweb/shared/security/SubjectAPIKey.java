
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
package org.zoxweb.shared.security;


import org.zoxweb.shared.data.TimeStampDAO;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.Const.Status;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

import java.util.Date;




/**
 * Created on 7/13/17
 */
@SuppressWarnings("serial")
public class SubjectAPIKey
    extends TimeStampDAO
    implements SubjectID<String>, SystemID<String>{

    public enum Param
        implements GetNVConfig {

        //SUBJECT_ID(NVConfigManager.createNVConfig("subject_id", "Subject ID", "SubjectID", true, false, true, String.class, null)),
        SYSTEM_ID(NVConfigManager.createNVConfig("system_id", "System ID", "SystemID", true, false, String.class)),
        API_KEY(NVConfigManager.createNVConfig("api_key", "API Key", "APIKey", true, false, false, String.class, FilterType.ENCRYPT)),
        STATUS(NVConfigManager.createNVConfig("status", "Status", "Status", true, false, Status.class)),
        TS_REQUIRED(NVConfigManager.createNVConfig("ts_required", "The timestamp is required", "TimeStampRequired", false, false, Boolean.class)),
        EXPIRY_DATE(NVConfigManager.createNVConfig("expiry_date", "The expiry timestamp", "Expired", false, false, false, true, Date.class, null)),
        PROPERTIES(NVConfigManager.createNVConfig("properties", "Properties", "Properties", false, true, NVGenericMap.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        @Override
        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_SUBJECT_API_KEY = new NVConfigEntityPortable(
            "subject_api_key",
            null,
            SubjectAPIKey.class.getSimpleName(),
            true,
            false,
            false,
            false,
            SubjectAPIKey.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            TimeStampDAO.NVC_TIME_STAMP_DAO
    );

    public SubjectAPIKey() {
        super(NVC_SUBJECT_API_KEY);
    }

    protected SubjectAPIKey(NVConfigEntity nvce) {
        super(nvce);
    }

    @Override
    public String getSubjectID() {
        return getSubjectGUID();
    }

    @Override
    public void setSubjectID(String id) {
      setSubjectGUID(id);
    }

//    public String getClientID() {
//        return lookupValue(Param.CLIENT_ID);
//    }
    
    
//    public byte[] getAPIKeyAsBytes()
//    {
//    	String key = getAPIKey();
//    	if (key != null)
//        {
//            return SharedStringUtil.getBytes(key);
//        }
//
//    	return null;
//    }

//    public void setClientID(String clientID) 
//    {
//    	 setValue(Param.CLIENT_ID, clientID);
//        //setValue(Param.API_KEY, apiKey);
//    }
    
    
    public void setAPIKey(byte[] secret)
    {
    	 setValue(Param.API_KEY, SharedBase64.encodeAsString(Base64Type.URL, secret));
    }
    
    public String getAPIKey() {
        return lookupValue(Param.API_KEY);
    }
    
    public byte[] getAPIKeyAsBytes()
    {
    	String secret = getAPIKey();
    	if (secret != null)
        {
            return SharedBase64.decode(Base64Type.URL, secret);
        }

    	return null;
    }
    
    /**
     * Returns the status.
     * @return
     */
    public Status getStatus() {
        return lookupValue(Param.STATUS);
    }

    /**
     * Sets the status.
     * @param status
     */
    public void setStatus(Status status) {
        setValue(Param.STATUS, status);
    }

    public static SubjectAPIKey copy(SubjectAPIKey subjectAPIKey) {
        SUS.checkIfNulls("SubjectAPIKey is null.", subjectAPIKey);

        SubjectAPIKey ret = new SubjectAPIKey();
        ret.setSubjectID(subjectAPIKey.getSubjectID());
        ret.setAPIKey(subjectAPIKey.getAPIKeyAsBytes());

        return ret;
    }
    
    
    /**
	 * Returns the last time (in milliseconds) the file was read.
	 * @return last time read 
	 */
	public long getExpiryDate() 
	{
		return lookupValue(Param.EXPIRY_DATE);
	}
	
	/**
	 * Sets the last time (in milliseconds) the file was read.
	 * @param ts
	 */
	public void setExpiryDate(long ts) 
	{
		setValue(Param.EXPIRY_DATE, ts);
	}
	
	public boolean isTimeStampRequired()
    {
        return lookupValue(Param.TS_REQUIRED);
    }
    
    
    public void setTimeStampRquired(boolean tsReq) 
    {
        setValue(Param.TS_REQUIRED, tsReq);
    }

    @Override
    public String getSystemID() {
      // TODO Auto-generated method stub
      return lookupValue(Param.SYSTEM_ID);
    }

    @Override
    public void setSystemID(String systemID) {
      // TODO Auto-generated method stub
      setValue(Param.SYSTEM_ID, systemID);
    }

    
    public NVGenericMap getProperties() {
      // TODO Auto-generated method stub
       return (NVGenericMap) lookup(Param.PROPERTIES.getNVConfig());
    }
}