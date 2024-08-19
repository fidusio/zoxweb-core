package org.zoxweb.shared.security.shiro;

public interface AuthorizationInfoLookup<O,I>
{
    O lookupAuthorizationInfo(I pc);
}
