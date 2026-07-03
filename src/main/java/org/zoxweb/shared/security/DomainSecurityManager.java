package org.zoxweb.shared.security;

import org.zoxweb.shared.api.APIDataStore;

/**
 * Manages security subjects: their principals, credentials, permissions, and permission grant
 * bindings. Persistence is delegated to an {@link APIDataStore} provided via
 * {@link #setDataStore(APIDataStore)}.
 */
public interface DomainSecurityManager {


    /**
     * Perform a Principal Login and the return the subject identifier
     * @param principalID to login with
     * @param credential like password
     * @return upon success the SubjectIdentifier
     * @throws SecurityException in case of login failure
     */
    SubjectIdentifier login(String principalID, String credential) throws SecurityException;

    SubjectIdentifier loginApiKey(String key) throws SecurityException;

    // Subject Identifier

    /**
     * Creates a new subject with an initial principal and credential.
     *
     * @param principalID    the initial principal identifier for the subject
     * @param credentialInfo the initial credential for the subject
     * @return the subject object
     */
    SubjectIdentifier createSubjectID(String principalID, CredentialInfo credentialInfo);

    /**
     * Returns a subject by one of its principal identifiers.
     *
     * @param principalID the principal identifier
     * @return the matching subject, or {@code null} if none owns the principal
     */
    SubjectIdentifier lookupSubjectID(String principalID);

    /**
     * Changes an existing subject.
     *
     * @param update the subject to update
     */
    void updateSubjectID(SubjectIdentifier update);

    /**
     * Deletes a subject and its linked credentials, principals, and grants.
     *
     * @param subject the subject to delete
     * @return {@code true} if a subject was found and removed
     */
    boolean deleteSubjectID(SubjectIdentifier subject);


    // Credentials

    /**
     * Attaches a credential to the subject that owns the given principal.
     *
     * @param principalID the principal identifying the owning subject
     * @param credential  the credential to attach
     * @return the credential object
     */
    CredentialInfo createCredential(String principalID, CredentialInfo credential);

    /**
     * Reads a single credential of the given type for the subject that owns
     * the given principal.
     *
     * @param principalID the principal identifying the owning subject
     * @param type        the credential type to read
     * @return the matching credential, or {@code null} if none is stored
     */
    CredentialInfo lookupCredential(String principalID, CredentialInfo.Type type);

    /**
     * Changes an existing credential.
     *
     * @param update the credential to update
     */
    void updateCredential(CredentialInfo update);

    /**
     * Removes a credential from its owning subject.
     *
     * @param credential the credential to delete
     */
    void deleteCredential(CredentialInfo credential);

    /**
     * Returns every credential linked to the subject that owns the given
     * principal.
     *
     * @param principalID the principal identifying the owning subject
     * @return the subject's credentials
     */
    CredentialInfo[] lookupAllPrincipalCredentials(String principalID);


    // Principal Identifier

    /**
     * Associates an additional principal identifier with an existing subject,
     * allowing it to log in under multiple identities.
     *
     * @param subject     the subject to extend
     * @param principalID the additional principal identifier to associate
     * @return the principal identifier object
     */
    PrincipalIdentifier addPrincipalID(SubjectIdentifier subject, String principalID);

    /**
     * Looks up a principal by its identifier.
     *
     * @param principalID the principal identifier to resolve
     * @return the matching principal, or {@code null} if none exists
     */
    PrincipalIdentifier lookupPrincipalID(String principalID);

    /**
     * Removes a single principal identifier from a subject; the subject itself
     * is preserved.
     *
     * @param principal the principal identifier to remove
     * @return {@code true} if the principal was found and removed
     */
    boolean deletePrincipalID(PrincipalIdentifier principal);

    /**
     * Returns every principal identifier associated with a subject.
     *
     * @param subjectGUID the GUID of the subject whose principals are returned
     * @return the subject's principal identifiers
     */
    PrincipalIdentifier[] lookupAllPrincipalIdentifiers(String subjectGUID);


    // Permissions, roles, and role groups


    /**
     * Creates a new permission definition.
     *
     * @param permission the permission to create
     * @return the permission object
     */
    PermissionInfo createPermission(PermissionInfo permission);

    /**
     * Looks up a permission by application and name.
     *
     * @param appID          the application that owns the permission
     * @param permissionName the name of the permission
     * @return the matching permission, or {@code null} if none exists
     */
    PermissionInfo lookupPermission(String appID, String permissionName);

    /**
     * Returns every permission defined for the given application.
     *
     * @param appID the application whose permissions are returned
     * @return the permissions belonging to the application
     */
    PermissionInfo[] lookupAllPermissionsByAppID(String appID);

    /**
     * Updates an existing permission.
     *
     * @param update the permission to update
     */
    void updatePermission(PermissionInfo update);

    /**
     * Deletes a permission definition.
     *
     * @param permission the permission to delete
     * @return {@code true} if the permission existed and was removed
     */
    boolean deletePermission(PermissionInfo permission);

    /**
     * @return an array of all permissions
     */
    PermissionInfo[] getPermissions();


    /**
     * Creates a new role definition.
     *
     * @param role the role to create
     * @return the role object
     */
    RoleInfo createRole(RoleInfo role);

    /**
     * Looks up a role by application and name.
     *
     * @param appID    the application that owns the role
     * @param roleName the name of the role
     * @return the matching role, or {@code null} if none exists
     */
    RoleInfo lookupRole(String appID, String roleName);

    /**
     * Returns every role defined for the given application.
     *
     * @param appID the application whose roles are returned
     * @return the roles belonging to the application
     */
    RoleInfo[] lookupAllRolesByAppID(String appID);

    /**
     * Updates an existing role.
     *
     * @param update the role to update
     */
    void updateRole(RoleInfo update);

    /**
     * Deletes a role definition.
     *
     * @param role the role to delete
     * @return {@code true} if the role existed and was removed
     */
    boolean deleteRole(RoleInfo role);

    /**
     * @return an array of all roles
     */
    RoleInfo[] getRoles();

    /**
     * Creates a new role group (a named bundle of roles).
     *
     * @param roleGroup the role group to create
     * @return the role group object
     */
    RoleGroupInfo createRoleGroup(RoleGroupInfo roleGroup);

    /**
     * Looks up a role group by application and name.
     *
     * @param appID         the application that owns the role group
     * @param roleGroupName the name of the role group
     * @return the matching role group, or {@code null} if none exists
     */
    RoleGroupInfo lookupRoleGroup(String appID, String roleGroupName);

    /**
     * Returns every role group defined for the given application.
     *
     * @param appID the application whose role groups are returned
     * @return the role groups belonging to the application
     */
    RoleGroupInfo[] lookupAllRoleGroupsByAppID(String appID);

    /**
     * Updates an existing role group.
     *
     * @param update the role group to update
     */
    void updateRoleGroup(RoleGroupInfo update);

    /**
     * Deletes a role group definition.
     *
     * @param roleGroup the role group to delete
     * @return {@code true} if the role group existed and was removed
     */
    boolean deleteRoleGroup(RoleGroupInfo roleGroup);

    /**
     * @return an array of all role groups
     */
    RoleGroupInfo[] getRoleGroups();


    // Grants

    /**
     * Grants a permission to a subject by creating and persisting a new
     * {@link PermissionGrant} for the supplied {@link PermissionInfo}.
     *
     * @param subject        the subject receiving the grant
     * @param permissionInfo the permission to grant
     * @return the grant object
     */
    PermissionGrant addPermissionGrant(SubjectIdentifier subject, PermissionInfo permissionInfo);

    /**
     * Revokes a previously-issued permission grant.
     *
     * @param permissionGrant the grant to revoke
     * @return {@code true} if the grant existed and was removed
     */
    boolean deletePermissionGrant(PermissionGrant permissionGrant);

    /**
     * Returns every permission grant currently attached to a subject.
     *
     * @param subjectGUID the GUID of the subject whose grants are returned
     * @return the subject's permission grants
     */
    PermissionGrant[] getPermissionGrants(String subjectGUID);


    /**
     * Grants a role to a subject by creating and persisting a new
     * {@link RoleGrant} for the supplied {@link RoleInfo}.
     *
     * @param subject  the subject receiving the grant
     * @param roleInfo the role to grant
     * @return the grant object
     */
    RoleGrant addRoleGrant(SubjectIdentifier subject, RoleInfo roleInfo);

    /**
     * Revokes a previously-issued role grant.
     *
     * @param roleGrant the grant to revoke
     * @return {@code true} if the grant existed and was removed
     */
    boolean deleteRoleGrant(RoleGrant roleGrant);

    /**
     * Returns every role grant currently attached to a subject.
     *
     * @param subjectGUID the GUID of the subject whose grants are returned
     * @return the subject's role grants
     */
    RoleGrant[] getRoleGrants(String subjectGUID);


    /**
     * Grants a role group (a bundle of roles) to a subject by creating and
     * persisting a new {@link RoleGroupGrant} for the supplied
     * {@link RoleGroupInfo}.
     *
     * @param subject       the subject receiving the grant
     * @param roleGroupInfo the role group to grant
     * @return the grant object
     */
    RoleGroupGrant addRoleGroupGrant(SubjectIdentifier subject, RoleGroupInfo roleGroupInfo);

    /**
     * Revokes a previously-issued role group grant.
     *
     * @param roleGroupGrant the grant to revoke
     * @return {@code true} if the grant existed and was removed
     */
    boolean deleteRoleGroupGrant(RoleGroupGrant roleGroupGrant);

    /**
     * Returns every role group grant currently attached to a subject.
     *
     * @param subjectGUID the GUID of the subject whose grants are returned
     * @return the subject's role group grants
     */
    RoleGroupGrant[] getRoleGroupGrants(String subjectGUID);


    // Data store

    /**
     * Injects the persistence layer.
     *
     * @param dataStore the data store to back this manager
     */
    DomainSecurityManager setDataStore(APIDataStore<?, ?> dataStore);

    /**
     * Returns the data store currently backing this manager, or {@code null}.
     *
     * @return the backing data store, or {@code null} if none is set
     */
    APIDataStore<?, ?> getDataStore();

     DomainSecurityManager addCredentialType(Class<? extends CredentialInfo> clazz);
}
