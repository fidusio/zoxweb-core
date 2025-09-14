package org.zoxweb.shared.crypto;

import org.zoxweb.shared.security.AccessSecurityException;
import org.zoxweb.shared.util.GetName;

public interface CredentialValidator<T>
        extends GetName {

    String[] supportedAlgorithms();

    boolean isAlgorithmSupported(String algoName);

    boolean validate(T ci, String password);

    boolean validate(T ci, byte[] password);

    boolean validate(T ci, char[] password);

    T update(T oldCI, String password, String newPassword) throws AccessSecurityException;

    T update(T oldCI, byte[] password, byte[] newPassword) throws AccessSecurityException;

    T update(T oldCI, char[] password, char[] newPassword) throws AccessSecurityException;

}
