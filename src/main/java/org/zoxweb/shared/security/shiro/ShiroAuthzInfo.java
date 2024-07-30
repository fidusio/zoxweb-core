package org.zoxweb.shared.security.shiro;

import org.zoxweb.shared.security.model.SecurityModel;
import org.zoxweb.shared.util.*;

public class ShiroAuthzInfo
extends ShiroDomainDAO
{
    public enum Param
            implements GetNVConfig
    {



        // The subject that
        SUBJECT_ID(NVConfigManager.createNVConfig("subject_id", "The subject global identifier.", "SubjectGID", false, true, String.class)),
        SUBJECT_TYPE(NVConfigManager.createNVConfig("subject_type", "The subject type.", "SubjectType", false, true, String.class)),


        AUTHZ_ID(NVConfigManager.createNVConfig("authz_id", "The authorization global identifier.", "AuthzGID", false, true, String.class)),
        AUTHZ_TYPE(NVConfigManager.createNVConfig("authz_type", "The authorization type permission, role or role group.", "AuthzType", false, true, SecurityModel.AuthzType.class)),


        RESOURCE_ID(NVConfigManager.createNVConfig("resource_id", "The resource global identifier.", "ResourceGID", false, true, String.class)),
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
            false, true, false, false, ShiroAuthzInfo.class, SharedUtil.extractNVConfigs(Param.values()), null, false, ShiroDomainDAO.NVC_APP_ID_DAO);

    public ShiroAuthzInfo()
    {
        super(NVC_SHIRO_AUTHZ_INFO);
    }

    /**
     * @return GID of a subject or resource
     */
    public String getSubjectID()
    {
        return lookupValue(Param.SUBJECT_ID);
    }

    /**
     *
     * @param subjectID the GID of subject or resource
     */
    public void setSubjectID(String subjectID)
    {
        setValue(Param.SUBJECT_ID, subjectID);
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
    public void setAuthzID(String authzID)
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


    public String getResourceID()
    {
        return lookupValue(Param.RESOURCE_ID);
    }


    public void setResourceID(String resourceID)
    {
        setValue(Param.RESOURCE_ID, resourceID);
    }

    public String getResourceType()
    {
        return lookupValue(Param.RESOURCE_TYPE);
    }

    public void setResourceType(String resourceType)
    {
        setValue(Param.RESOURCE_TYPE, resourceType);
    }
}