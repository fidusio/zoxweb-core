package org.zoxweb.shared.crypto;

public interface CredentialHasher<T>
        extends CredentialValidator<T> {

    int getRounds();

    T hash(String password);

    T hash(byte[] password);

    T hash(char[] password);

    T fromCanonicalID(String passwordCanID);

}
