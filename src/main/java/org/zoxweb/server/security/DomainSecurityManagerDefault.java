package org.zoxweb.server.security;

import org.zoxweb.server.util.MockAPIDataStore;
import org.zoxweb.server.util.UUID7;
import org.zoxweb.shared.api.APIDataStore;
import org.zoxweb.shared.crypto.CIPassword;
import org.zoxweb.shared.crypto.CredentialHasher;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.db.QueryMatch;
import org.zoxweb.shared.security.*;
import org.zoxweb.shared.util.Const.RelationalOperator;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

import java.util.*;

/**
 * Default {@link DomainSecurityManager} implementation that delegates all persistence
 * to an injected {@link APIDataStore} (typically a {@link MockAPIDataStore} in unit
 * tests). No data store is set by construction; one must be supplied via
 * {@link #setDataStore(APIDataStore)} before use, otherwise operations fail with an
 * {@link IllegalStateException}.
 *
 * <p>Every entity (subjects, principals, credentials, the permission/role/role-group
 * catalog, and grants) is stored and retrieved through the data store; this class holds
 * no entity state of its own beyond a small registry of which collections hold
 * credentials ({@link #addCredentialType(Class)}). Linkage is by GUID: principals and
 * grants carry a {@code subject_guid}, and lookups resolve a principal to its owning
 * subject, then fetch related rows by GUID/field query.</p>
 *
 * <p>{@link #login(String, String)} resolves the principal to its owning subject,
 * loads that subject's {@link CredentialInfo.Type#PASSWORD} credential, and validates it
 * with {@link SecUtil#isPasswordValid(CIPassword, String)} - so the stored credential
 * must be a hashed {@link CIPassword}.</p>
 */
public class DomainSecurityManagerDefault
        implements DomainSecurityManager {

    private static final String FIELD_SUBJECT_GUID = "subject_guid";
    private static final String FIELD_NAME = "name";

    // NVConfigEntity collections, by name, that currently hold credentials
    private final Set<Class<?>> credentialCollections = new HashSet<>();

    private volatile APIDataStore<?, ?> dataStore;

    /**
     * Creates a manager with no backing data store; one must be injected via
     * {@link #setDataStore(APIDataStore)} before any operation is invoked.
     */
    public DomainSecurityManagerDefault() {
    }


    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * Returns the injected data store, failing fast if none has been set.
     *
     * @return the backing data store
     * @throws IllegalStateException if no data store has been injected
     */
    private APIDataStore<?, ?> ds() {
        if (dataStore == null) {
            throw new IllegalStateException("No data store set");
        }
        return dataStore;
    }

    /**
     * Returns the first element of a search result, or {@code null} if the
     * result is {@code null} or empty.
     */
    private static <V> V first(List<V> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    /**
     * Builds an equality query criterion for the given field and value.
     */
    private static QueryMatch<String> eq(String field, String value) {
        return new QueryMatch<>(field, value, RelationalOperator.EQUAL);
    }

    /**
     * Checks whether an authorization entity belongs to the given application,
     * comparing app IDs case-insensitively; a {@code null} owned app ID matches
     * only a {@code null} appID.
     */
    private static boolean appIDMatches(AuthzInfo info, String appID) {
        String owned = info.getAppIdDAO() != null ? info.getAppIdDAO().getAppID() : null;
        return SharedStringUtil.equals(owned, appID, true);
    }

    /**
     * Fetches the {@link PrincipalIdentifier} row matching the given principal
     * identifier, or {@code null} if none exists.
     */
    private PrincipalIdentifier resolvePrincipal(String principalID) {
        return first(ds().search(PrincipalIdentifier.NVC_PRINCIPAL_IDENTIFIER, null,
                new QueryMatch<>(RelationalOperator.EQUAL, principalID, PrincipalIdentifier.Param.PRINCIPAL_ID)));
    }

    /**
     * Resolves a principal identifier to the GUID of its owning subject, or
     * {@code null} if the principal is unknown.
     */
    private String resolveSubjectGUID(String principalID) {
        PrincipalIdentifier principal = resolvePrincipal(principalID);
        return principal != null ? principal.getSubjectGUID() : null;
    }

    // ------------------------------------------------------------------
    // login
    // ------------------------------------------------------------------

    /**
     * Authenticates a principal against its stored password credential. The
     * principal is resolved to its owning subject, the subject's
     * {@link CredentialInfo.Type#PASSWORD} credential is loaded, and the supplied
     * clear-text password is validated against the stored hash with
     * {@link SecUtil#isPasswordValid(CIPassword, String)}. The same generic
     * "Invalid credentials" failure is raised whether the principal is unknown
     * or the password does not match.
     *
     * @param principalID the principal identifier to log in with
     * @param credential  the clear-text password to validate
     * @return the subject that owns the principal
     * @throws SecurityException if the principal is unknown, no password
     *                           credential is stored, or validation fails
     */
    @Override
    public SubjectIdentifier login(String principalID, String credential) throws SecurityException {
        SubjectIdentifier subject = lookupSubjectID(principalID);
        if (subject == null) {
            throw new SecurityException("Invalid credentials");
        }

        CredentialInfo ci = lookupCredential(principalID, CredentialInfo.Type.PASSWORD);
        if (!(ci instanceof CIPassword) || !SecUtil.isPasswordValid((CIPassword) ci, credential)) {
            throw new SecurityException("Invalid credentials");
        }

        return subject;
    }

    /**
     * Authenticates with an API key by looking up the matching
     * {@link SubjectAPIKey} row and resolving its owning subject. The same
     * generic "Invalid key" failure is raised whether the key is {@code null},
     * unknown, or its subject no longer exists.
     *
     * @param key the API key to log in with
     * @return the subject that owns the API key
     * @throws SecurityException if the key cannot be resolved to a subject
     */
    @Override
    public SubjectIdentifier loginApiKey(String key) throws SecurityException {
        if (key == null) {
            throw new SecurityException("Invalid key");
        }

        SubjectAPIKey sak = first(ds().search(SubjectAPIKey.NVC_SUBJECT_API_KEY, null, eq(SubjectAPIKey.Param.API_KEY.getNVConfig().getName(), key)));
        if (sak == null) {
            throw new SecurityException("Invalid key");
        }

        SubjectIdentifier subject =
                first(ds().searchByID(SubjectIdentifier.NVC_SUBJECT_IDENTIFIER, sak.getSubjectGUID()));
        if (subject == null) {
            throw new SecurityException("Invalid key");
        }

        return subject;
    }

    // ------------------------------------------------------------------
    // subject identifier
    // ------------------------------------------------------------------

    /**
     * Creates a new subject with an initial principal and, optionally, an initial
     * credential. Runs inside a data-store transaction: the subject is inserted
     * with a fresh UUIDv7 GUID and {@code ACTIVE} status, the principal is bound
     * to it, and the credential (if non-{@code null}) is attached; any failure
     * aborts the transaction.
     *
     * @param principalID    the initial principal identifier for the subject
     * @param credentialInfo the initial credential, or {@code null} for none
     * @return the persisted subject
     * @throws SecurityException if the principal already exists or the
     *                           transaction fails
     */
    @Override
    public synchronized SubjectIdentifier createSubjectID(String principalID, CredentialInfo credentialInfo) {
        APIDataStore<?, ?> dataStore = ds();
        dataStore.beginTransaction();
        try {

            PrincipalIdentifier principal = resolvePrincipal(principalID);
            if (principal != null) {
                throw new SecurityException("principal already exists");
            }

            SubjectIdentifier subject = new SubjectIdentifier();
            subject.setGUID(UUID7.randomUUID().toString());
            subject.setSubjectStatus(SecConst.SecStatus.ACTIVE);
            subject = dataStore.insert(subject); // assigns the GUID (and subject_guid)

            // register the initial principal and bind it to the subject
            addPrincipalID(subject, principalID);

            if (credentialInfo != null) {
                createCredential(principalID, credentialInfo);
            }
            return subject;
        } catch (Exception e) {
            dataStore.abortTransaction();
            throw new SecurityException(e);
        } finally {
            dataStore.endTransaction();
        }
    }

    /**
     * Creates a new subject with an initial principal and a password credential.
     * The clear-text password is hashed with the hasher registered for the given
     * algorithm ({@link SecUtil#lookupCredentialHasher(String)}) and the result
     * is delegated to {@link #createSubjectID(String, CredentialInfo)}.
     *
     * @param principalID the initial principal identifier for the subject
     * @param password    the initial clear-text password for the subject
     * @param hashType    the hash algorithm used to store the password
     * @return the persisted subject
     * @throws SecurityException if the subject cannot be created
     */
    @Override
    public SubjectIdentifier createSubjectID(String principalID, String password, CryptoConst.HashType hashType) throws SecurityException {
        CredentialHasher<CIPassword> hasher = SecUtil.lookupCredentialHasher(hashType.getName());
        CIPassword ciPassword = hasher.hash(password);

        return createSubjectID(principalID, ciPassword);
    }


    /**
     * Returns the subject that owns the given principal by resolving the
     * principal to its {@code subject_guid} and fetching the subject by GUID.
     *
     * @param principalID the principal identifier
     * @return the owning subject, or {@code null} if the principal is unknown
     */
    @Override
    public SubjectIdentifier lookupSubjectID(String principalID) {
        String subjectGUID = resolveSubjectGUID(principalID);
        if (subjectGUID == null) {
            return null;
        }
        return first(ds().searchByID(SubjectIdentifier.NVC_SUBJECT_IDENTIFIER, subjectGUID));
    }

    /**
     * Persists changes to an existing subject; silently ignored if the subject
     * is {@code null} or has no GUID.
     *
     * @param update the subject to update
     */
    @Override
    public void updateSubjectID(SubjectIdentifier update) {
        if (update != null && update.getGUID() != null) {
            ds().update(update);
        }
    }

    /**
     * Deletes a subject and cascades to everything linked to it by
     * {@code subject_guid}: principals, credentials in every registered
     * credential collection, and permission/role/role-group grants. The cascade
     * is not transactional; the subject row itself is removed last.
     *
     * @param subject the subject to delete
     * @return {@code true} if the subject row was found and removed
     */
    @Override
    public boolean deleteSubjectID(SubjectIdentifier subject) {
        if (subject == null || SUS.isEmpty(subject.getGUID())) {
            return false;
        }
        String subjectGUID = subject.getGUID();

        // cascade: principals, credentials, and grants owned by the subject
        for (PrincipalIdentifier p : lookupAllPrincipalIdentifiers(subjectGUID)) {
            ds().delete(p, false);
        }
        for (Class<?> credColl : credentialCollections.toArray(new Class[0])) {
            for (NVEntity ci : ds().search(credColl.getName(), null, eq(FIELD_SUBJECT_GUID, subjectGUID))) {
                ds().delete(ci, false);
            }
        }
        for (PermissionGrant g : getPermissionGrants(subjectGUID)) {
            ds().delete(g, false);
        }
        for (RoleGrant g : getRoleGrants(subjectGUID)) {
            ds().delete(g, false);
        }
        for (RoleGroupGrant g : getRoleGroupGrants(subjectGUID)) {
            ds().delete(g, false);
        }

        return ds().delete(subject, false);
    }

    // ------------------------------------------------------------------
    // credentials
    // ------------------------------------------------------------------

    /**
     * Attaches a credential to the subject that owns the given principal. The
     * credential must also be an {@link NVEntity} so it can be persisted; its
     * {@code subject_guid} is stamped with the owning subject's GUID before
     * insertion.
     *
     * @param principalID the principal identifying the owning subject
     * @param credential  the credential to attach
     * @return the attached credential
     * @throws SecurityException        if the principal is unknown
     * @throws IllegalArgumentException if the credential is not an {@link NVEntity}
     */
    @Override
    public CredentialInfo createCredential(String principalID, CredentialInfo credential) {
        String subjectGUID = resolveSubjectGUID(principalID);
        if (subjectGUID == null) {
            throw new SecurityException("Unknown principal: " + principalID);
        }
        if (!(credential instanceof NVEntity)) {
            throw new IllegalArgumentException("Credential must be an NVEntity to be persisted");
        }
        NVEntity nve = (NVEntity) credential;
        nve.setSubjectGUID(subjectGUID);
        ds().insert(nve);
        return credential;
    }


    /**
     * Attaches a credential to the subject that owns the given principal.
     *
     * @param subjectIdentifier the subject identifying the owning subject
     * @param credential  the credential to attach
     * @return the credential object
     */
    public CredentialInfo createCredential(SubjectIdentifier subjectIdentifier, CredentialInfo credential){
        String subjectGUID = subjectIdentifier.getSubjectGUID();
        if (subjectGUID == null) {
            throw new SecurityException("Unknown subject");
        }
        if (!(credential instanceof NVEntity)) {
            throw new IllegalArgumentException("Credential must be an NVEntity to be persisted");
        }
        NVEntity nve = (NVEntity) credential;
        nve.setSubjectGUID(subjectGUID);
        ds().insert(nve);
        return credential;
    }

    /**
     * Returns the first credential of the given type stored for the subject
     * that owns the given principal, scanning all registered credential
     * collections.
     *
     * @param principalID the principal identifying the owning subject
     * @param type        the credential type to read
     * @return the matching credential, or {@code null} if the principal is
     *         unknown or no credential of that type is stored
     */
    @Override
    public CredentialInfo lookupCredential(String principalID, CredentialInfo.Type type) {
        String subjectGUID = resolveSubjectGUID(principalID);
        if (subjectGUID == null) {
            return null;
        }
        CredentialInfo[] ret = lookupCredentialsBySubjectGUID(subjectGUID, type);
        return ret.length > 0 ? ret[0] : null;
    }

    /**
     * Changes an existing credential.
     *
     * @param subjectIdentifier to update credentials
     * @param update the credential to update
     *
     */
    @Override
    public void updateCredential(SubjectIdentifier subjectIdentifier, CredentialInfo update) {
        SUS.checkIfNulls("subjectIdentifier and credentail info can't be null", subjectIdentifier, update);
        // we have an update
        // must check permission to do so
        // user or domain admin to be added later
        APIDataStore<?, ?> dataStore = ds();


        // if the credential info has a guid
        if (update instanceof NVEntity && ((NVEntity) update).getGUID() != null) {
            dataStore.update((NVEntity) update);
            return;
        }

        // begin the transaction
        dataStore.beginTransaction();
        boolean status = false;
        try {
            if (update instanceof CIPassword) {
                CIPassword newPassword = (CIPassword) update;
                newPassword.setSubjectGUID(subjectIdentifier.getGUID());
                CredentialInfo[] oldPassword = lookupCredentialsBySubjectGUID(subjectIdentifier.getSubjectGUID(), CredentialInfo.Type.PASSWORD);
                if (oldPassword.length > 0) {
                    dataStore.delete((CIPassword) oldPassword[0], false);
                }
                dataStore.insert(newPassword);
                status = true;
            }

        } finally {
            if (status)
                ds().endTransaction();
            else
                ds().abortTransaction();

        }
    }

    /**
     * Removes a credential from the data store; silently ignored unless the
     * credential is an {@link NVEntity}.
     *
     * @param credential the credential to delete
     */
    @Override
    public void deleteCredential(CredentialInfo credential) {
        if (credential instanceof NVEntity) {
            ds().delete((NVEntity) credential, false);
        }
    }

    /**
     * Returns every credential linked to the subject that owns the given
     * principal, across all registered credential collections.
     *
     * @param principalID the principal identifying the owning subject
     * @return the subject's credentials; empty if the principal is unknown
     */
    @Override
    public CredentialInfo[] lookupAllPrincipalCredentials(String principalID) {
        return lookupCredentialsBySubjectGUID(resolveSubjectGUID(principalID), null);
    }

    /**
     * Collects every {@link CredentialInfo} stored for the given subject GUID
     * by querying each registered credential collection on {@code subject_guid}.
     */
    @Override
    public CredentialInfo[] lookupCredentialsBySubjectGUID(String subjectGUID, CredentialInfo.Type type) {
        List<CredentialInfo> ret = new ArrayList<>();
        if (subjectGUID == null) {
            return ret.toArray(new CredentialInfo[0]);
        }
        for (Class<?> credColl : credentialCollections.toArray(new Class[0])) {
            for (NVEntity nve : ds().search(credColl.getName(), null, eq(FIELD_SUBJECT_GUID, subjectGUID))) {
                if (nve instanceof CredentialInfo) {
                    if (type != null) {
                        if (((CredentialInfo) nve).getCredentialType() == type)
                            ret.add((CredentialInfo) nve);
                    } else
                        ret.add((CredentialInfo) nve);
                }
            }
        }
        return ret.toArray(new CredentialInfo[0]);
    }

    // ------------------------------------------------------------------
    // principal identifier
    // ------------------------------------------------------------------

    /**
     * Associates an additional principal identifier with an existing subject by
     * inserting a new {@link PrincipalIdentifier} bound to the subject's GUID.
     * No duplicate check is performed here; callers such as
     * {@link #createSubjectID(String, CredentialInfo)} enforce uniqueness.
     *
     * @param subject     the subject to extend
     * @param principalID the additional principal identifier to associate
     * @return the persisted principal identifier
     */
    @Override
    public PrincipalIdentifier addPrincipalID(SubjectIdentifier subject, String principalID) {
        PrincipalIdentifier principal = new PrincipalIdentifier(principalID);
        principal.setSubjectGUID(subject.getGUID());
        return ds().insert(principal);
    }

    /**
     * Looks up a principal by its identifier.
     *
     * @param principalID the principal identifier to resolve
     * @return the matching principal, or {@code null} if none exists
     */
    @Override
    public PrincipalIdentifier lookupPrincipalID(String principalID) {
        return resolvePrincipal(principalID);
    }

    /**
     * Removes a single principal identifier row; the owning subject and its
     * other principals are preserved.
     *
     * @param principal the principal identifier to remove
     * @return {@code true} if the principal was found and removed
     */
    @Override
    public boolean deletePrincipalID(PrincipalIdentifier principal) {
        return principal != null && ds().delete(principal, false);
    }

    /**
     * Returns every principal identifier whose {@code subject_guid} matches the
     * given subject GUID.
     *
     * @param subjectGUID the GUID of the subject whose principals are returned
     * @return the subject's principal identifiers
     */
    @Override
    public PrincipalIdentifier[] lookupAllPrincipalIdentifiers(String subjectGUID) {
        List<PrincipalIdentifier> list = ds().search(PrincipalIdentifier.NVC_PRINCIPAL_IDENTIFIER, null,
                eq(FIELD_SUBJECT_GUID, subjectGUID));
        return list.toArray(new PrincipalIdentifier[0]);
    }

    // ------------------------------------------------------------------
    // permissions
    // ------------------------------------------------------------------

    /**
     * Persists a new permission definition.
     *
     * @param permission the permission to create
     * @return the persisted permission
     */
    @Override
    public PermissionInfo createPermission(PermissionInfo permission) {
        return ds().insert(permission);
    }

    /**
     * Looks up a permission by name (data-store query on the {@code name}
     * field) and application (case-insensitive app ID comparison).
     *
     * @param appID          the application that owns the permission
     * @param permissionName the name of the permission
     * @return the matching permission, or {@code null} if none exists
     */
    @Override
    public PermissionInfo lookupPermission(String appID, String permissionName) {
        for (PermissionInfo p : this.<PermissionInfo>byName(PermissionInfo.NVC_PERMISSION_INFO, permissionName)) {
            if (appIDMatches(p, appID)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Returns every permission belonging to the given application by filtering
     * the full permission catalog in memory.
     *
     * @param appID the application whose permissions are returned
     * @return the permissions belonging to the application
     */
    @Override
    public PermissionInfo[] lookupAllPermissionsByAppID(String appID) {
        List<PermissionInfo> ret = new ArrayList<>();
        for (PermissionInfo p : getPermissions()) {
            if (appIDMatches(p, appID)) {
                ret.add(p);
            }
        }
        return ret.toArray(new PermissionInfo[0]);
    }

    /**
     * Persists changes to an existing permission; silently ignored if the
     * permission is {@code null} or has no GUID.
     *
     * @param update the permission to update
     */
    @Override
    public void updatePermission(PermissionInfo update) {
        if (update != null && update.getGUID() != null) {
            ds().update(update);
        }
    }

    /**
     * Deletes a permission definition; existing grants referencing it are not
     * cascaded.
     *
     * @param permission the permission to delete
     * @return {@code true} if the permission existed and was removed
     */
    @Override
    public boolean deletePermission(PermissionInfo permission) {
        return permission != null && ds().delete(permission, false);
    }

    /**
     * Returns every permission definition across all applications (full scan
     * of the permission collection).
     *
     * @return all defined permissions
     */
    @Override
    public PermissionInfo[] getPermissions() {
        List<PermissionInfo> list = ds().search(PermissionInfo.NVC_PERMISSION_INFO, null);
        return list.toArray(new PermissionInfo[0]);
    }

    // ------------------------------------------------------------------
    // roles
    // ------------------------------------------------------------------

    /**
     * Persists a new role definition.
     *
     * @param role the role to create
     * @return the persisted role
     */
    @Override
    public RoleInfo createRole(RoleInfo role) {
        return ds().insert(role);
    }

    /**
     * Looks up a role by name (data-store query on the {@code name} field) and
     * application (case-insensitive app ID comparison).
     *
     * @param appID    the application that owns the role
     * @param roleName the name of the role
     * @return the matching role, or {@code null} if none exists
     */
    @Override
    public RoleInfo lookupRole(String appID, String roleName) {
        for (RoleInfo r : this.<RoleInfo>byName(RoleInfo.NVC_ROLE_INFO, roleName)) {
            if (appIDMatches(r, appID)) {
                return r;
            }
        }
        return null;
    }

    /**
     * Returns every role belonging to the given application by filtering the
     * full role catalog in memory.
     *
     * @param appID the application whose roles are returned
     * @return the roles belonging to the application
     */
    @Override
    public RoleInfo[] lookupAllRolesByAppID(String appID) {
        List<RoleInfo> ret = new ArrayList<>();
        for (RoleInfo r : getRoles()) {
            if (appIDMatches(r, appID)) {
                ret.add(r);
            }
        }
        return ret.toArray(new RoleInfo[0]);
    }

    /**
     * Persists changes to an existing role; silently ignored if the role is
     * {@code null} or has no GUID.
     *
     * @param update the role to update
     */
    @Override
    public void updateRole(RoleInfo update) {
        if (update != null && update.getGUID() != null) {
            ds().update(update);
        }
    }

    /**
     * Deletes a role definition; existing grants referencing it are not
     * cascaded.
     *
     * @param role the role to delete
     * @return {@code true} if the role existed and was removed
     */
    @Override
    public boolean deleteRole(RoleInfo role) {
        return role != null && ds().delete(role, false);
    }

    /**
     * Returns every role definition across all applications (full scan of the
     * role collection).
     *
     * @return all defined roles
     */
    @Override
    public RoleInfo[] getRoles() {
        List<RoleInfo> list = ds().search(RoleInfo.NVC_ROLE_INFO, null);
        return list.toArray(new RoleInfo[0]);
    }

    // ------------------------------------------------------------------
    // role groups
    // ------------------------------------------------------------------

    /**
     * Persists a new role group definition.
     *
     * @param roleGroup the role group to create
     * @return the persisted role group
     */
    @Override
    public RoleGroupInfo createRoleGroup(RoleGroupInfo roleGroup) {
        return ds().insert(roleGroup);
    }

    /**
     * Looks up a role group by name (data-store query on the {@code name}
     * field) and application (case-insensitive app ID comparison).
     *
     * @param appID         the application that owns the role group
     * @param roleGroupName the name of the role group
     * @return the matching role group, or {@code null} if none exists
     */
    @Override
    public RoleGroupInfo lookupRoleGroup(String appID, String roleGroupName) {
        for (RoleGroupInfo g : this.<RoleGroupInfo>byName(RoleGroupInfo.NVC_ROLE_GROUP_INFO, roleGroupName)) {
            if (appIDMatches(g, appID)) {
                return g;
            }
        }
        return null;
    }

    /**
     * Returns every role group belonging to the given application by filtering
     * the full role group catalog in memory.
     *
     * @param appID the application whose role groups are returned
     * @return the role groups belonging to the application
     */
    @Override
    public RoleGroupInfo[] lookupAllRoleGroupsByAppID(String appID) {
        List<RoleGroupInfo> ret = new ArrayList<>();
        for (RoleGroupInfo g : getRoleGroups()) {
            if (appIDMatches(g, appID)) {
                ret.add(g);
            }
        }
        return ret.toArray(new RoleGroupInfo[0]);
    }

    /**
     * Persists changes to an existing role group; silently ignored if the role
     * group is {@code null} or has no GUID.
     *
     * @param update the role group to update
     */
    @Override
    public void updateRoleGroup(RoleGroupInfo update) {
        if (update != null && update.getGUID() != null) {
            ds().update(update);
        }
    }

    /**
     * Deletes a role group definition; existing grants referencing it are not
     * cascaded.
     *
     * @param roleGroup the role group to delete
     * @return {@code true} if the role group existed and was removed
     */
    @Override
    public boolean deleteRoleGroup(RoleGroupInfo roleGroup) {
        return roleGroup != null && ds().delete(roleGroup, false);
    }

    /**
     * Returns every role group definition across all applications (full scan
     * of the role group collection).
     *
     * @return all defined role groups
     */
    @Override
    public RoleGroupInfo[] getRoleGroups() {
        List<RoleGroupInfo> list = ds().search(RoleGroupInfo.NVC_ROLE_GROUP_INFO, null);
        return list.toArray(new RoleGroupInfo[0]);
    }

    /**
     * Queries a collection for all entities whose {@code name} field equals the
     * given name.
     */
    private <V extends NVEntity> List<V> byName(NVConfigEntity nvce, String name) {
        return ds().search(nvce, null, eq(FIELD_NAME, name));
    }

    // ------------------------------------------------------------------
    // grants
    // ------------------------------------------------------------------

    /**
     * Grants a permission to a subject by inserting a new
     * {@link PermissionGrant} that references the permission's GUID and carries
     * the subject's GUID.
     *
     * @param subject        the subject receiving the grant
     * @param permissionInfo the permission to grant
     * @return the persisted grant
     */
    @Override
    public PermissionGrant addPermissionGrant(SubjectIdentifier subject, PermissionInfo permissionInfo) {
        PermissionGrant grant = new PermissionGrant(permissionInfo.getGUID());
        grant.setSubjectGUID(subject.getGUID());
        return ds().insert(grant);
    }

    /**
     * Revokes a previously-issued permission grant.
     *
     * @param permissionGrant the grant to revoke
     * @return {@code true} if the grant existed and was removed
     */
    @Override
    public boolean deletePermissionGrant(PermissionGrant permissionGrant) {
        return permissionGrant != null && ds().delete(permissionGrant, false);
    }

    /**
     * Returns every permission grant whose {@code subject_guid} matches the
     * given subject GUID.
     *
     * @param subjectGUID the GUID of the subject whose grants are returned
     * @return the subject's permission grants
     */
    @Override
    public PermissionGrant[] getPermissionGrants(String subjectGUID) {
        List<PermissionGrant> list = ds().search(PermissionGrant.NVC_PERMISSION_GRANT, null,
                eq(FIELD_SUBJECT_GUID, subjectGUID));
        return list.toArray(new PermissionGrant[0]);
    }

    /**
     * Grants a role to a subject by inserting a new {@link RoleGrant} that
     * references the role's GUID and carries the subject's GUID.
     *
     * @param subject  the subject receiving the grant
     * @param roleInfo the role to grant
     * @return the persisted grant
     */
    @Override
    public RoleGrant addRoleGrant(SubjectIdentifier subject, RoleInfo roleInfo) {
        RoleGrant grant = new RoleGrant(roleInfo.getGUID());
        grant.setSubjectGUID(subject.getGUID());
        return ds().insert(grant);
    }

    /**
     * Revokes a previously-issued role grant.
     *
     * @param roleGrant the grant to revoke
     * @return {@code true} if the grant existed and was removed
     */
    @Override
    public boolean deleteRoleGrant(RoleGrant roleGrant) {
        return roleGrant != null && ds().delete(roleGrant, false);
    }

    /**
     * Returns every role grant whose {@code subject_guid} matches the given
     * subject GUID.
     *
     * @param subjectGUID the GUID of the subject whose grants are returned
     * @return the subject's role grants
     */
    @Override
    public RoleGrant[] getRoleGrants(String subjectGUID) {
        List<RoleGrant> list = ds().search(RoleGrant.NVC_ROLE_GRANT, null,
                eq(FIELD_SUBJECT_GUID, subjectGUID));
        return list.toArray(new RoleGrant[0]);
    }

    /**
     * Grants a role group to a subject by inserting a new
     * {@link RoleGroupGrant} that references the role group's GUID and carries
     * the subject's GUID.
     *
     * @param subject       the subject receiving the grant
     * @param roleGroupInfo the role group to grant
     * @return the persisted grant
     */
    @Override
    public RoleGroupGrant addRoleGroupGrant(SubjectIdentifier subject, RoleGroupInfo roleGroupInfo) {
        RoleGroupGrant grant = new RoleGroupGrant(roleGroupInfo.getGUID());
        grant.setSubjectGUID(subject.getGUID());
        return ds().insert(grant);
    }

    /**
     * Revokes a previously-issued role group grant.
     *
     * @param roleGroupGrant the grant to revoke
     * @return {@code true} if the grant existed and was removed
     */
    @Override
    public boolean deleteRoleGroupGrant(RoleGroupGrant roleGroupGrant) {
        return roleGroupGrant != null && ds().delete(roleGroupGrant, false);
    }

    /**
     * Returns every role group grant whose {@code subject_guid} matches the
     * given subject GUID.
     *
     * @param subjectGUID the GUID of the subject whose grants are returned
     * @return the subject's role group grants
     */
    @Override
    public RoleGroupGrant[] getRoleGroupGrants(String subjectGUID) {
        List<RoleGroupGrant> list = ds().search(RoleGroupGrant.NVC_ROLE_GROUP_GRANT, null,
                eq(FIELD_SUBJECT_GUID, subjectGUID));
        return list.toArray(new RoleGroupGrant[0]);
    }

    // ------------------------------------------------------------------
    // data store
    // ------------------------------------------------------------------

    /**
     * Injects the persistence layer used by every operation of this manager.
     *
     * @param dataStore the data store to back this manager
     * @return this manager, for call chaining
     */
    @Override
    public DomainSecurityManager setDataStore(APIDataStore<?, ?> dataStore) {
        this.dataStore = dataStore;
        return this;
    }

    /**
     * Returns the data store currently backing this manager.
     *
     * @return the backing data store, or {@code null} if none has been set
     */
    @Override
    public APIDataStore<?, ?> getDataStore() {
        return dataStore;
    }

    /**
     * Registers a {@link CredentialInfo} implementation class; its class name
     * identifies the data-store collection scanned by credential lookups and
     * the subject-delete cascade.
     *
     * @param clazz the credential implementation class to register
     * @return this manager, for call chaining
     */
    @Override
    public DomainSecurityManager addCredentialType(Class<? extends CredentialInfo> clazz) {
        credentialCollections.add(clazz);

        return this;
    }

}
