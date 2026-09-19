package org.zoxweb.shared.api;

import org.zoxweb.shared.data.AppConfigDAO;
import org.zoxweb.shared.data.AppDeviceDAO;
import org.zoxweb.shared.app.AppIDDefault;
import org.zoxweb.shared.data.UserIDDAO;
import org.zoxweb.shared.data.UserInfoDAO;
import org.zoxweb.shared.security.*;
import org.zoxweb.shared.db.QueryMarker;
import org.zoxweb.shared.util.CRUD;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.Const.Status;

import java.util.List;

public interface APIAppManager {

    /**
     * Retruns the APIDataStore.
     *
     * @return
     */
    APIDataStore<?, ?> getAPIDataStore();

    /**
     * Set the APIDataStore.
     *
     * @param dataStore
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    void setAPIDataStore(APIDataStore<?, ?> dataStore)
            throws NullPointerException, IllegalArgumentException;

    /**
     * Get the API security manager
     *
     * @return
     */
    APISecurityManager<?, ?, ?> getAPISecurityManager();


    /**
     * Set the api security manager
     *
     * @param apiSecurityManager
     */
    void setAPISecurityManager(APISecurityManager<?, ?, ?> apiSecurityManager);

    /**
     * Get the domain security manager used for permissions, roles and grants
     *
     * @return the domain security manager, null if not set
     */
    DomainSecurityManager getDomainSecurityManager();

    /**
     * Set the domain security manager used for permissions, roles and grants
     *
     * @param domainSecurityManager to be used
     */
    void setDomainSecurityManager(DomainSecurityManager domainSecurityManager);


    /**
     * Register user.
     *
     * @param userInfoDAO
     * @param appDeviceDAO
     * @param username
     * @param password
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    SubjectAPIKey registerSubjectAPIKey(UserInfoDAO userInfoDAO, AppDeviceDAO appDeviceDAO, String username, String password)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    UserInfoDAO registerSubject(String username, String password)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    /**
     * Create a userDAO, the creation requires persistence of the following:
     * <ol>
     * <li> UserIDDAO the user id
     * <li> UserInfoDAO the user data addresses, cc etc
     * <li> UserIDCredentialsDAO the password info
     * <li> UserPreferenceDAO his/her preferences
     * </ol>
     *
     * @param userIDDAO
     * @param password
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    UserIDDAO createUserIDDAO(UserIDDAO userIDDAO, SecConst.SecStatus userIDstatus, String password)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    /**
     * Create UserIDDAO.
     *
     * @param subjectID
     * @param password
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    UserIDDAO createUserIDDAO(String subjectID, SecConst.SecStatus userIDstatus, String password)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    /**
     * Delete a user based on his subjectID like email, this method can only be called by a super admin
     * @param subjectID
     */


    /**
     * Delete a user based on his subjectID like email, this method can only be called by a super admin
     *
     * @param subjectID
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    void deleteUser(String subjectID)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;


    /**
     * Look up UserIDDAO based on subject ID.
     *
     * @param subjectID
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException          - only Super Admin can do lookup
     * @throws APIException
     */
    UserIDDAO lookupUserIDDAO(String subjectID, String... params)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;


    /**
     * Look up UserPreferenceDAO based on subject ID.
     *
     * @param subjectID
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    SubjectPreference lookupUserPreferenceDAO(AppIDDefault appIDDAO, String subjectID)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;


    SubjectPreference lookupUserPreferenceDAO(AppIDDefault appIDDAO, UserIDDAO userIDDAO)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    /**
     * Create AppDeviceDAO.
     *
     * @param subjectAPIKey
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    SubjectAPIKey createAppDeviceDAO(AppDeviceDAO subjectAPIKey)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    /**
     * Create SubjectAPIKey.
     *
     * @param subjectAPIKey
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    SubjectAPIKey createSubjectAPIKey(SubjectAPIKey subjectAPIKey)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    /**
     * Create SubjectAPIKey.
     *
     * @param subjectAPIKey
     * @param status        ACTIVE
     * @param ttl           time to live 0 forever
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    public SubjectAPIKey createSubjectAPIKey(SubjectAPIKey subjectAPIKey, Status status, long ttl)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;


//    /**
//     * Delete SubjectAPIKey.
//     * @param subjectID
//     * @throws NullPointerException
//     * @throws IllegalArgumentException
//     * @throws AccessSecurityException
//     * @throws APIException
//     */
//	void deleteSubjectAPIKey(String subjectID)
//            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    /**
     * Delete SubjectAPIKey.
     *
     * @param subjectAPIKey
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    void deleteSubjectAPIKey(SubjectAPIKey subjectAPIKey)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

//    /**
//     * Look up SubjectAPIKey.
//     * @param subjectID
//     * @param throwExceptionIfNotFound if true and the subject api key not found an API exception is throw otherwise null is returned
//     * @return
//     * @throws NullPointerException
//     * @throws IllegalArgumentException
//     * @throws AccessSecurityException
//     * @throws APIException
//     */
//	<V extends SubjectAPIKey> V lookupSubjectAPIKey(String subjectID, boolean throwExceptionIfNotFound)
//            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;


    /**
     * Update SubjectAPIKey.
     * @param subjectAPIKey
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
//	void updateSubjectAPIKey(SubjectAPIKey subjectAPIKey)
//            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;
//
//	SubjectAPIKey renewSubjectAPIKEy(String subjectID)
//			 throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;
//
//	SubjectAPIKey renewSubjectAPIKEy(SubjectAPIKey sak)
//			 throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;


    /**
     * Look up AppIDDAO based on domain ID and app ID.
     *
     * @param domainID
     * @param appID
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    AppIDDefault lookupAppIDDAO(String domainID, String appID)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    /**
     * Look up AppIDDAO based on domain ID and app ID.
     *
     * @param domainID
     * @param appID
     * @param exceptionIfNotFound
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    AppIDDefault lookupAppIDDAO(String domainID, String appID, boolean exceptionIfNotFound)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;


    /**
     * Look up AppConfigDAO based on domain ID and app ID.
     *
     * @param domainID
     * @param appID
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    AppConfigDAO lookupAppConfigDAO(String domainID, String appID)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    /**
     * Validate JWT token.
     *
     * @param token
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    JWT validateJWT(String token)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    /**
     * Reset password.
     *
     * @param subjectID
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    void resetPassword(String subjectID)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;


    /**
     * Change password.
     *
     * @param oldPassword
     * @param newPassword
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    void changePassword(String oldPassword, String newPassword)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    /**
     * Create NVEntity object.
     *
     * @param nve
     * @param <V>
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    <V extends NVEntity> V create(V nve)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    /**
     * Looks up NVEntity objects based on given given subject ID and NVEntity class type.
     *
     * @param subjectID
     * @param classType
     * @param <V>
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    <V extends NVEntity> List<V> lookup(String subjectID, Class<V> classType)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    /**
     * Update NVEntity object.
     *
     * @param nve
     * @param <V>
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    <V extends NVEntity> V update(V nve)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    /**
     * Delete NVEntity object.
     *
     * @param nve
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    <V extends NVEntity> boolean delete(V nve)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    /**
     *
     * @param nve
     * @param withReference any referenced object
     * @param <V>
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessSecurityException
     * @throws APIException
     */
    <V extends NVEntity> boolean delete(V nve, boolean withReference)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;


    AppIDDefault createAppIDDAO(String domainID, String appID);

    AppIDDefault deleteAppIDDAO(String domainID, String appID);


    <V extends NVEntity> List<V> search(NVConfigEntity nvce, QueryMarker... queryCriteria)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;

    <V extends NVEntity> List<V> search(NVConfigEntity nvce, List<String> fieldNames, QueryMarker... queryCriteria)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException, APIException;


    void updateSubjectRole(String subjectID, AppIDDefault appID, String roleName, CRUD crud)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;

    void updateSubjectPermission(String subjectID, AppIDDefault appID, String permssionName, CRUD crud)
            throws NullPointerException, IllegalArgumentException, AccessSecurityException;
}