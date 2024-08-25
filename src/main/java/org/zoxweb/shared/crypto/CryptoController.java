package org.zoxweb.shared.crypto;

import org.zoxweb.shared.security.AccessSecurityException;
import org.zoxweb.shared.security.CredentialInfo;

public interface CryptoController
{
    void validateCredential(CredentialInfo ci, String input)
            throws AccessSecurityException;
    void validateCredential(CredentialInfo ci, byte[] input)
            throws AccessSecurityException;
}
