package org.zoxweb.shared.security;

import org.zoxweb.shared.util.AppID;

public interface SubjectCredential
    extends AppID<String>
{
    CredentialInfo getCredential();
}
