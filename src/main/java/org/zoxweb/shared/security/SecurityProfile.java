package org.zoxweb.shared.security;


import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.*;

import java.util.Arrays;
import java.util.List;


public class SecurityProfile
extends PropertyDAO
{
    public enum Param
            implements GetNVConfig
    {
        AUTHENTICATIONS(NVConfigManager.createNVConfig("authentications", "Authentication types", "Authentications", false, true, AuthenticationType[].class)),
        PERMISSIONS(NVConfigManager.createNVConfig("permissions", "Permission tokens", "Permissions", false, true, NVStringList.class)),
        ROLES(NVConfigManager.createNVConfig("roles", "Role tokens", "Roles", false, true, NVStringList.class)),
        RESTRICTIONS(NVConfigManager.createNVConfig("restrictions", "Restrictions", "Restrictions", false, true, NVStringList.class)),

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



    /**
     * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on DataContentDAO.
     */
    public static final NVConfigEntity NVC_SECURITY_PROFILE = new NVConfigEntityLocal("security_profile",
            null,
            "SecurityProp",
            true,
            false,
            false,
            false,
            SecurityProfile.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            PropertyDAO.NVC_PROPERTY_DAO);


    public SecurityProfile()
    {
        super(NVC_SECURITY_PROFILE);
    }

    protected SecurityProfile(NVConfigEntity nvce)
    {
        super(nvce);
    }

    public String[] getPermissions()
    {
        return ((NVStringList)lookup(Param.PERMISSIONS)).getValues();
    }

    public void setPermissions(String ...permissions)
    {
        ((NVStringList)lookup(Param.PERMISSIONS)).setValues(permissions);
    }

    public String[] getRoles()
    {
        return ((NVStringList)lookup(Param.ROLES)).getValues();
    }

    public void setRoles(String ...roles)
    {
        ((NVStringList)lookup(Param.ROLES)).setValues(roles);
    }

    public String[] getRestrictions()
    {
        return ((NVStringList)lookup(Param.RESTRICTIONS)).getValues();
    }

    public void setRestrictions(String ...restrictions)
    {
        ((NVStringList)lookup(Param.RESTRICTIONS)).setValues(restrictions);
    }

    public AuthenticationType[] getAuthenticationTypes()
    {
        return ((List<Enum>)lookupValue(Param.AUTHENTICATIONS)).toArray(new AuthenticationType[0]);
    }

    public void setAuthenticationTypes(AuthenticationType ...authTypes)
    {
        NVEnumList el = (NVEnumList) lookup(Param.AUTHENTICATIONS);
        el.setValues(authTypes);
    }
}
