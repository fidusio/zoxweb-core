package org.zoxweb.shared.security;

import org.zoxweb.shared.util.GetGlobalID;

import java.util.Set;

public interface ResourceSecurity
    extends GetGlobalID<String>
{
    String[] permissions();
    String[] roles();
    String[] restrictions();
    Set<String> getPermissions();
    Set<String> getRoles();

    SecConst.AuthenticationType[] authenticationTypes();
}
