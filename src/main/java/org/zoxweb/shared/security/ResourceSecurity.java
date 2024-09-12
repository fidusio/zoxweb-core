package org.zoxweb.shared.security;

import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.util.GlobalIDBase;

import java.util.Set;

public interface ResourceSecurity
    extends GlobalIDBase<String>
{
    String[] permissions();
    String[] roles();
    String[] restrictions();
    Set<String> getPermissions();
    Set<String> getRoles();

    CryptoConst.AuthenticationType[] authenticationTypes();
}
