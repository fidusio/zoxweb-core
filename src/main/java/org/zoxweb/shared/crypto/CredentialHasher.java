package org.zoxweb.shared.crypto;

import org.zoxweb.shared.util.GetName;

public interface CredentialHasher<T>
    extends GetName
{

    String[] supportedAlgorithms();
    boolean isAlgorithmSupported(String algoName);
    int getRounds();

    T hash(String password);

    T hash(byte[] password);

    T hash(char[] password);

    T fromCanonicalID(String passwordCanID);
    boolean isPasswordValid(T ci, String password);
    boolean isPasswordValid(T ci, byte[] password);
    boolean isPasswordValid(T ci, char[] password);

}
