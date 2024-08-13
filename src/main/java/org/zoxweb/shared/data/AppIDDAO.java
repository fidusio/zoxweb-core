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

import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.security.shiro.ShiroBase;
import org.zoxweb.shared.filters.AppIDNameFilter;
import org.zoxweb.shared.util.AppGlobalID;
import org.zoxweb.shared.util.AppID;
import org.zoxweb.shared.util.CanonicalID;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

/**
 * Created on 7/3/17
 */
@SuppressWarnings("serial")
public class AppIDDAO
    extends SetNameDescriptionDAO
    implements AppID<String>, CanonicalID, AppGlobalID<String>
{

    public enum Param
        implements GetNVConfig
    {
        APP_ID(NVConfigManager.createNVConfig("app_id", "App ID","AddID", true, false, false, String.class, AppIDNameFilter.SINGLETON)),
        DOMAIN_ID(NVConfigManager.createNVConfig("domain_id", "Domain ID", "Domain ID", true, true, false, String.class, FilterType.DOMAIN)),
        SUBJECT_ID(NVConfigManager.createNVConfig("subject_id", "Subject ID", "Subject ID", true, false, true, String.class, null)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc)
        {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig()
        {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_APP_ID_DAO = new NVConfigEntityLocal(
            "app_id_dao",
            "AppIDDAO" ,
            AppIDDAO.class.getSimpleName(),
            true, false,
            false, false,
            AppIDDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );

    /**
     * The default constructor.
     */
    public AppIDDAO()
    {
        super(NVC_APP_ID_DAO);
    }
    
    protected AppIDDAO(NVConfigEntity nvce)
    {
    	 super(nvce);
    }

    public AppIDDAO(String domainID, String appID)
    {
        this();
        setDomainAppID(domainID, appID);
    }

  
    public String getDomainID()
    {
        return lookupValue(Param.DOMAIN_ID);
    }


    @Deprecated
    public void setDomainID(String domainID)
            throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("Not supported.");
    }


    public String getAppID()
    {
        return lookupValue(Param.APP_ID);
    }

    
    @Deprecated
    public void setAppID(String appID)
            throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    public String getSubjectID()
    {
        synchronized(this)
    	{
    		String ret = toCanonicalID();
			setValue(Param.SUBJECT_ID, ret);
			return ret;
    	}
    }


    public void setSubjectID(String subjectID)
            throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("Not supported.");
    }

    public synchronized void setDomainAppID(String domainID, String appID) {
        setValue(Param.DOMAIN_ID, domainID);
        setValue(Param.APP_ID, appID);
//        setValue(Param.SUBJECT_ID, toCanonicalID());
        getSubjectID();
    }

  
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj != null && obj instanceof AppIDDAO) {
            AppIDDAO appIDDAO = (AppIDDAO) obj;

            if (SharedStringUtil.equals(getDomainID(), appIDDAO.getDomainID(), true)
                    && SharedStringUtil.equals(getAppID(), appIDDAO.getAppID(), true)) {
                return true;
            }
        }
        return false;
    }
    
    
    public String toString()
	{
		return getSubjectID();
	}

    @Override
    public int hashCode() {
        
        if (getDomainID() != null && getAppID() != null) {
            return 31 * getDomainID().hashCode() + getAppID().hashCode() + (getName() != null ? getName().hashCode() : 0);
        }

        if (getDomainID() != null) {
            return getDomainID().hashCode();
        }

        if (getAppID() != null) {
            return getAppID().hashCode();
        }

        return super.hashCode();
    }

	
	public String getAppGUID()
	{
		// TODO Auto-generated method stub
		return getSubjectID();
	}


	@Deprecated
	public void setAppGUID(String appGID)
		throws UnsupportedOperationException
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Not supported");
	}

	public String toCanonicalID()
	{
		return appIDSubjectID(getDomainID(), getAppID());	
	}

	
	
	public static String appIDSubjectID(String domainID, String appIDName)
	{
		return SharedUtil.toCanonicalID(ShiroBase.CAN_ID_SEP, FilterType.DOMAIN.validate(domainID),AppIDNameFilter.SINGLETON.validate(appIDName));
	}
	
	public static AppIDDAO toAppID(String gid)
	{
		gid = SharedStringUtil.trimOrNull(gid);
		SharedUtil.checkIfNulls("Null app global ig", gid);
		int sepIndex = gid.lastIndexOf(ShiroBase.CAN_ID_SEP);
		if (sepIndex < 1 || sepIndex + 1 == gid.length())
			throw new IllegalArgumentException("Illegal gid:"+ gid);
		
		String domainID = FilterType.DOMAIN.validate(gid.substring(0, sepIndex));
		String appID = AppIDNameFilter.SINGLETON.validate(gid.substring(sepIndex + 1, gid.length()));
		
		
		return new AppIDDAO(domainID, appID);
		
	}

    public static AppIDDAO toAppID(String domainID, String appID)
    {
        return new AppIDDAO(domainID, appID);
    }
}