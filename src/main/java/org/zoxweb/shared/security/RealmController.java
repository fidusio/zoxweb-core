package org.zoxweb.shared.security;

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
    <C> C lookupCredential(String subjectID, CredentialInfo.Type credentialType);

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
     * Add a permission
     * @param permission to be added
     * @return the added permission
     * @throws AccessSecurityException if not permitted
     */
    PermissionInfo addPermission(PermissionInfo permission)
            throws AccessSecurityException;

    /**
     * Update a permission
     * @param permission to be updated
     * @return the updated permission
     * @throws AccessSecurityException if not permitted
     */
    PermissionInfo updatePermission(PermissionInfo permission)
            throws AccessSecurityException;

    /**
     * Delete a permission
     * @param permission to be deleted
     * @return the deleted permission null if not found
     * @throws AccessSecurityException if not permitted
     */
    PermissionInfo deletePermission(PermissionInfo permission)
            throws AccessSecurityException;

    /**
     * Add a role
     * @param role to be added
     * @return the added role
     * @throws AccessSecurityException if not permitted
     */
    RoleInfo addRole(RoleInfo role)
            throws AccessSecurityException;

    /**
     * Update a role
     * @param role to be updated
     * @return the updated role
     * @throws AccessSecurityException if not permitted
     */
    RoleInfo updateRole(RoleInfo role)
            throws AccessSecurityException;

    /**
     * Delete a role
     * @param role to be deleted
     * @return the deleted role
     * @throws AccessSecurityException if not permitted
     */
    RoleInfo deleteRole(RoleInfo role)
            throws AccessSecurityException;

    /**
     * Add a role group
     * @param roleGroup to be added
     * @return the added role group
     * @throws AccessSecurityException if not permitted
     */
    RoleGroupInfo addRoleGroup(RoleGroupInfo roleGroup)
            throws AccessSecurityException;

    /**
     * Update a role group
     * @param roleGroup to be updated
     * @return the updated role group
     * @throws AccessSecurityException if not permitted
     */
    RoleGroupInfo updateRoleGroup(RoleGroupInfo roleGroup)
            throws AccessSecurityException;

    /**
     * Delete a role group
     * @param roleGroup to be deleted
     * @return the deleted role group
     * @throws AccessSecurityException if not permitted
     */
    RoleGroupInfo deleteRoleGroup(RoleGroupInfo roleGroup)
            throws AccessSecurityException;


    /**
     * Add a grant, binding a subject to a permission, role or role group
     * @param grant to be added
     * @return the added grant
     * @throws AccessSecurityException if not permitted
     */
    GrantBase addGrant(GrantBase grant)
            throws AccessSecurityException;

    /**
     * Lookup every grant issued to a subject
     * @param subjectIdentifier the subject identifier
     * @return the subject grants, empty if none
     * @throws AccessSecurityException if not permitted
     */
    Set<GrantBase> lookupSubjectGrants(String subjectIdentifier)
            throws AccessSecurityException;

    /**
     * Update a grant
     * @param grant to be updated
     * @return the updated grant
     * @throws AccessSecurityException if not permitted
     */
    GrantBase updateGrant(GrantBase grant)
            throws AccessSecurityException;

    /**
     * Delete a grant
     * @param grant to be deleted
     * @return the deleted grant
     * @throws AccessSecurityException if not permitted
     */
    GrantBase deleteGrant(GrantBase grant)
            throws AccessSecurityException;

    /**
     *
     * @return the key maker associated with the realm controller
     * @throws AccessSecurityException if not permitted
     */
    KeyMaker getKeyMaker() throws AccessSecurityException;

    /**
     * @param keyMaker to be set for the realm controller
     * @throws AccessSecurityException if not permitted
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
