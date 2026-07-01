package org.zoxweb.server.security;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.MockAPIDataStore;
import org.zoxweb.shared.crypto.CIPassword;
import org.zoxweb.shared.security.*;

import static org.junit.jupiter.api.Assertions.*;

class DomainSecurityManagerDefaultTest {

    private static final String PRINCIPAL = "alice@example.com";
    private static final String PASSWORD = "Secret123!";

    private DomainSecurityManager newManager() {
        return new DomainSecurityManagerDefault().setDataStore(new MockAPIDataStore());
    }

    @Test
    void getDataStore_returnsConfiguredStore() {
        assertInstanceOf(MockAPIDataStore.class, newManager().getDataStore());
    }

    @Test
    void createSubject_assignsGUID_andIsLookupable() {
        DomainSecurityManager mgr = newManager();
        SubjectIdentifier subject = mgr.createSubjectID(PRINCIPAL, HashUtil.toBCryptPassword(PASSWORD));

        assertNotNull(subject.getGUID());
        SubjectIdentifier looked = mgr.lookupSubjectID(PRINCIPAL);
        assertNotNull(looked);
        assertEquals(subject.getGUID(), looked.getGUID());
    }

    @Test
    void login_succeedsWithCorrectPassword_failsOtherwise() {
        DomainSecurityManager mgr = newManager();
        SubjectIdentifier subject = mgr.createSubjectID(PRINCIPAL, HashUtil.toBCryptPassword(PASSWORD));

        SubjectIdentifier loggedIn = mgr.login(PRINCIPAL, PASSWORD);
        assertEquals(subject.getGUID(), loggedIn.getGUID());

        // case-insensitive principal still resolves
        assertEquals(subject.getGUID(), mgr.login("ALICE@example.com", PASSWORD).getGUID());

        assertThrows(SecurityException.class, () -> mgr.login(PRINCIPAL, "wrong-password"));
        assertThrows(SecurityException.class, () -> mgr.login("nobody@example.com", PASSWORD));
    }

    @Test
    void lookupCredential_returnsStoredPassword() {
        DomainSecurityManager mgr = newManager();
        mgr.createSubjectID(PRINCIPAL, HashUtil.toBCryptPassword(PASSWORD));

        CredentialInfo ci = mgr.lookupCredential(PRINCIPAL, CredentialInfo.Type.PASSWORD);
        assertInstanceOf(CIPassword.class, ci);
        assertEquals(1, mgr.lookupAllPrincipalCredentials(PRINCIPAL).length);
    }

    @Test
    void permissionGrant_roundTripsThroughDataStore() {
        DomainSecurityManager mgr = newManager();
        SubjectIdentifier subject = mgr.createSubjectID(PRINCIPAL, HashUtil.toBCryptPassword(PASSWORD));

        PermissionInfo perm = mgr.createPermission(new PermissionInfo("perm.read", "system:read"));
        assertNotNull(perm.getGUID());
        assertEquals(perm.getGUID(), mgr.lookupPermission(null, "perm.read").getGUID());

        PermissionGrant grant = mgr.addPermissionGrant(subject, perm);
        assertEquals(perm.getGUID(), grant.getPermissionGUID());

        PermissionGrant[] grants = mgr.getPermissionGrants(subject.getGUID());
        assertEquals(1, grants.length);
        assertEquals(grant.getGUID(), grants[0].getGUID());

        assertTrue(mgr.deletePermissionGrant(grant));
        assertEquals(0, mgr.getPermissionGrants(subject.getGUID()).length);
    }

    @Test
    void deleteSubject_cascadesPrincipalsCredentialsAndGrants() {
        DomainSecurityManager mgr = newManager();
        SubjectIdentifier subject = mgr.createSubjectID(PRINCIPAL, HashUtil.toBCryptPassword(PASSWORD));
        PermissionInfo perm = mgr.createPermission(new PermissionInfo("perm.read", "system:read"));
        mgr.addPermissionGrant(subject, perm);

        assertTrue(mgr.deleteSubjectID(subject));

        assertNull(mgr.lookupSubjectID(PRINCIPAL));
        assertNull(mgr.lookupPrincipalID(PRINCIPAL));
        assertEquals(0, mgr.lookupAllPrincipalCredentials(PRINCIPAL).length);
        assertEquals(0, mgr.getPermissionGrants(subject.getGUID()).length);
    }
}
