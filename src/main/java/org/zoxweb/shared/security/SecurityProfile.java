package org.zoxweb.shared.security;


import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.crypto.CryptoConst.AuthenticationType;

import java.util.List;


public class SecurityProfile
extends PropertyDAO
implements ResourceSecurity
{


    public enum Param
            implements GetNVConfig
    {
        AUTHENTICATIONS(NVConfigManager.createNVConfig("authentications", "Authentication types", "Authentications", false, true, CryptoConst.AuthenticationType[].class)),
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

    private String[] permissions = null;
    private String[] roles = null;
    private String[] restrictions  = null;
    AuthenticationType[] authenticationTypes = null;

    /**
     * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on DataContentDAO.
     */
    public static final NVConfigEntity NVC_SECURITY_PROFILE = new NVConfigEntityLocal("security_profile",
            null,
            "SecurityProfile",
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
        this.permissions = null;
        ((NVStringList)lookup(Param.PERMISSIONS)).setValues(permissions);
    }

    public String[] getRoles()
    {
        return ((NVStringList)lookup(Param.ROLES)).getValues();
    }

    public void setRoles(String ...roles)
    {
        this.roles = null;
        ((NVStringList)lookup(Param.ROLES)).setValues(roles);

    }

    public String[] getRestrictions()
    {
        return ((NVStringList)lookup(Param.RESTRICTIONS)).getValues();
    }

    public void setRestrictions(String ...restrictions)
    {
        this.restrictions = null;
        ((NVStringList)lookup(Param.RESTRICTIONS)).setValues(restrictions);

    }

    public AuthenticationType[] getAuthenticationTypes()
    {
        return ((List<Enum>)lookupValue(Param.AUTHENTICATIONS)).toArray(new AuthenticationType[0]);
    }

    public void setAuthenticationTypes(CryptoConst.AuthenticationType...authTypes)
    {
        this.authenticationTypes = null;
        NVEnumList el = (NVEnumList) lookup(Param.AUTHENTICATIONS);
        el.setValues(authTypes);
    }

    @Override
    public String[] permissions()
    {
        if (permissions == null)
        {
            synchronized (this)
            {
                if(permissions == null)
                    permissions = getPermissions();
            }
        }
        return permissions;
    }

    @Override
    public String[] roles() {

        if (roles == null)
        {
            synchronized (this)
            {
                if(roles == null)
                    roles = getRoles();
            }
        }

        return roles;

    }

    @Override
    public String[] restrictions() {
        if (restrictions == null)
        {
            synchronized (this)
            {
                if(restrictions == null)
                    restrictions = getRestrictions();
            }
        }

        return restrictions;
    }

    @Override
    public AuthenticationType[] authenticationTypes() {
        if (authenticationTypes == null)
        {
            synchronized (this)
            {
                if(authenticationTypes == null)
                {
                    authenticationTypes = getAuthenticationTypes();
                }
            }
        }
        return authenticationTypes;
    }
}
