package org.zoxweb.shared.crypto;

public interface CredentialHasher<T>
{

    T hash(String password);

    T hash(byte[] password);

    T hash(char[] password);
}
