package org.zoxweb.shared.security;

import org.zoxweb.shared.api.APIDataStore;
import org.zoxweb.shared.util.*;

public interface SecurityController
{
    void validateCredential(CredentialInfo ci, String input)
            throws AccessSecurityException;
    void validateCredential(CredentialInfo ci, byte[] input)
            throws AccessSecurityException;

    Object encryptValue(APIDataStore<?, ?> dataStore, NVEntity container, NVConfig nvc, NVBase<?> nvb, byte[] msKey)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    Object decryptValue(APIDataStore<?, ?> dataStore, NVEntity container, NVBase<?> nvb, Object value, byte[] msKey)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    String decryptValue(APIDataStore<?, ?> dataStore, NVEntity container, NVPair nvp, byte[] msKey)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    Object decryptValue(String userID, APIDataStore<?,?> dataStore, NVEntity container, Object value, byte[] msKey)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    NVEntity decryptValues(APIDataStore<?, ?> dataStore, NVEntity container, byte[] msKey)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    void associateNVEntityToSubjectGUID(NVEntity nve, String subjectGUID);

    String currentSubjectID()
            throws AccessSecurityException;
    String currentSubjectGUID()
            throws AccessSecurityException;


    boolean isNVEntityAccessible(String nveRefID, String nveUserID, CRUD... permissions);

}
