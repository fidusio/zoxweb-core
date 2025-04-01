package org.zoxweb.server.security;

import org.zoxweb.server.util.ReflectionUtil;
import org.zoxweb.shared.annotation.SecurityProp;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.security.ResourceSecurity;
import org.zoxweb.shared.security.SecurityProfile;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public class SecUtil
{
    public static final SecUtil SINGLETON = new SecUtil();
    private final Map<Method, ResourceSecurity> methodResourceSecurityMap = new LinkedHashMap<>();

    private SecUtil(){}

    /**
     * This method read the assigned SecurityProp of method and if it exists it will apply it to the security profile
     * @param method to inspect can't be null
     * @param securityProfile if null SecurityProfile will be created
     * @return ResourceSecurity if applicable or null
     */
    public synchronized ResourceSecurity applyAndCacheSecurityProfile(Method method, SecurityProfile securityProfile)
    {
        SUS.checkIfNulls("Method null", method);
        SecurityProp sp = ReflectionUtil.getAnnotationFromMethod(method, SecurityProp.class);
        if(sp != null)
        {
            ResourceSecurity ret = applySecurityProp(securityProfile != null ? securityProfile : new SecurityProfile(), sp);
            methodResourceSecurityMap.put(method, ret);
            return ret;
        }
        return null;
    }

    /**
     * Apply the SecurityProp to security profile and return it as ResourceSecurity
     * @param securityProfile to be applied to
     * @param securityProp to be applied
     * @return ResourceSecurity or null
     */
    public ResourceSecurity applySecurityProp(SecurityProfile securityProfile, SecurityProp securityProp)
    {
        if (securityProfile != null && securityProp != null)
        {
            String[] roles = SUS.isEmpty(securityProp.roles()) ? null : SharedStringUtil.parseString(securityProp.roles(), ",", " ", "\t");
            String[] permissions = SUS.isEmpty(securityProp.permissions()) ? null : SharedStringUtil.parseString(securityProp.permissions(), ",", " ", "\t");
            CryptoConst.AuthenticationType[] authTypes = securityProp.authentications();
            String[] restrictions = securityProp.restrictions().length > 0 ? securityProp.restrictions() : null;
            securityProfile.setPermissions(permissions);
            securityProfile.setRoles(roles);
            securityProfile.setAuthenticationTypes(authTypes);
            securityProfile.setRestrictions(restrictions);
            securityProfile.setProtocols(securityProp.protocols());
            return securityProfile;
        }
        return null;
    }

    /**
     * Lookup cached ResourceSecurity associated with method
     * @param method to look for
     * @return associated ResourceSecurity if it exists
     */
    public ResourceSecurity lookupCachedResourceSecurity(Method method)
    {
        return methodResourceSecurityMap.get(method);
    }

    /**
     * Remove cached resource security from a method
     * @param method that has been cached
     * @return ResourceSecurity if it existed
     */
    public synchronized ResourceSecurity removeCachedResourceSecurity(Method method)
    {
        return methodResourceSecurityMap.remove(method);
    }

    /**
     * @return all cached methods
     */
    public synchronized Method[] getAllCachedMethods()
    {
        return methodResourceSecurityMap.keySet().toArray(new Method[0]);
    }
}
