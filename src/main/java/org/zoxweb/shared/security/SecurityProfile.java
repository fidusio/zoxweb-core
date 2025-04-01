package org.zoxweb.shared.security;


import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.crypto.CryptoConst.AuthenticationType;
import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.http.URIScheme;
import org.zoxweb.shared.util.*;

import java.util.List;
import java.util.Set;


public class SecurityProfile
extends PropertyDAO
implements ResourceSecurity
{


    public enum Param
            implements GetNVConfig
    {
        AUTHENTICATIONS(NVConfigManager.createNVConfig("authentications", "Authentication types", "Authentications", false, true, CryptoConst.AuthenticationType[].class)),
        PERMISSIONS(NVConfigManager.createNVConfig("permissions", "Permission tokens", "Permissions", false, true, NVStringSet.class)),
        ROLES(NVConfigManager.createNVConfig("roles", "Role tokens", "Roles", false, true, NVStringSet.class)),
        RESTRICTIONS(NVConfigManager.createNVConfig("restrictions", "Restrictions", "Restrictions", false, true, NVStringSet.class)),
        PROTOCOLS(NVConfigManager.createNVConfig("protocols", "Http, Https...", "Protocols", false, true, URIScheme[].class)),


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

    public Set<String> getPermissions()
    {
        return ((NVStringSet)lookup(Param.PERMISSIONS)).getValue();
    }

    public void setPermissions(String ...permissions)
    {
        this.permissions = null;
        ((NVStringSet)lookup(Param.PERMISSIONS)).setValues(permissions);
    }

    public Set<String> getRoles()
    {
        return ((NVStringSet)lookup(Param.ROLES)).getValue();
    }

    public void setRoles(String ...roles)
    {
        this.roles = null;
        ((NVStringSet)lookup(Param.ROLES)).setValues(roles);

    }

    public Set<String> getRestrictions()
    {
        return ((NVStringSet)lookup(Param.RESTRICTIONS)).getValue();
    }

    public void setRestrictions(String ...restrictions)
    {
        this.restrictions = null;
        ((NVStringSet)lookup(Param.RESTRICTIONS)).setValues(restrictions);

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
                    permissions = getPermissions().toArray(new String[0]);
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
                    roles = getRoles().toArray(new String[0]);
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
                    restrictions = getRestrictions().toArray(new String[0]);
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

    public URIScheme[] getProtocols()
    {
        return ((NVEnumList)lookup(Param.PROTOCOLS)).getValues(new URIScheme[0]);
    }

    public boolean isProtocolSupported(String protocol)
    {
        return isProtocolSupported((URIScheme)SharedUtil.lookupEnum(protocol, URIScheme.values()));
    }
    public boolean isProtocolSupported(URIScheme protocol)
    {
        NVEnumList protocolList = (NVEnumList)lookup(Param.PROTOCOLS);
        return protocolList.getValue().size() > 0 ? protocolList.contains(protocol) : true;
    }

    public void setProtocols(URIScheme ...protocols)
    {
        ((NVEnumList)lookup(Param.PROTOCOLS)).setValues(protocols);
    }
}
