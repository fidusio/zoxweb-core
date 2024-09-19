package org.zoxweb.shared.security.shiro;

import org.zoxweb.shared.security.*;
import org.zoxweb.shared.util.BaseSubjectID;

import java.util.Set;


public interface RealmController<O,I>
extends AuthorizationInfoLookup<O,I>
{

    /**
     * Create a subject identifier
     * @param subjectID the email or uuid identifier of the subject
     * @param subjectType the type of the subject
     * @param credential subject credentials
     * @return the created subject identifier
     * @throws AccessSecurityException if not permitted
     */
    SubjectIdentifier addSubjectIdentifier(String subjectID, BaseSubjectID.SubjectType subjectType, CredentialInfo credential)
            throws AccessSecurityException;


    /**
     * Create a subject identifier
     * @param subjectIdentifier the subject identifier
     * @param credential the subject credential
     * @return the created subject identifier
     * @throws AccessSecurityException if not permitted
     */
    SubjectIdentifier addSubjectIdentifier(SubjectIdentifier subjectIdentifier, CredentialInfo credential)
            throws AccessSecurityException;


    /**
     * Delete a user identifier use with extreme care
     * @param subjectID to be deleted
     * @return the deleted subject identifier
     * @throws AccessSecurityException if not permitted
     */
    SubjectIdentifier deleteSubjectIdentifier(String subjectID)
            throws AccessSecurityException;

    /**
     * Lookup the subject identifier based on its id
     * @param subjectID to look for
     * @return the matching subject identifier, null if not found
     * @throws AccessSecurityException if not permitted
     */
    SubjectIdentifier lookupSubjectIdentifier(String subjectID)
            throws AccessSecurityException;


    /**
     * Lookup subject credential info
     * @param subjectID the subject identifier
     * @param credentialType the
     * @return the subject credential
     * @param <C> of instance CredentialInfo
     */
    <C> C lookupCredential(String subjectID, CredentialInfo.CredentialType credentialType);

    /**
     * Add a credential object for the specified subject
     * @param subjectID than owns the credentials
     * @param ci the credential info object ie: password, public key, token ...
     * @return the validated credential info object
     * @throws AccessSecurityException if not permitted
     */
    CredentialInfo addCredentialInfo(String subjectID, CredentialInfo ci)
            throws AccessSecurityException;

    /**
     * Add a credential object for the specified subject
     * @param subjectID than owns the credentials
     * @param password the credential info object ie: password, public key, token ...
     * @return the validated credential info object
     * @throws AccessSecurityException if not permitted
     */
    CredentialInfo addCredentialInfo(String subjectID, String password)
            throws AccessSecurityException;

    /**
     * Add a credential object for the specified subject
     * @param subjectID than owns the credentials
     * @param password the credential info object ie: password, public key, token ...
     * @return the validated credential info object
     * @throws AccessSecurityException if not permitted
     */
    CredentialInfo addCredentialInfo(String subjectID, byte[] password)
            throws AccessSecurityException;


    /**
     * Delete a credential info
     * @param ci to be deleted
     * @return the deleted credential info
     * @throws AccessSecurityException if not permitted
     */
    CredentialInfo deleteCredentialInfo(CredentialInfo ci)
            throws AccessSecurityException;

    // todo check i needed
    CredentialInfo updateCredentialInfo(CredentialInfo oldCI, CredentialInfo newCI)
            throws AccessSecurityException;


    /**
     * Add a shiro permission
     * @param permission to be added
     * @return the added permission
     * @throws AccessSecurityException if not permitted
     */
    ShiroPermission addPermission(ShiroPermission permission)
            throws AccessSecurityException;

    /**
     * Updated a shiro permission
     * @param permission to be updated
     * @return the shiro permission
     * @throws AccessSecurityException if no permitted
     */
    ShiroPermission updatePermission(ShiroPermission permission)
            throws AccessSecurityException;

    /**
     * Delete a shiro permission
     * @param permission to be deleted
     * @return the deleted permission null if not found
     * @throws AccessSecurityException if not permitted
     */
    ShiroPermission deletePermission(ShiroPermission permission)
            throws AccessSecurityException;

    /**
     * Add a shiro role
     * @param shiroRole to be added
     * @return the added shiro role
     * @throws AccessSecurityException if not permitted
     */
    ShiroRole addRole(ShiroRole shiroRole)
            throws AccessSecurityException;

    /**
     * Update a shiro role
     * @param shiroRole to be added
     * @return the added shiro role
     * @throws AccessSecurityException if not permitted
     */
    ShiroRole updateRole(ShiroRole shiroRole)
            throws AccessSecurityException;

    /**
     * Delete a shiro role
     * @param shiroRole to be deleted
     * @return the deleted shiro role
     * @throws AccessSecurityException if not permitted
     */
    ShiroRole deleteRole(ShiroRole shiroRole)
            throws AccessSecurityException;

    /**
     * Add a shiro group role
     * @param shiroRoleGroup to be added
     * @return the added shiro role group
     * @throws AccessSecurityException if not permitted
     */
    ShiroRoleGroup addRoleGroup(ShiroRoleGroup shiroRoleGroup)
            throws AccessSecurityException;

    /**
     * Update a shiro group role
     * @param shiroRoleGroup to be updated
     * @return the updated shiro role group
     * @throws AccessSecurityException if not permitted
     */
    ShiroRoleGroup updateRoleGroup(ShiroRoleGroup shiroRoleGroup)
            throws AccessSecurityException;

    /**
     * Delete a shiro group role
     * @param shiroRoleGroup to be deleted
     * @return the deleted shiro role group
     * @throws AccessSecurityException if not permitted
     */
    ShiroRoleGroup deleteRoleGroup(ShiroRoleGroup shiroRoleGroup)
            throws AccessSecurityException;


    /**
     * Add a shiro authorization info
     * @param shiroAuthzInfo to added
     * @return the added shiro authorization info
     * @throws AccessSecurityException if no permitted
     */
    ShiroAuthzInfo addShiroAuthzInfo(ShiroAuthzInfo shiroAuthzInfo)
            throws AccessSecurityException;

    Set<ShiroAuthzInfo> lookupSubjectAuthzInfo(String subjectIdentifier)
            throws AccessSecurityException;

    /**
     * Update a shiro authorization info
     * @param shiroAuthzInfo to updated
     * @return the updated shiro authorization info
     * @throws AccessSecurityException if no permitted
     */
    ShiroAuthzInfo updateShiroAuthzInfo(ShiroAuthzInfo shiroAuthzInfo)
            throws AccessSecurityException;
    /**
     * Delete a shiro authorization info
     * @param shiroAuthzInfo to delted
     * @return the deleted shiro authorization info
     * @throws AccessSecurityException if no permitted
     */
    ShiroAuthzInfo deleteShiroAuthzInfo(ShiroAuthzInfo shiroAuthzInfo)
            throws AccessSecurityException;

    /**
     *
     * @return the key maker associated with shiro realm controller
     * @throws AccessSecurityException if not permitted
     */
    KeyMaker getKeyMaker() throws AccessSecurityException;

    /**
     * @param keyMaker to be set for the shiro realm controller
     * @throws AccessSecurityException if no permitted
     */
    void setKeyMaker(KeyMaker keyMaker) throws AccessSecurityException;


    /**
     * Lookup subject resource security based on the subject id
     * @param subjectID the subject identifier can't be null
     * @param domainID the domain id can be null
     * @param appID the app id can be null
     * @return permissions and role associated with subject
     */
    ResourceSecurity subjectResourceSecurity(String subjectID, String domainID, String appID);

}
