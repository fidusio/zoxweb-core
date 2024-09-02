package org.zoxweb.shared.security;

import org.zoxweb.shared.api.APIDataStore;
import org.zoxweb.shared.util.*;

public interface SecurityController
{
    void validateCredential(CredentialInfo ci, String input)
            throws AccessSecurityException;
    void validateCredential(CredentialInfo ci, byte[] input)
            throws AccessSecurityException;

    Object encryptValue(APIDataStore<?> dataStore, NVEntity container, NVConfig nvc, NVBase<?> nvb, byte[] msKey)
            throws NullPointerException, IllegalArgumentException, AccessException;

    Object decryptValue(APIDataStore<?> dataStore, NVEntity container, NVBase<?> nvb, Object value, byte[] msKey)
            throws NullPointerException, IllegalArgumentException, AccessException;

    String decryptValue(APIDataStore<?> dataStore, NVEntity container, NVPair nvp, byte[] msKey)
            throws NullPointerException, IllegalArgumentException, AccessException;

    Object decryptValue(String userID, APIDataStore<?> dataStore, NVEntity container, Object value, byte[] msKey)
            throws NullPointerException, IllegalArgumentException, AccessException;

    NVEntity decryptValues(APIDataStore<?> dataStore, NVEntity container, byte[] msKey)
            throws NullPointerException, IllegalArgumentException, AccessException;

    void associateNVEntityToSubjectGUID(NVEntity nve, String subjectGUID);

    String currentSubjectID()
            throws AccessException;
    String currentSubjectGUID()
            throws AccessException;


    boolean isNVEntityAccessible(String nveRefID, String nveUserID, CRUD... permissions);

}
