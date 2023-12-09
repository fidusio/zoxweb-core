package org.zoxweb.shared.security;

import org.zoxweb.shared.crypto.CryptoConst;

public interface ResourceSecurity
{
    String[] permissions();
    String[] roles();
    String[] restrictions();

    CryptoConst.AuthenticationType[] authenticationTypes();
}
