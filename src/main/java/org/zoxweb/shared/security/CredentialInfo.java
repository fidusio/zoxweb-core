package org.zoxweb.shared.security;

import org.zoxweb.shared.util.GetNVProperties;

public interface CredentialInfo
    extends GetNVProperties
{

    enum Type
    {
        PASSWORD,
        PUBLIC_KEY,
        SYMMETRIC_KEY,
        API_KEY,
        TOKEN
    }

    Type getCredentialType();
}
