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
            assertEquals(p.getTarget().getName(), parts[0]);
            if (p.getTarget() == Target.RESOURCE) {
                // resource grammar: resource:<res>:<subject>:<verbs> (verb LAST) or the namespace wildcard resource:*
                if (p.getAction() == Action.ALL) {
                    assertEquals(SecurityModel.RESOURCE + ":*", token, p.name());
                } else {
                    assertTrue(SecurityModel.isResourceToken(token), p.name() + " must be a 4-part resource token: " + token);
                    assertEquals("*", parts[1], p.name() + " catalog resource part must be a wildcard");
                    assertEquals("*", parts[2], p.name() + " catalog subject part must be a wildcard");
                    assertEquals(p.getAction().getName(), parts[3], p.name());
                }
                continue;
            }
            assertTrue(actions.contains(parts[1]), p.name() + " action " + parts[1]);
            assertEquals(p.getAction().getName(), parts[1]);
        }
    }

    @Test
    void resourceTokenGrammar() {
        String s = "0199a2b4-1111-7000-8000-000000000001";
        String e = "0199a2b4-2222-7000-8000-000000000002";
        String g = "0199A2B4-3333-7000-8000-000000000003";
        assertEquals("create,read,update,delete,share", SecurityModel.RESOURCE_SELF_VERBS);
        // self permission of S: resource:S:S:create,read,update,delete,share
        assertEquals("resource:" + s + ":" + s + ":create,read,update,delete,share",
                SecurityModel.toResourceToken(s, s, SecurityModel.RESOURCE_SELF_VERBS));
        // a grant of E to G, stored form resource:<verbs> -> composed 4-part token, lower-cased
        assertEquals("resource:" + e + ":" + g.toLowerCase() + ":read,share",
                SecurityModel.toResourceToken(e, g, SecurityModel.toResourceToken("Read", "share")));
        assertEquals("resource:" + e + ":" + g.toLowerCase() + ":read", SecurityModel.toResourceToken(e, g, " READ "));
        assertEquals("resource:*:*:read", SecurityModel.toResourceToken("*", "*", "read"));
        assertThrows(IllegalArgumentException.class, () -> SecurityModel.toResourceToken(e, g, "subject:read"));
        assertThrows(IllegalArgumentException.class, () -> SecurityModel.toResourceToken(e, g, "resource:read:x"));
        assertThrows(IllegalArgumentException.class, () -> SecurityModel.toResourceToken(e, g, "resource:"));
        assertThrows(NullPointerException.class, () -> SecurityModel.toResourceToken(null, g, "read"));
        assertThrows(NullPointerException.class, () -> SecurityModel.toResourceToken(e, " ", "read"));
        assertThrows(NullPointerException.class, () -> SecurityModel.toResourceToken(e, g, null));

        assertTrue(SecurityModel.isResourceToken("resource:" + e + ":" + g + ":read"));
        assertTrue(SecurityModel.isResourceToken("Resource:*:*:read"));
        assertFalse(SecurityModel.isResourceToken("resource:*"));
        assertFalse(SecurityModel.isResourceToken("resource:read"));
        assertFalse(SecurityModel.isResourceToken("resource:" + e + "::read"));
        assertFalse(SecurityModel.isResourceToken("subject:" + e + ":" + g + ":read"));
        assertFalse(SecurityModel.isResourceToken(null));
        assertFalse(SecurityModel.isResourceToken(""));

        // stored form is 2-part and instance-scopable; composed form is not
        assertTrue(SecurityModel.isInstanceScopable(SecurityModel.toResourceToken("read")));
        assertFalse(SecurityModel.isInstanceScopable(SecurityModel.toResourceToken(e, g, "read")));
        // no nventity anywhere in the catalog
        for (Permission p : Permission.values()) assertFalse(p.getValue().contains("nventity"), p.name());
        assertThrows(IllegalArgumentException.class, () -> Target.valueOf("NVENTITY"));
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
        assertEquals("resource:*", Permission.NVE_ALL.getValue());
        assertEquals("resource:*:*:share", Permission.NVE_SHARE_ALL.getValue());
        assertEquals("resource:*:*:create", Permission.NVE_CREATE_ALL.getValue());
        assertEquals(Target.RESOURCE, Permission.NVE_READ_ALL.getTarget());
        assertEquals(Action.READ, Permission.NVE_READ_ALL.getAction());
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
        assertEquals(0, Role.USER.getPermissions().length, "user marks membership, it opens no app (2026-10-20)");
        assertArrayEquals(new Permission[]{Permission.APP_LOGIN}, Role.APP_USER.getPermissions(), "app_user: the login gate only");
        // the app model: registrar = subject:create + app:login; app_admin manages its app's catalog and owns its rows; two platform-only roles
        assertArrayEquals(new Permission[]{Permission.SUBJECT_CREATE, Permission.APP_LOGIN}, Role.APP_REGISTRAR.getPermissions());
        for (Permission p : new Permission[]{Permission.PERMISSION_CREATE, Permission.ROLE_CREATE, Permission.ROLE_ASSIGN, Permission.APP_UPDATE}) {
            assertTrue(java.util.Arrays.asList(Role.APP_ADMIN.getPermissions()).contains(p), p.name());
        }
        assertFalse(java.util.Arrays.asList(Role.APP_ADMIN.getPermissions()).contains(Permission.APP_CREATE));
        for (Role r : Role.values()) {
            assertEquals(r == Role.SUPER_ADMIN || r == Role.DOMAIN_ADMIN, r.isPlatformOnly(), r.name());
        }
        for (RoleGroup g : RoleGroup.values()) {
            assertEquals(g == RoleGroup.DOMAIN_ADMINS, g.isPlatformOnly(), g.name());
        }
        for (Role r : Role.values()) {
            assertThrows(IllegalArgumentException.class, () -> Role.valueOf("RESOURCE"));
        }
        // getPermissions() returns the enum's own array (no defensive copy, user decision 2026-09-18): callers read only
        assertSame(Role.DOMAIN_ADMIN.getPermissions(), Role.DOMAIN_ADMIN.getPermissions());
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
        assertFalse(SecurityModel.isWildcardToken("resource:*"));
        assertFalse(SecurityModel.isWildcardToken("resource:*:*:read"));
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
    void appResourceTokens_catalogForm_checkedForm_filter() {
        // catalog form: app_resource:<fqcn>:<verbs>, lower-cased, de-duplicated, class name validated
        assertEquals("app_resource:com.acme.order:read,update", SecurityModel.toAppResourceCatalogToken("com.acme.Order", "read", "update", "READ"));
        assertEquals("app_resource:*:*", SecurityModel.toAppResourceCatalogToken("*", "*"));
        assertEquals("app_resource:com.acme.order:share", SecurityModel.AppResourcePermissionTokenFilter.SINGLETON.validate(" App_Resource : com.acme.Order : Share "));
        assertEquals(Permission.APP_RESOURCE_ALL.getValue(), SecurityModel.toAppResourceCatalogToken("*", "*"));
        assertEquals(Target.APP_RESOURCE, Permission.APP_RESOURCE_ALL.getTarget());
        assertThrows(IllegalArgumentException.class, () -> SecurityModel.toAppResourceCatalogToken("com.acme.Order", "create"), "creation is judged on the owner");
        assertThrows(IllegalArgumentException.class, () -> SecurityModel.toAppResourceCatalogToken("com.acme.Order", "fly"));
        assertThrows(IllegalArgumentException.class, () -> SecurityModel.toAppResourceCatalogToken("not a class", "read"));
        assertThrows(IllegalArgumentException.class, () -> SecurityModel.toAppResourceCatalogToken("com.acme.Order", ""));
        assertThrows(IllegalArgumentException.class, () -> SecurityModel.toAppResourceCatalogToken("com.acme.Order"));
        assertThrows(IllegalArgumentException.class, () -> SecurityModel.AppResourcePermissionTokenFilter.SINGLETON.validate("resource:read"));
        assertThrows(IllegalArgumentException.class, () -> SecurityModel.AppResourcePermissionTokenFilter.SINGLETON.validate("app_resource:read"));
        assertThrows(IllegalArgumentException.class, () -> SecurityModel.AppResourcePermissionTokenFilter.SINGLETON.validate("app_resource:a:b:read"));
        assertThrows(NullPointerException.class, () -> SecurityModel.AppResourcePermissionTokenFilter.SINGLETON.validate(null));
        assertFalse(SecurityModel.AppResourcePermissionTokenFilter.SINGLETON.isValid("app_resource::read"));
        assertTrue(SecurityModel.isAppResourceCatalogToken("app_resource:com.acme.Order:read"));
        assertFalse(SecurityModel.isAppResourceCatalogToken("app_resource::read"));
        assertFalse(SecurityModel.isAppResourceCatalogToken("resource:read"));
        assertFalse(SecurityModel.isAppResourceCatalogToken(null));
        assertFalse(SecurityModel.isWildcardToken(Permission.APP_RESOURCE_ALL.getValue()), "not the reserved wildcard");
        assertFalse(SecurityModel.isInstanceScopable(Permission.APP_RESOURCE_ALL.getValue()), "not a share token");

        // checked form: app_resource:<app>:<fqcn>:<subject>:<verbs>; the verbs may come from a catalog token
        String app = "a1b2", subject = "s9";
        assertEquals("app_resource:a1b2:com.acme.order:s9:read", SecurityModel.toAppResourceToken(app, "com.acme.Order", subject, "read"));
        assertEquals("app_resource:a1b2:com.acme.order:s9:read,update", SecurityModel.toAppResourceToken(app, "com.acme.Order", subject, "app_resource:com.acme.Order:read,update"));
        assertEquals("app_resource:a1b2:*:s9:*", SecurityModel.toAppResourceToken(app, "*", subject, "app_resource:*:*"));
        assertTrue(SecurityModel.isAppResourceToken("app_resource:a1b2:com.acme.order:s9:read"));
        assertFalse(SecurityModel.isAppResourceToken("app_resource:com.acme.order:read"));
        assertThrows(IllegalArgumentException.class, () -> SecurityModel.toAppResourceToken(app, "x", subject, "resource:read"));
        assertThrows(IllegalArgumentException.class, () -> SecurityModel.toAppResourceToken(app, "x", subject, "a:b"));
        assertThrows(NullPointerException.class, () -> SecurityModel.toAppResourceToken(app, "x", subject, " "));
        assertThrows(NullPointerException.class, () -> SecurityModel.toAppResourceToken(null, "x", subject, "read"));

        // the app admin owns the app's rows through app_resource:*:*; no other starter role carries an app-resource right
        assertTrue(java.util.Arrays.asList(Role.APP_ADMIN.getPermissions()).contains(Permission.APP_RESOURCE_ALL));
        for (Role r : Role.values()) {
            if (r != Role.APP_ADMIN) {
                assertFalse(java.util.Arrays.asList(r.getPermissions()).contains(Permission.APP_RESOURCE_ALL), r.name());
            }
        }
    }

    @Test
    void resourceTypeOf_metaTypeClassName_storeAndControllerAgree() {
        PermissionInfo pi = new PermissionInfo();
        assertEquals(PermissionInfo.class.getName(), SecurityModel.resourceTypeOf(pi));
        assertEquals(PermissionInfo.class.getName(), SecurityModel.resourceTypeOf(PermissionInfo.NVC_PERMISSION_INFO));
        assertEquals(SecurityModel.resourceTypeOf(pi), SecurityModel.resourceTypeOf((org.zoxweb.shared.util.NVConfigEntity) pi.getNVConfig()));
        assertNull(SecurityModel.resourceTypeOf((org.zoxweb.shared.util.NVEntity) null));
        assertNull(SecurityModel.resourceTypeOf((org.zoxweb.shared.util.NVConfigEntity) null));
    }

    @Test
    void toPermissionInfo_isGlobalCatalogRow() {
        PermissionInfo pi = Permission.SUBJECT_CREATE.toPermissionInfo();
        assertEquals("subject_create", pi.getName());
        assertEquals("subject:create", pi.getPermissionToken());
        assertNotNull(pi.getDescription());
        assertNull(pi.getAppID());
        assertNull(pi.getGUID());
    }
}
