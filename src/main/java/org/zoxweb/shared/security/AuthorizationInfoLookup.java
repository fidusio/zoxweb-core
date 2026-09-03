package org.zoxweb.shared.security;

public interface AuthorizationInfoLookup<O, I> {
    O lookupAuthorizationInfo(I pc);
}
