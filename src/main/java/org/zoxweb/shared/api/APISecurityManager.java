package org.zoxweb.shared.api;

import org.zoxweb.shared.security.AccessSecurityException;
import org.zoxweb.shared.security.JWTToken;
import org.zoxweb.shared.util.CRUD;
import org.zoxweb.shared.util.Const.LogicalOperator;
import org.zoxweb.shared.util.NVBase;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.NVPair;

public interface APISecurityManager<S, O, I> {

    Object encryptValue(APIDataStore<?, ?> dataStore, NVEntity container, NVConfig nvc, NVBase<?> nvb, byte msKey[])
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    Object decryptValue(APIDataStore<?, ?> dataStore, NVEntity container, NVBase<?> nvb, Object value, byte msKey[])
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    String decryptValue(APIDataStore<?, ?> dataStore, NVEntity container, NVPair nvp, byte msKey[])
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    Object decryptValue(String userID, APIDataStore<?, ?> dataStore, NVEntity container, Object value, byte msKey[])
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    NVEntity decryptValues(APIDataStore<?, ?> dataStore, NVEntity container, byte msKey[])
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    void associateNVEntityToSubjectUserID(NVEntity nve, String userID);

    String currentSubjectID()
            throws AccessSecurityException;

    String currentUserID()
            throws AccessSecurityException;

    String currentDomainID()
            throws AccessSecurityException;

    String currentAppID()
            throws AccessSecurityException;

    String currentJWTSubjectID()
            throws AccessSecurityException;

    S getDaemonSubject();

    void setDaemonSubject(S subject);

    boolean isNVEntityAccessible(NVEntity nve, CRUD... permissions);

    boolean isNVEntityAccessible(LogicalOperator lo, NVEntity nve, CRUD... permissions);

    boolean isNVEntityAccessible(String nveRefID, String nveUserID, CRUD... permissions);


    String checkNVEntityAccess(NVEntity nve, CRUD... permissions)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    String checkNVEntityAccess(LogicalOperator lo, NVEntity nve, CRUD... permissions)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    String checkNVEntityAccess(String nveRefID, String nveUserID, CRUD... permissions)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    void checkSubject(String subjectID)
            throws NullPointerException, AccessSecurityException;

    String checkNVEntityAccess(String nveRefID, CRUD... permissions)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;


    void checkPermissions(String... permissions)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    void checkPermissions(boolean partial, String... permissions)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;


    void checkPermission(NVEntity nve, String permission)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    boolean isPermitted(NVEntity nve, String permission)
            throws NullPointerException, IllegalArgumentException;

    boolean isPermitted(String permission)
            throws NullPointerException, IllegalArgumentException;

    /**
     * Return true of subject has all the permissions
     *
     * @param permission
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     */
    boolean hasPermission(String permission)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    /**
     * Check if the current subject has all the roles
     *
     * @param roles
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     */
    void checkRoles(String... roles)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    /**
     * Check if the current subject has the roles, if partial is true one of the is sufficient
     *
     * @param partial
     * @param roles
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     */
    void checkRoles(boolean partial, String... roles)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    /**
     * Check if the user has the role
     *
     * @param role
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     */
    boolean hasRole(String role)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;


    /**
     * Login a user based on user name and password
     *
     * @param subjectID
     * @param credentials
     * @param domainID
     * @param appID
     * @param autoLogin
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     */
    S login(String subjectID, String credentials, String domainID, String appID, boolean autoLogin)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    /**
     * Login a subject based on jwtToken
     *
     * @param jwtToken
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     */
    S login(JWTToken jwtToken)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    /**
     * Logout the current subject
     */
    void logout();


    /**
     * Invalidate a resource
     *
     * @param resourceID
     */
    void invalidateResource(String resourceID);


}
