package org.zoxweb.shared.security.shiro;

import org.zoxweb.shared.security.model.SecurityModel;
import org.zoxweb.shared.util.*;

public class ShiroAuthzInfo
extends ShiroDomain
{
    public enum Param
            implements GetNVConfig
    {



        SUBJECT_TYPE(NVConfigManager.createNVConfig("subject_type", "The class name type of the subject.", "SubjectType", false, true, String.class)),


        AUTHZ_ID(NVConfigManager.createNVConfig("authz_guid", "The authorization global identifier.", "AuthzGID", false, true, String.class)),
        AUTHZ_TYPE(NVConfigManager.createNVConfig("authz_type", "The authorization type permission, role or role group.", "AuthzType", false, true, SecurityModel.AuthzType.class)),


        RESOURCE_GUID(NVConfigManager.createNVConfig(MetaToken.RESOURCE_GUID.getName(), "The resource global identifier.", "ResourceGUID", false, true, String.class)),
        RESOURCE_TYPE(NVConfigManager.createNVConfig(MetaToken.RESOURCE_TYPE.getName(), "The class name of the resource.", "ResourceType", false, true, String.class)),
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

    public static final NVConfigEntity NVC_SHIRO_AUTHZ_INFO = new NVConfigEntityLocal("shiro_authz_info", "Shiro authorization info" , "ShiroAuthzInfo",
            false, true, false, false, ShiroAuthzInfo.class, SharedUtil.extractNVConfigs(Param.values()), null, false, ShiroDomain.NVC_SHIRO_DOMAIN);

    public ShiroAuthzInfo()
    {
        super(NVC_SHIRO_AUTHZ_INFO);
    }



    /**
     * @return the class name of a subject
     */
    public String getSubjectType()
    {
        return lookupValue(Param.SUBJECT_TYPE);
    }

    /**
     * @param subjectType the class name if the subject id
     */
    public void setSubjectType(String subjectType)
    {
        setValue(Param.SUBJECT_TYPE, subjectType);
    }

    /**
     * @return gid of the authorization
     */
    public String getAuthzID()
    {
        return lookupValue(Param.AUTHZ_ID);
    }

    /**
     *
     * @param authzID gid of the authorization
     */
    public void setAuthzGUID(String authzID)
    {
        setValue(Param.AUTHZ_ID, authzID);
    }

    /**
     * @return the authz type enum PERMISSION, ROLE, ROLE_GROUP
     */
    public SecurityModel.AuthzType getAuthzType()
    {
        return lookupValue(Param.AUTHZ_TYPE);
    }


    public void setAuthzType(SecurityModel.AuthzType authzType)
    {
        setValue(Param.AUTHZ_TYPE, authzType);
    }


    /**
     * Get the resource GUID
     * @return resource GUID
     */
    public String getResourceGUID()
    {
        return lookupValue(Param.RESOURCE_GUID);
    }


    public void setResourceGUID(String resourceGUID)
    {
        setValue(Param.RESOURCE_GUID, resourceGUID);
    }

    /**
     * Get the resource type, if null the authz info is just for the subject
     * @return the resource class name null if no set
     */
    public String getResourceType()
    {
        return lookupValue(Param.RESOURCE_TYPE);
    }

    public void setResourceType(String resourceType)
    {
        setValue(Param.RESOURCE_TYPE, resourceType);
    }

    public ShiroAuthzInfo setAuthz(ShiroBase shiroBase)
    {
        SUS.checkIfNulls("Shiro Authz null", shiroBase);
        if (shiroBase instanceof ShiroPermission)
        {
            setAuthzGUID(((ShiroPermission) shiroBase).getGUID());
            setAuthzType(SecurityModel.AuthzType.PERMISSION);
        }
        else if (shiroBase instanceof ShiroRoleGroup)
        {
            setAuthzGUID(((ShiroRoleGroup) shiroBase).getGUID());
            setAuthzType(SecurityModel.AuthzType.ROLE_GROUP);
        }
        else if (shiroBase instanceof ShiroRole)
        {
            setAuthzGUID(((ShiroRole) shiroBase).getGUID());
            setAuthzType(SecurityModel.AuthzType.ROLE);
        }
        else
        {
            throw new IllegalArgumentException("Invalid Authz " + shiroBase);
        }

        return this;
    }

    public ShiroAuthzInfo setResource(NVEntity resource)
    {
        SUS.checkIfNulls("Resource null", resource);
        SUS.checkIfNulls("Resource global id null", resource.getGUID());
        setResourceGUID(resource.getGUID());
        setResourceType(resource.getClass().getName());
        return this;
    }

    public ShiroAuthzInfo setSubject(NVEntity subject)
    {
        SUS.checkIfNulls("Subject null", subject);
        SUS.checkIfNulls("Subject global id null", subject.getGUID());
        setSubjectGUID(subject.getGUID());
        setSubjectType(subject.getClass().getName());
        return this;
    }

    public static ShiroAuthzInfo create(NVEntity subject, ShiroBase shiro, NVEntity resource)
    {
        ShiroAuthzInfo ret = new ShiroAuthzInfo();
        ret.setSubject(subject);
        ret.setResource(resource);
        ret.setAuthz(shiro);
        return ret;
    }
    public boolean equals(Object to)
    {
        if (to instanceof ShiroAuthzInfo)
        {
            return toCanonicalID().equals(((ShiroAuthzInfo) to).toCanonicalID());
        }
        return false;
    }

    public int hashCode()
    {
        return toCanonicalID().hashCode();
    }
    public String toCanonicalID()
    {
        return SharedUtil.toCanonicalID(CAN_ID_SEP, getDomainID(), getAppID(), getSubjectGUID(),
                getAuthzID(), getAuthzType(), getResourceGUID());
    }
}