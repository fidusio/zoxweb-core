package org.zoxweb.shared.crypto;

import org.zoxweb.shared.util.GetName;

public interface CredentialHasher<T>
    extends GetName
{

    T hash(String password);

    T hash(byte[] password);

    T hash(char[] password);
}
