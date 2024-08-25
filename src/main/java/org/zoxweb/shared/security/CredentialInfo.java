package org.zoxweb.shared.security;

public interface CredentialInfo
{

    enum CredentialType
    {
        PASSWORD,
        PUBLIC_KEY,
        SYMMETRIC_KEY,
        API_KEY,
        TOKEN
    }
}
