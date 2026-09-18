package org.zoxweb.shared.security;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.security.model.SecurityModel;
import org.zoxweb.shared.security.model.SecurityModel.Action;
import org.zoxweb.shared.security.model.SecurityModel.Permission;
import org.zoxweb.shared.security.model.SecurityModel.Role;
import org.zoxweb.shared.security.model.SecurityModel.RoleGroup;
import org.zoxweb.shared.security.model.SecurityModel.Target;
import org.zoxweb.shared.util.SUS;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the shape of the permission catalog: token grammar, uniqueness, the single reserved
 * wildcard entry, the {@code PERM_*} constants the rest of the stack checks, and the role /
 * role-group closures the seeder materialises.
 */
class SecurityModelTest {

    @Test
    void everyPermissionTokenIsWellFormed() {
        Set<String> targets = new HashSet<>();
        for (Target t : Target.values()) targets.add(t.getName());
        Set<String> actions = new HashSet<>();
        for (Action a : Action.values()) actions.add(a.getName());

        for (Permission p : Permission.values()) {
            String token = p.getValue();
            assertNotNull(token, p.name());
            assertEquals(token, token.toLowerCase(), p.name() + " must be lower-case");
            assertFalse(token.contains(" "), p.name() + " must not contain whitespace");
            assertFalse(token.contains("$$"), p.name() + " must not carry a placeholder");
            assertFalse(p.getName().contains(" "));
            assertNotNull(p.getDescription(), p.name());
            if (p.isReserved()) {
                assertEquals(SecurityModel.WILDCARD, token);
                assertNull(p.getTarget());
                assertNull(p.getAction());
                continue;
            }
            String[] parts = token.split(SecurityModel.PART_SEP, -1);
            assertTrue(parts.length >= 2, p.name());
            for (String part : parts) assertFalse(part.isEmpty(), p.name() + " has an empty part");
            assertTrue(targets.contains(parts[0]), p.name() + " target " + parts[0]);
            assertTrue(actions.contains(parts[1]), p.name() + " action " + parts[1]);
            assertEquals(p.getTarget().getName(), parts[0]);
            assertEquals(p.getAction().getName(), parts[1]);
        }
    }

    @Test
    void noDuplicatePermissionNamesOrTokens() {
        Set<String> names = new HashSet<>();
        Set<String> tokens = new HashSet<>();
        for (Permission p : Permission.values()) {
            assertTrue(names.add(p.getName()), "duplicate name " + p.getName());
            assertTrue(tokens.add(p.getValue()), "duplicate token " + p.getValue());
        }
    }

    @Test
    void onlyReservedEntryCarriesWildcard() {
        for (Permission p : Permission.values()) {
            assertEquals(p == Permission.SUPER_ADMIN_ALL, SecurityModel.isWildcardToken(p.getValue()), p.name());
            assertEquals(p == Permission.SUPER_ADMIN_ALL, p.isReserved(), p.name());
        }
    }

    @Test
    void permConstantsMatchEnum() {
        assertEquals(SecurityModel.PERM_ADD_SUBJECT, Permission.SUBJECT_CREATE.getValue());
        assertEquals(SecurityModel.PERM_READ_SUBJECT, Permission.SUBJECT_READ.getValue());
        assertEquals(SecurityModel.PERM_UPDATE_SUBJECT, Permission.SUBJECT_UPDATE.getValue());
        assertEquals(SecurityModel.PERM_DELETE_SUBJECT, Permission.SUBJECT_DELETE.getValue());
        assertEquals(SecurityModel.PERM_ADD_PERMISSION, Permission.PERMISSION_CREATE.getValue());
        assertEquals(SecurityModel.PERM_UPDATE_PERMISSION, Permission.PERMISSION_UPDATE.getValue());
        assertEquals(SecurityModel.PERM_DELETE_PERMISSION, Permission.PERMISSION_DELETE.getValue());
        assertEquals(SecurityModel.PERM_ASSIGN_PERMISSION, Permission.PERMISSION_ASSIGN.getValue());
        assertEquals(SecurityModel.PERM_REMOVE_PERMISSION, Permission.PERMISSION_REMOVE.getValue());
        assertEquals(SecurityModel.PERM_ADD_ROLE, Permission.ROLE_CREATE.getValue());
        assertEquals(SecurityModel.PERM_UPDATE_ROLE, Permission.ROLE_UPDATE.getValue());
        assertEquals(SecurityModel.PERM_DELETE_ROLE, Permission.ROLE_DELETE.getValue());
        assertEquals(SecurityModel.PERM_ASSIGN_ROLE, Permission.ROLE_ASSIGN.getValue());
        assertEquals(SecurityModel.PERM_REMOVE_ROLE, Permission.ROLE_REMOVE.getValue());
        assertEquals(SecurityModel.PERM_CREATE_APP_ID, Permission.APP_CREATE.getValue());
        assertEquals(SecurityModel.PERM_UPDATE_APP_ID, Permission.APP_UPDATE.getValue());
        assertEquals(SecurityModel.PERM_DELETE_APP_ID, Permission.APP_DELETE.getValue());
        assertEquals("nventity:*", Permission.NVE_ALL.getValue());
        assertEquals("nventity:share:*", Permission.NVE_SHARE_ALL.getValue());
        assertEquals("permission:assign:permission", Permission.PERMISSION_ASSIGN.getValue());
        assertEquals("permission:assign:role", Permission.ROLE_ASSIGN.getValue());
    }

    @Test
    void roleClosure() {
        for (Role r : Role.values()) {
            Set<Permission> seen = new HashSet<>();
            for (Permission p : r.getPermissions()) {
                assertNotNull(p, r.name());
                assertTrue(seen.add(p), r.name() + " lists " + p.name() + " twice");
                if (r != Role.SUPER_ADMIN) {
                    assertFalse(p.isReserved(), r.name() + " must not carry the reserved permission");
                }
            }
            assertEquals(r == Role.SUPER_ADMIN, r.isReserved());
        }
        assertArrayEquals(new Permission[]{Permission.SUPER_ADMIN_ALL}, Role.SUPER_ADMIN.getPermissions());
        assertEquals(0, Role.USER.getPermissions().length);
        assertEquals(0, Role.APP_USER.getPermissions().length);
        for (Role r : Role.values()) {
            assertThrows(IllegalArgumentException.class, () -> Role.valueOf("RESOURCE"));
        }
        // defensive copies
        Permission[] copy = Role.DOMAIN_ADMIN.getPermissions();
        copy[0] = Permission.SUPER_ADMIN_ALL;
        assertNotEquals(Permission.SUPER_ADMIN_ALL, Role.DOMAIN_ADMIN.getPermissions()[0]);
    }

    @Test
    void roleGroupClosure() {
        Set<String> names = new HashSet<>();
        for (RoleGroup g : RoleGroup.values()) {
            assertTrue(names.add(g.getName()));
            assertTrue(g.getRoles().length > 0, g.name());
            Set<Role> seen = new HashSet<>();
            for (Role r : g.getRoles()) {
                assertTrue(seen.add(r), g.name() + " lists " + r.name() + " twice");
                assertNotEquals(Role.SUPER_ADMIN, r, g.name() + " must not contain super_admin");
            }
        }
    }

    @Test
    void isWildcardTokenCases() {
        assertTrue(SecurityModel.isWildcardToken("*"));
        assertTrue(SecurityModel.isWildcardToken(" * "));
        assertTrue(SecurityModel.isWildcardToken("*:read"));
        assertTrue(SecurityModel.isWildcardToken("*:*:*"));
        assertFalse(SecurityModel.isWildcardToken("a:*"));
        assertFalse(SecurityModel.isWildcardToken("nventity:*"));
        assertFalse(SecurityModel.isWildcardToken("nventity:read:*"));
        assertFalse(SecurityModel.isWildcardToken(""));
        assertFalse(SecurityModel.isWildcardToken("   "));
        assertFalse(SecurityModel.isWildcardToken(null));
    }

    @Test
    void descriptionsPresent_andSubjectReadSaysRead() {
        assertTrue(Permission.SUBJECT_READ.getDescription().toLowerCase().contains("read"));
        for (Permission p : Permission.values()) assertFalse(SUS.isEmpty(p.getDescription()), p.name());
        for (Role r : Role.values()) assertFalse(SUS.isEmpty(r.getDescription()), r.name());
        for (RoleGroup g : RoleGroup.values()) assertFalse(SUS.isEmpty(g.getDescription()), g.name());
    }

    @Test
    void toPermissionInfo_isGlobalCatalogRow() {
        PermissionInfo pi = Permission.SUBJECT_CREATE.toPermissionInfo();
        assertEquals("subject_create", pi.getName());
        assertEquals("subject:create", pi.getPermissionToken());
        assertNotNull(pi.getDescription());
        assertNull(pi.getAppIdDAO());
        assertNull(pi.getGUID());
    }
}
