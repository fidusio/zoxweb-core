package org.zoxweb.shared.security;

import org.zoxweb.shared.util.GetNVProperties;

public interface CredentialInfo
    extends GetNVProperties
{

    enum CredentialType
    {
        PASSWORD,
        PUBLIC_KEY,
        SYMMETRIC_KEY,
        API_KEY,
        TOKEN
    }

    CredentialType getCredentialType();
}
