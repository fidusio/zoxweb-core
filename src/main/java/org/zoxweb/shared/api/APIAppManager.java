package org.zoxweb.shared.api;

import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.data.AppConfigDAO;
import org.zoxweb.shared.data.AppDeviceDAO;
import org.zoxweb.shared.data.AppIDDAO;
import org.zoxweb.shared.data.UserIDDAO;
import org.zoxweb.shared.data.UserInfoDAO;
import org.zoxweb.shared.security.SubjectPreference;
import org.zoxweb.shared.db.QueryMarker;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.security.JWT;
import org.zoxweb.shared.security.SubjectAPIKey;
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
     * Register user.
     *
     * @param userInfoDAO
     * @param appDeviceDAO
     * @param username
     * @param password
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    SubjectAPIKey registerSubjectAPIKey(UserInfoDAO userInfoDAO, AppDeviceDAO appDeviceDAO, String username, String password)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    UserInfoDAO registerSubject(String username, String password)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

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
     * @throws AccessException
     * @throws APIException
     */
    UserIDDAO createUserIDDAO(UserIDDAO userIDDAO, CryptoConst.SubjectStatus userIDstatus, String password)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Create UserIDDAO.
     *
     * @param subjectID
     * @param password
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    UserIDDAO createUserIDDAO(String subjectID, CryptoConst.SubjectStatus userIDstatus, String password)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

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
     * @throws AccessException
     * @throws APIException
     */
    void deleteUser(String subjectID)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;


    /**
     * Look up UserIDDAO based on subject ID.
     *
     * @param subjectID
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException          - only Super Admin can do lookup
     * @throws APIException
     */
    UserIDDAO lookupUserIDDAO(String subjectID, String... params)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;


    /**
     * Look up UserPreferenceDAO based on subject ID.
     *
     * @param subjectID
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    SubjectPreference lookupUserPreferenceDAO(AppIDDAO appIDDAO, String subjectID)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;


    SubjectPreference lookupUserPreferenceDAO(AppIDDAO appIDDAO, UserIDDAO userIDDAO)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Create AppDeviceDAO.
     *
     * @param subjectAPIKey
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    SubjectAPIKey createAppDeviceDAO(AppDeviceDAO subjectAPIKey)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Create SubjectAPIKey.
     *
     * @param subjectAPIKey
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    SubjectAPIKey createSubjectAPIKey(SubjectAPIKey subjectAPIKey)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Create SubjectAPIKey.
     *
     * @param subjectAPIKey
     * @param status        ACTIVE
     * @param ttl           time to live 0 forever
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    public SubjectAPIKey createSubjectAPIKey(SubjectAPIKey subjectAPIKey, Status status, long ttl)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;


//    /**
//     * Delete SubjectAPIKey.
//     * @param subjectID
//     * @throws NullPointerException
//     * @throws IllegalArgumentException
//     * @throws AccessException
//     * @throws APIException
//     */
//	void deleteSubjectAPIKey(String subjectID)
//            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Delete SubjectAPIKey.
     *
     * @param subjectAPIKey
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    void deleteSubjectAPIKey(SubjectAPIKey subjectAPIKey)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

//    /**
//     * Look up SubjectAPIKey.
//     * @param subjectID
//     * @param throwExceptionIfNotFound if true and the subject api key not found an API exception is throw otherwise null is returned
//     * @return
//     * @throws NullPointerException
//     * @throws IllegalArgumentException
//     * @throws AccessException
//     * @throws APIException
//     */
//	<V extends SubjectAPIKey> V lookupSubjectAPIKey(String subjectID, boolean throwExceptionIfNotFound)
//            throws NullPointerException, IllegalArgumentException, AccessException, APIException;


    /**
     * Update SubjectAPIKey.
     * @param subjectAPIKey
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
//	void updateSubjectAPIKey(SubjectAPIKey subjectAPIKey)
//            throws NullPointerException, IllegalArgumentException, AccessException, APIException;
//
//	SubjectAPIKey renewSubjectAPIKEy(String subjectID)
//			 throws NullPointerException, IllegalArgumentException, AccessException, APIException;
//
//	SubjectAPIKey renewSubjectAPIKEy(SubjectAPIKey sak)
//			 throws NullPointerException, IllegalArgumentException, AccessException, APIException;


    /**
     * Look up AppIDDAO based on domain ID and app ID.
     *
     * @param domainID
     * @param appID
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    AppIDDAO lookupAppIDDAO(String domainID, String appID)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Look up AppIDDAO based on domain ID and app ID.
     *
     * @param domainID
     * @param appID
     * @param exceptionIfNotFound
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    AppIDDAO lookupAppIDDAO(String domainID, String appID, boolean exceptionIfNotFound)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;


    /**
     * Look up AppConfigDAO based on domain ID and app ID.
     *
     * @param domainID
     * @param appID
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    AppConfigDAO lookupAppConfigDAO(String domainID, String appID)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Validate JWT token.
     *
     * @param token
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    JWT validateJWT(String token)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Reset password.
     *
     * @param subjectID
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    void resetPassword(String subjectID)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;


    /**
     * Change password.
     *
     * @param oldPassword
     * @param newPassword
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    void changePassword(String oldPassword, String newPassword)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Create NVEntity object.
     *
     * @param nve
     * @param <V>
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    <V extends NVEntity> V create(V nve)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Looks up NVEntity objects based on given given subject ID and NVEntity class type.
     *
     * @param subjectID
     * @param classType
     * @param <V>
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    <V extends NVEntity> List<V> lookup(String subjectID, Class<V> classType)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Update NVEntity object.
     *
     * @param nve
     * @param <V>
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    <V extends NVEntity> V update(V nve)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     * Delete NVEntity object.
     *
     * @param nve
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    <V extends NVEntity> boolean delete(V nve)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    /**
     *
     * @param nve
     * @param withReference any referenced object
     * @param <V>
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws AccessException
     * @throws APIException
     */
    <V extends NVEntity> boolean delete(V nve, boolean withReference)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;


    AppIDDAO createAppIDDAO(String domainID, String appID);

    AppIDDAO deleteAppIDDAO(String domainID, String appID);


    <V extends NVEntity> List<V> search(NVConfigEntity nvce, QueryMarker... queryCriteria)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;

    <V extends NVEntity> List<V> search(NVConfigEntity nvce, List<String> fieldNames, QueryMarker... queryCriteria)
            throws NullPointerException, IllegalArgumentException, AccessException, APIException;


    void updateSubjectRole(String subjectID, AppIDDAO appID, String roleName, CRUD crud)
            throws NullPointerException, IllegalArgumentException, AccessException;

    void updateSubjectPermission(String subjectID, AppIDDAO appID, String permssionName, CRUD crud)
            throws NullPointerException, IllegalArgumentException, AccessException;
}