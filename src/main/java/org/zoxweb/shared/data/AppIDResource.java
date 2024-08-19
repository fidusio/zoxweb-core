package org.zoxweb.shared.data;

import org.zoxweb.shared.filters.AppIDNameFilter;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.security.shiro.ShiroBase;
import org.zoxweb.shared.util.*;

public class AppIDResource
    extends PropertyDAO
    implements AppID<String>,
        CanonicalID
{
    public enum Param
            implements GetNVConfig
    {
        APP_ID(NVConfigManager.createNVConfig("app_id", "App ID","AppID", true, false, false, String.class, AppIDNameFilter.SINGLETON)),
        DOMAIN_ID(NVConfigManager.createNVConfig("domain_id", "Domain ID", "Domain ID", true, true, false, String.class, FilterType.DOMAIN)),
        //SUBJECT_ID(NVConfigManager.createNVConfig("subject_id", "Subject ID", "Subject ID", true, false, true, String.class, null)),
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



    public static final NVConfigEntity NVC_APP_ID_RESOURCE = new NVConfigEntityLocal(
            "app_id_resource",
            "AppIDResource" ,
            AppIDResource.class.getSimpleName(),
            true, false,
            false, false,
            AppIDResource.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            PropertyDAO.NVC_PROPERTY_DAO
    );


    public AppIDResource()
    {
        super(NVC_APP_ID_RESOURCE);
    }

    public AppIDResource(String domainID, String appID)
    {
        super(NVC_APP_ID_RESOURCE);
        setDomainAppID(domainID, appID);
    }



    protected AppIDResource(NVConfigEntity nvce)
    {
        super(nvce);
    }


    public String getDomainID()
    {
        return lookupValue(Param.DOMAIN_ID);
    }


    public void setDomainID(String domainID)
    {
        setValue(Param.DOMAIN_ID, FilterType.DOMAIN.validate(domainID));
    }


    public String getAppID()
    {
        return lookupValue(Param.APP_ID);
    }



    public void setAppID(String appID)
    {
        setValue(Param.APP_ID, AppIDNameFilter.SINGLETON.validate(appID));
    }



    public synchronized void setDomainAppID(String domainID, String appID)
    {
        setDomainID(domainID);
        setAppID(appID);
    }

    public String toCanonicalID()
    {
        return appIDSubjectID(getDomainID(), getAppID());
    }



    public static String appIDSubjectID(String domainID, String appIDName)
    {
        return SharedUtil.toCanonicalID(ShiroBase.CAN_ID_SEP, FilterType.DOMAIN.validate(domainID),AppIDNameFilter.SINGLETON.validate(appIDName));
    }

    public boolean equals(Object obj) {
        if (obj == this)
        {
            return true;
        }

        if (obj instanceof AppIDResource)
        {
            AppIDResource appIDResource = (AppIDResource) obj;

            if (SharedStringUtil.equals(getDomainID(), appIDResource.getDomainID(), true)
                    && SharedStringUtil.equals(getAppID(), appIDResource.getAppID(), true))
                return true;

        }
        return false;
    }

    @Override
    public int hashCode()
    {


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


    public static AppIDResource toAppID(String gid)
    {
        gid = SharedStringUtil.trimOrNull(gid);
        SharedUtil.checkIfNulls("Null app global ig", gid);
        int sepIndex = gid.lastIndexOf(ShiroBase.CAN_ID_SEP);
        if (sepIndex < 1 || sepIndex + 1 == gid.length())
            throw new IllegalArgumentException("Illegal gid:"+ gid);

        String domainID = FilterType.DOMAIN.validate(gid.substring(0, sepIndex));
        String appID = AppIDNameFilter.SINGLETON.validate(gid.substring(sepIndex + 1));


        return new AppIDResource(domainID, appID);

    }

    public static AppIDResource toAppID(String domainID, String appID)
    {
        return new AppIDResource(domainID, appID);
    }
}
