package org.zoxweb.server.security;

import org.zoxweb.server.util.MockAPIDataStore;
import org.zoxweb.server.util.UUID7;
import org.zoxweb.shared.api.APIDataStore;
import org.zoxweb.shared.crypto.CIPassword;
import org.zoxweb.shared.db.QueryMatch;
import org.zoxweb.shared.security.*;
import org.zoxweb.shared.util.Const.RelationalOperator;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

import java.util.*;

/**
 * An in-memory {@link DomainSecurityManager} for unit tests that delegates all
 * persistence to an {@link APIDataStore} - by default a {@link MockAPIDataStore}.
 *
 * <p>Every entity (subjects, principals, credentials, the permission/role/role-group
 * catalog, and grants) is stored and retrieved through the data store; this class holds
 * no entity state of its own beyond a small registry of which collections hold
 * credentials. Linkage is by GUID exactly as in production: principals and grants carry
 * a {@code subject_guid}, and lookups resolve a principal to its owning subject, then
 * fetch related rows by GUID/field query.</p>
 *
 * <p>{@link #login(String, String)} resolves the principal to its owning subject,
 * loads that subject's {@link CredentialInfo.Type#PASSWORD} credential, and validates it
 * with {@link SecUtil#isPasswordValid(CIPassword, String)} - so the stored credential
 * must be a hashed {@link CIPassword}, exactly as in production.</p>
 */
public class DomainSecurityManagerDefault
        implements DomainSecurityManager {

    private static final String FIELD_SUBJECT_GUID = "subject_guid";
    private static final String FIELD_NAME = "name";

    // NVConfigEntity collections, by name, that currently hold credentials
    private final Set<Class<?>> credentialCollections = new HashSet<>();

    private volatile APIDataStore<?, ?> dataStore;

    public DomainSecurityManagerDefault() {
    }


    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private APIDataStore<?, ?> ds() {
        if (dataStore == null) {
            throw new IllegalStateException("No data store set");
        }
        return dataStore;
    }

    private static <V> V first(List<V> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    private static QueryMatch<String> eq(String field, String value) {
        return new QueryMatch<>(field, value, RelationalOperator.EQUAL);
    }

    private static boolean appIDMatches(AuthzInfo info, String appID) {
        String owned = info.getAppIdDAO() != null ? info.getAppIdDAO().getAppID() : null;
        return SharedStringUtil.equals(owned, appID, true);
    }

    private PrincipalIdentifier resolvePrincipal(String principalID) {
        return first(ds().search(PrincipalIdentifier.NVC_PRINCIPAL_IDENTIFIER, null,
                new QueryMatch<>(RelationalOperator.EQUAL, principalID, PrincipalIdentifier.Param.PRINCIPAL_ID)));
    }

    private String resolveSubjectGUID(String principalID) {
        PrincipalIdentifier principal = resolvePrincipal(principalID);
        return principal != null ? principal.getSubjectGUID() : null;
    }

    // ------------------------------------------------------------------
    // login
    // ------------------------------------------------------------------

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

    @Override
    public SubjectIdentifier loginApiKey(String key) throws SecurityException {
        if (key == null) {
            throw new SecurityException("Invalid key");
        }

        SubjectAPIKey sak = first(ds().search(SubjectAPIKey.NVC_SUBJECT_API_KEY, null, eq(SubjectAPIKey.Param.API_KEY.getNVConfig().getName(), key)));
        if (sak == null) {
            throw new SecurityException("Invalid key");
        }

        SubjectIdentifier subject = lookupSubjectID(sak.getSubjectID());
        if (subject == null) {
            throw new SecurityException("Invalid key");
        }

        return subject;
    }

    // ------------------------------------------------------------------
    // subject identifier
    // ------------------------------------------------------------------

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

    @Override
    public SubjectIdentifier lookupSubjectID(String principalID) {
        String subjectGUID = resolveSubjectGUID(principalID);
        if (subjectGUID == null) {
            return null;
        }
        return first(ds().searchByID(SubjectIdentifier.NVC_SUBJECT_IDENTIFIER, subjectGUID));
    }

    @Override
    public void updateSubjectID(SubjectIdentifier update) {
        if (update != null && update.getGUID() != null) {
            ds().update(update);
        }
    }

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

    @Override
    public CredentialInfo lookupCredential(String principalID, CredentialInfo.Type type) {
        String subjectGUID = resolveSubjectGUID(principalID);
        if (subjectGUID == null) {
            return null;
        }
        for (CredentialInfo ci : lookupAllPrincipalCredentials(subjectGUID, principalID)) {
            if (ci.getCredentialType() == type) {
                return ci;
            }
        }
        return null;
    }

    @Override
    public void updateCredential(CredentialInfo update) {
        if (update instanceof NVEntity && ((NVEntity) update).getGUID() != null) {
            ds().update((NVEntity) update);
        }
    }

    @Override
    public void deleteCredential(CredentialInfo credential) {
        if (credential instanceof NVEntity) {
            ds().delete((NVEntity) credential, false);
        }
    }

    @Override
    public CredentialInfo[] lookupAllPrincipalCredentials(String principalID) {
        return lookupAllPrincipalCredentials(resolveSubjectGUID(principalID), principalID)
                .toArray(new CredentialInfo[0]);
    }

    private List<CredentialInfo> lookupAllPrincipalCredentials(String subjectGUID, String principalID) {
        List<CredentialInfo> ret = new ArrayList<>();
        if (subjectGUID == null) {
            return ret;
        }
        for (Class<?> credColl : credentialCollections.toArray(new Class[0])) {
            for (NVEntity nve : ds().search(credColl.getName(), null, eq(FIELD_SUBJECT_GUID, subjectGUID))) {
                if (nve instanceof CredentialInfo) {
                    ret.add((CredentialInfo) nve);
                }
            }
        }
        return ret;
    }

    // ------------------------------------------------------------------
    // principal identifier
    // ------------------------------------------------------------------

    @Override
    public PrincipalIdentifier addPrincipalID(SubjectIdentifier subject, String principalID) {
        PrincipalIdentifier principal = new PrincipalIdentifier(principalID);
        principal.setSubjectGUID(subject.getGUID());
        return ds().insert(principal);
    }

    @Override
    public PrincipalIdentifier lookupPrincipalID(String principalID) {
        return resolvePrincipal(principalID);
    }

    @Override
    public boolean deletePrincipalID(PrincipalIdentifier principal) {
        return principal != null && ds().delete(principal, false);
    }

    @Override
    public PrincipalIdentifier[] lookupAllPrincipalIdentifiers(String subjectGUID) {
        List<PrincipalIdentifier> list = ds().search(PrincipalIdentifier.NVC_PRINCIPAL_IDENTIFIER, null,
                eq(FIELD_SUBJECT_GUID, subjectGUID));
        return list.toArray(new PrincipalIdentifier[0]);
    }

    // ------------------------------------------------------------------
    // permissions
    // ------------------------------------------------------------------

    @Override
    public PermissionInfo createPermission(PermissionInfo permission) {
        return ds().insert(permission);
    }

    @Override
    public PermissionInfo lookupPermission(String appID, String permissionName) {
        for (PermissionInfo p : this.<PermissionInfo>byName(PermissionInfo.NVC_PERMISSION_INFO, permissionName)) {
            if (appIDMatches(p, appID)) {
                return p;
            }
        }
        return null;
    }

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

    @Override
    public void updatePermission(PermissionInfo update) {
        if (update != null && update.getGUID() != null) {
            ds().update(update);
        }
    }

    @Override
    public boolean deletePermission(PermissionInfo permission) {
        return permission != null && ds().delete(permission, false);
    }

    @Override
    public PermissionInfo[] getPermissions() {
        List<PermissionInfo> list = ds().search(PermissionInfo.NVC_PERMISSION_INFO, null);
        return list.toArray(new PermissionInfo[0]);
    }

    // ------------------------------------------------------------------
    // roles
    // ------------------------------------------------------------------

    @Override
    public RoleInfo createRole(RoleInfo role) {
        return ds().insert(role);
    }

    @Override
    public RoleInfo lookupRole(String appID, String roleName) {
        for (RoleInfo r : this.<RoleInfo>byName(RoleInfo.NVC_ROLE_INFO, roleName)) {
            if (appIDMatches(r, appID)) {
                return r;
            }
        }
        return null;
    }

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

    @Override
    public void updateRole(RoleInfo update) {
        if (update != null && update.getGUID() != null) {
            ds().update(update);
        }
    }

    @Override
    public boolean deleteRole(RoleInfo role) {
        return role != null && ds().delete(role, false);
    }

    @Override
    public RoleInfo[] getRoles() {
        List<RoleInfo> list = ds().search(RoleInfo.NVC_ROLE_INFO, null);
        return list.toArray(new RoleInfo[0]);
    }

    // ------------------------------------------------------------------
    // role groups
    // ------------------------------------------------------------------

    @Override
    public RoleGroupInfo createRoleGroup(RoleGroupInfo roleGroup) {
        return ds().insert(roleGroup);
    }

    @Override
    public RoleGroupInfo lookupRoleGroup(String appID, String roleGroupName) {
        for (RoleGroupInfo g : this.<RoleGroupInfo>byName(RoleGroupInfo.NVC_ROLE_GROUP_INFO, roleGroupName)) {
            if (appIDMatches(g, appID)) {
                return g;
            }
        }
        return null;
    }

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

    @Override
    public void updateRoleGroup(RoleGroupInfo update) {
        if (update != null && update.getGUID() != null) {
            ds().update(update);
        }
    }

    @Override
    public boolean deleteRoleGroup(RoleGroupInfo roleGroup) {
        return roleGroup != null && ds().delete(roleGroup, false);
    }

    @Override
    public RoleGroupInfo[] getRoleGroups() {
        List<RoleGroupInfo> list = ds().search(RoleGroupInfo.NVC_ROLE_GROUP_INFO, null);
        return list.toArray(new RoleGroupInfo[0]);
    }

    private <V extends NVEntity> List<V> byName(NVConfigEntity nvce, String name) {
        return ds().search(nvce, null, eq(FIELD_NAME, name));
    }

    // ------------------------------------------------------------------
    // grants
    // ------------------------------------------------------------------

    @Override
    public PermissionGrant addPermissionGrant(SubjectIdentifier subject, PermissionInfo permissionInfo) {
        PermissionGrant grant = new PermissionGrant(permissionInfo.getGUID());
        grant.setSubjectGUID(subject.getGUID());
        return ds().insert(grant);
    }

    @Override
    public boolean deletePermissionGrant(PermissionGrant permissionGrant) {
        return permissionGrant != null && ds().delete(permissionGrant, false);
    }

    @Override
    public PermissionGrant[] getPermissionGrants(String subjectGUID) {
        List<PermissionGrant> list = ds().search(PermissionGrant.NVC_PERMISSION_GRANT, null,
                eq(FIELD_SUBJECT_GUID, subjectGUID));
        return list.toArray(new PermissionGrant[0]);
    }

    @Override
    public RoleGrant addRoleGrant(SubjectIdentifier subject, RoleInfo roleInfo) {
        RoleGrant grant = new RoleGrant(roleInfo.getGUID());
        grant.setSubjectGUID(subject.getGUID());
        return ds().insert(grant);
    }

    @Override
    public boolean deleteRoleGrant(RoleGrant roleGrant) {
        return roleGrant != null && ds().delete(roleGrant, false);
    }

    @Override
    public RoleGrant[] getRoleGrants(String subjectGUID) {
        List<RoleGrant> list = ds().search(RoleGrant.NVC_ROLE_GRANT, null,
                eq(FIELD_SUBJECT_GUID, subjectGUID));
        return list.toArray(new RoleGrant[0]);
    }

    @Override
    public RoleGroupGrant addRoleGroupGrant(SubjectIdentifier subject, RoleGroupInfo roleGroupInfo) {
        RoleGroupGrant grant = new RoleGroupGrant(roleGroupInfo.getGUID());
        grant.setSubjectGUID(subject.getGUID());
        return ds().insert(grant);
    }

    @Override
    public boolean deleteRoleGroupGrant(RoleGroupGrant roleGroupGrant) {
        return roleGroupGrant != null && ds().delete(roleGroupGrant, false);
    }

    @Override
    public RoleGroupGrant[] getRoleGroupGrants(String subjectGUID) {
        List<RoleGroupGrant> list = ds().search(RoleGroupGrant.NVC_ROLE_GROUP_GRANT, null,
                eq(FIELD_SUBJECT_GUID, subjectGUID));
        return list.toArray(new RoleGroupGrant[0]);
    }

    // ------------------------------------------------------------------
    // data store
    // ------------------------------------------------------------------

    @Override
    public DomainSecurityManager setDataStore(APIDataStore<?, ?> dataStore) {
        this.dataStore = dataStore;
        return this;
    }

    @Override
    public APIDataStore<?, ?> getDataStore() {
        return dataStore;
    }

    @Override
    public DomainSecurityManager addCredentialType(Class<? extends CredentialInfo> clazz) {
        credentialCollections.add(clazz);

        return this;
    }

}
