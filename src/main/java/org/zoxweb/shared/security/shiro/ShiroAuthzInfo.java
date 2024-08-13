package org.zoxweb.shared.security.shiro;

import org.zoxweb.shared.security.model.SecurityModel;
import org.zoxweb.shared.util.*;

public class ShiroAuthzInfo
extends ShiroDomain
{
    public enum Param
            implements GetNVConfig
    {



        // The subject that
        SUBJECT_GUID(NVConfigManager.createNVConfig("subject_guid", "The subject global identifier.", "SubjectGID", false, true, String.class)),
        SUBJECT_TYPE(NVConfigManager.createNVConfig("subject_type", "The subject type.", "SubjectType", false, true, String.class)),


        AUTHZ_ID(NVConfigManager.createNVConfig("authz_guid", "The authorization global identifier.", "AuthzGID", false, true, String.class)),
        AUTHZ_TYPE(NVConfigManager.createNVConfig("authz_type", "The authorization type permission, role or role group.", "AuthzType", false, true, SecurityModel.AuthzType.class)),


        RESOURCE_ID(NVConfigManager.createNVConfig("resource_guid", "The resource global identifier.", "ResourceGID", false, true, String.class)),
        RESOURCE_TYPE(NVConfigManager.createNVConfig("resource_type", "The resource type.", "ResourceType", false, true, String.class)),
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
            false, true, false, false, ShiroAuthzInfo.class, SharedUtil.extractNVConfigs(Param.values()), null, false, ShiroDomain.NVC_APP_ID_DAO);

    public ShiroAuthzInfo()
    {
        super(NVC_SHIRO_AUTHZ_INFO);
    }

    /**
     * @return GID of a subject or resource
     */
    public String getSubjectGUID()
    {
        return lookupValue(Param.SUBJECT_GUID);
    }

    /**
     *
     * @param subjectID the GID of subject or resource
     */
    private void setSubjectGUID(String subjectID)
    {
        setValue(Param.SUBJECT_GUID, subjectID);
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
    private void setSubjectType(String subjectType)
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
    private void setAuthzGUID(String authzID)
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


    private void setAuthzType(SecurityModel.AuthzType authzType)
    {
        setValue(Param.AUTHZ_TYPE, authzType);
    }


    /**
     * Get the resource global id
     * @return
     */
    public String getResourceGUID()
    {
        return lookupValue(Param.RESOURCE_ID);
    }


    private void setResourceID(String resourceID)
    {
        setValue(Param.RESOURCE_ID, resourceID);
    }

    /**
     * Get the resource type, if null the authz info is just for the subject
     * @return the resource class name null if no set
     */
    public String getResourceType()
    {
        return lookupValue(Param.RESOURCE_TYPE);
    }

    private void setResourceType(String resourceType)
    {
        setValue(Param.RESOURCE_TYPE, resourceType);
    }

    public ShiroAuthzInfo setAuthz(ShiroBase shiroBase)
    {
        SharedUtil.checkIfNulls("Shiro Authz null", shiroBase);
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
        SharedUtil.checkIfNulls("Resource null", resource);
        SharedUtil.checkIfNulls("Resource global id null", resource.getGUID());
        setResourceID(resource.getGUID());
        setResourceType(resource.getClass().getName());
        return this;
    }

    public ShiroAuthzInfo setSubject(NVEntity subject)
    {
        SharedUtil.checkIfNulls("Subject null", subject);
        SharedUtil.checkIfNulls("Subject global id null", subject.getGUID());
        setSubjectGUID(subject.getGUID());
        setSubjectType(subject.getClass().getName());
        return this;
    }
}