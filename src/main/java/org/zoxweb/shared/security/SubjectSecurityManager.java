package org.zoxweb.shared.security;

import org.zoxweb.shared.api.APIDataStore;

/**
 * Manages security subjects: their principals, credentials, and grant
 * bindings. Persistence is delegated to an {@link APIDataStore} provided via
 * {@link #setDataStore(APIDataStore)}.
 */
public interface SubjectSecurityManager {


    // Subject Identifier

    /**
     * Creates a new subject with an initial principal and credential.
     */
    void createSubjectID(String principalID, CredentialInfo credentialInfo);

    /**
     * Returns a subject by one of its principal identifiers.
     *
     * @return the matching subject, or {@code null} if none owns the principal
     */
    SubjectIdentifier lookupSubjectID(String principalID);

    /**
     * Persists changes to an existing subject.
     */
    void updateSubjectID(SubjectIdentifier subject);

    /**
     * Deletes a subject and its linked credentials, principals, and grants.
     *
     * @return {@code true} if a subject was found and removed
     */
    boolean deleteSubjectID(String subjectGUID);


    // Credentials

    /**
     * Attaches a credential to the subject that owns the given principal.
     *
     * @return the persisted credential
     */
    CredentialInfo createCredential(String principalID, CredentialInfo credential);

    /**
     * Reads a single credential of the given type for the subject that owns
     * the given principal.
     *
     * @return the matching credential, or {@code null} if none is stored
     */
    CredentialInfo lookupCredential(String principalID, CredentialInfo.Type type);

    //void updateCredential(String subjectGUID, CredentialInfo credential);

    //void deleteCredential(String subjectGUID, CredentialInfo.Type type);

    /**
     * Returns every credential linked to the subject that owns the given
     * principal.
     */
    CredentialInfo[] lookupAllPrincipalCredentials(String principalID);


    // Principal Identifier

    /**
     * Associates an additional principal identifier with an existing subject,
     * allowing it to log in under multiple identities.
     *
     * @return the persisted principal identifier
     */
    PrincipalIdentifier addPrincipalID(SubjectIdentifier subject, String principalID);

    /**
     * Looks up a specific principal owned by a specific subject.
     *
     * @return the matching principal, or {@code null} if the subject does not
     * own it
     */
    PrincipalIdentifier lookupPrincipalID(String subjectGUID, String principalID);

    /**
     * Removes a single principal identifier from a subject; the subject itself
     * is preserved.
     *
     * @return {@code true} if the principal was found and removed
     */
    boolean deletePrincipalID(String subjectGUID, String principalID);

    /**
     * Returns every principal identifier associated with a subject.
     */
    PrincipalIdentifier[] lookupAllPrincipalIdentifiers(String subjectGUID);


    // Permissions

    /**
     * Grants a permission to a subject by creating and persisting a new
     * {@link PermissionGrant} for the supplied {@link PermissionInfo}.
     *
     * @return the persisted grant
     */
    PermissionGrant addPermissionGrant(SubjectIdentifier subject, PermissionInfo permissionInfo);

    /**
     * Revokes a previously-issued permission grant.
     *
     * @return {@code true} if the grant existed and was removed
     */
    boolean deletePermissionGrant(PermissionGrant permissionGrant);

    /**
     * Returns every permission grant currently attached to a subject.
     */
    PermissionGrant[] getPermissionGrants(String subjectGUID);


    /**
     * Grants a role to a subject by creating and persisting a new
     * {@link RoleGrant} for the supplied {@link RoleInfo}.
     *
     * @return the persisted grant
     */
    RoleGrant addRoleGrant(SubjectIdentifier subject, RoleInfo roleInfo);

    /**
     * Revokes a previously-issued role grant.
     *
     * @return {@code true} if the grant existed and was removed
     */
    boolean deleteRoleGrant(RoleGrant roleGrant);

    /**
     * Returns every role grant currently attached to a subject.
     */
    RoleGrant[] getRoleGrants(String subjectGUID);


    /**
     * Grants a role group (a bundle of roles) to a subject by creating and
     * persisting a new {@link RoleGroupGrant} for the supplied
     * {@link RoleGroupInfo}.
     *
     * @return the persisted grant
     */
    RoleGroupGrant addRoleGroupGrant(SubjectIdentifier subject, RoleGroupInfo roleGroupInfo);

    /**
     * Revokes a previously-issued role group grant.
     *
     * @return {@code true} if the grant existed and was removed
     */
    boolean deleteRoleGroupGrant(RoleGroupGrant roleGroupGrant);

    /**
     * Returns every role group grant currently attached to a subject.
     */
    RoleGroupGrant[] getRoleGroupGrants(String subjectGUID);


    // Data store

    /**
     * Injects the persistence layer.
     */
    void setDataStore(APIDataStore<?, ?> dataStore);

    /**
     * Returns the data store currently backing this manager, or {@code null}.
     */
    APIDataStore<?, ?> getDataStore();
}
