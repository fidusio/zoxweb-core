package org.zoxweb.shared.security;

import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.util.GlobalIDBase;

public interface ResourceSecurity
    extends GlobalIDBase<String>
{
    String[] permissions();
    String[] roles();
    String[] restrictions();

    CryptoConst.AuthenticationType[] authenticationTypes();
}
