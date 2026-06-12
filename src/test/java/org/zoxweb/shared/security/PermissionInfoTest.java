package org.zoxweb.shared.security;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionInfoTest {

    // ============================================================
    //                       PermissionInfo
    // ============================================================

    @Test
    void permissionInfo_defaultConstructor_tokenIsNull() {
        PermissionInfo p = new PermissionInfo();
        assertNull(p.getPermissionToken());
    }

    @Test
    void permissionInfo_singleArgConstructor_setsToken() {
        PermissionInfo p = new PermissionInfo("read:files");
        assertEquals("read:files", p.getPermissionToken());
    }

    @Test
    void permissionInfo_setAndGetToken() {
        PermissionInfo p = new PermissionInfo();
        p.setPermissionToken("write:files");
        assertEquals("write:files", p.getPermissionToken());
    }

    @Test
    void permissionInfo_setTokenOverwritesPrevious() {
        PermissionInfo p = new PermissionInfo("first");
        p.setPermissionToken("second");
        assertEquals("second", p.getPermissionToken());
    }

    @Test
    void permissionInfo_setTokenToNullClears() {
        PermissionInfo p = new PermissionInfo("read:files");
        p.setPermissionToken(null);
        assertNull(p.getPermissionToken());
    }

    @Test
    void permissionInfo_isAuthzInfo() {
        assertInstanceOf(AuthzInfo.class, new PermissionInfo());
    }

    @Test
    void permissionInfo_inheritsAuthzInfoFields() {
        PermissionInfo p = new PermissionInfo("read:files");
        p.setBrokerGUID("broker-1");
        p.setName("perm.read");
        p.setDescription("Read permission");

        assertEquals("broker-1", p.getBrokerGUID());
        assertEquals("perm.read", p.getName());
        assertEquals("Read permission", p.getDescription());
        assertEquals("read:files", p.getPermissionToken());
    }

    // ============================================================
    //                          RoleInfo
    // ============================================================

    @Test
    void roleInfo_defaultConstructor_permissionsIsEmpty() {
        RoleInfo r = new RoleInfo();
        PermissionInfo[] perms = r.getPermissionGUIDS();
        assertNotNull(perms);
        assertEquals(0, perms.length);
    }

    @Test
    void roleInfo_varargConstructor_setsPermissions() {
        PermissionInfo p1 = permission("perm-1", "system:read");
        PermissionInfo p2 = permission("perm-2", "system:write");

        RoleInfo r = new RoleInfo(p1, p2);

        assertArrayEquals(new PermissionInfo[]{p1, p2}, r.getPermissionGUIDS());
    }

    @Test
    void roleInfo_setAndGetPermissions() {
        RoleInfo r = new RoleInfo();
        PermissionInfo p1 = permission("perm-1", "a:1");
        PermissionInfo p2 = permission("perm-2", "b:2");
        PermissionInfo p3 = permission("perm-3", "c:3");

        r.setPermissionGUIDS(p1, p2, p3);

        assertArrayEquals(new PermissionInfo[]{p1, p2, p3}, r.getPermissionGUIDS());
    }

    @Test
    void roleInfo_setPermissionsAppendsToExisting() {
        // setPermissionGUIDS adds to the backing ArrayValues rather than
        // replacing it — both batches must be present afterward.
        PermissionInfo p1 = permission("perm-1", "old:1");
        PermissionInfo p2 = permission("perm-2", "old:2");
        PermissionInfo p3 = permission("perm-3", "new:1");

        RoleInfo r = new RoleInfo(p1, p2);
        r.setPermissionGUIDS(p3);

        assertArrayEquals(new PermissionInfo[]{p1, p2, p3}, r.getPermissionGUIDS());
    }

    @Test
    void roleInfo_addPermission_appendsToList() {
        RoleInfo r = new RoleInfo();
        PermissionInfo p1 = permission("perm-1", "system:read");
        PermissionInfo p2 = permission("perm-2", "system:write");

        r.addPermissionGUID(p1);
        r.addPermissionGUID(p2);

        assertArrayEquals(new PermissionInfo[]{p1, p2}, r.getPermissionGUIDS());
    }

    @Test
    void roleInfo_addPermission_dedupesByName() {
        // The backing ArrayValues is a GET_NAME_MAP, so entries dedupe
        // by NVEntity name — not by reference or by GUID.
        RoleInfo r = new RoleInfo();
        PermissionInfo p1 = permission("perm-1", "system:read");
        PermissionInfo p1Dup = permission("perm-1-other-guid", "system:read");
        p1Dup.setName(p1.getName());

        r.addPermissionGUID(p1);
        r.addPermissionGUID(p1);
        r.addPermissionGUID(p1Dup);

        PermissionInfo[] perms = r.getPermissionGUIDS();
        assertEquals(1, perms.length);
    }

    @Test
    void roleInfo_removePermission_removesWhenPresent() {
        PermissionInfo p1 = permission("perm-1", "system:read");
        PermissionInfo p2 = permission("perm-2", "system:write");
        RoleInfo r = new RoleInfo(p1, p2);

        r.removePermissionGUID(p1);

        assertArrayEquals(new PermissionInfo[]{p2}, r.getPermissionGUIDS());
    }

    @Test
    void roleInfo_removePermission_noopWhenAbsent() {
        PermissionInfo p1 = permission("perm-1", "system:read");
        PermissionInfo missing = permission("perm-missing", "does:not:exist");
        RoleInfo r = new RoleInfo();
        r.addPermissionGUID(p1);

        r.removePermissionGUID(missing);

        assertArrayEquals(new PermissionInfo[]{p1}, r.getPermissionGUIDS());
    }

    @Test
    void roleInfo_isAuthzInfo() {
        assertInstanceOf(AuthzInfo.class, new RoleInfo());
    }

    @Test
    void roleInfo_inheritsAuthzInfoFields() {
        RoleInfo r = new RoleInfo();
        PermissionInfo p1 = permission("perm-1", "system:read");
        r.setBrokerGUID("broker-1");
        r.setName("role.admin");
        r.setDescription("Administrator role");
        r.addPermissionGUID(p1);

        assertEquals("broker-1", r.getBrokerGUID());
        assertEquals("role.admin", r.getName());
        assertEquals("Administrator role", r.getDescription());
        assertArrayEquals(new PermissionInfo[]{p1}, r.getPermissionGUIDS());
    }

    // ============================================================
    //                        RoleGroupInfo
    // ============================================================

    @Test
    void roleGroupInfo_defaultConstructor_rolesIsEmpty() {
        RoleGroupInfo g = new RoleGroupInfo();
        RoleInfo[] roles = g.getRoleGUIDS();
        assertNotNull(roles);
        assertEquals(0, roles.length);
    }

    @Test
    void roleGroupInfo_varargConstructor_setsRoles() {
        RoleInfo r1 = role("role-1", "role.r1");
        RoleInfo r2 = role("role-2", "role.r2");

        RoleGroupInfo g = new RoleGroupInfo(r1, r2);

        assertArrayEquals(new RoleInfo[]{r1, r2}, g.getRoleGUIDS());
    }

    @Test
    void roleGroupInfo_setAndGetRoles() {
        RoleInfo r1 = role("role-1", "role.r1");
        RoleInfo r2 = role("role-2", "role.r2");
        RoleGroupInfo g = new RoleGroupInfo();

        g.setRoleGUIDS(r1, r2);

        assertArrayEquals(new RoleInfo[]{r1, r2}, g.getRoleGUIDS());
    }

    @Test
    void roleGroupInfo_setRolesAppendsToExisting() {
        // Mirrors RoleInfo: setRoleGUIDS appends rather than replacing.
        RoleInfo r1 = role("role-1", "role.r1");
        RoleInfo r2 = role("role-2", "role.r2");
        RoleInfo r3 = role("role-3", "role.r3");
        RoleGroupInfo g = new RoleGroupInfo(r1, r2);

        g.setRoleGUIDS(r3);

        assertArrayEquals(new RoleInfo[]{r1, r2, r3}, g.getRoleGUIDS());
    }

    @Test
    void roleGroupInfo_addRole_appendsToList() {
        RoleInfo r1 = role("role-1", "role.r1");
        RoleInfo r2 = role("role-2", "role.r2");
        RoleGroupInfo g = new RoleGroupInfo();

        g.addRoleGUID(r1);
        g.addRoleGUID(r2);

        assertArrayEquals(new RoleInfo[]{r1, r2}, g.getRoleGUIDS());
    }

    @Test
    void roleGroupInfo_addRole_dedupesByName() {
        RoleInfo r1 = role("role-1", "role.r1");
        RoleInfo r1Dup = role("role-1-other-guid", "role.r1");
        RoleGroupInfo g = new RoleGroupInfo();

        g.addRoleGUID(r1);
        g.addRoleGUID(r1);
        g.addRoleGUID(r1Dup);

        assertEquals(1, g.getRoleGUIDS().length);
    }

    @Test
    void roleGroupInfo_removeRole_removesWhenPresent() {
        RoleInfo r1 = role("role-1", "role.r1");
        RoleInfo r2 = role("role-2", "role.r2");
        RoleGroupInfo g = new RoleGroupInfo(r1, r2);

        g.removeRoleGUID(r2);

        assertArrayEquals(new RoleInfo[]{r1}, g.getRoleGUIDS());
    }

    @Test
    void roleGroupInfo_removeRole_noopWhenAbsent() {
        RoleInfo r1 = role("role-1", "role.r1");
        RoleInfo missing = role("role-missing", "role.missing");
        RoleGroupInfo g = new RoleGroupInfo();
        g.addRoleGUID(r1);

        g.removeRoleGUID(missing);

        assertArrayEquals(new RoleInfo[]{r1}, g.getRoleGUIDS());
    }

    @Test
    void roleGroupInfo_isAuthzInfo() {
        assertInstanceOf(AuthzInfo.class, new RoleGroupInfo());
    }

    @Test
    void roleGroupInfo_flatCollection_noNesting() {
        // RoleGroupInfo holds only RoleInfo references — no RoleGroupInfo nesting.
        RoleInfo a = role("role-a", "role.a");
        RoleInfo b = role("role-b", "role.b");
        RoleInfo c = role("role-c", "role.c");
        RoleGroupInfo g = new RoleGroupInfo();
        g.addRoleGUID(a);
        g.addRoleGUID(b);
        g.addRoleGUID(c);

        RoleInfo[] roles = g.getRoleGUIDS();
        assertEquals(3, roles.length);
        assertArrayEquals(new RoleInfo[]{a, b, c}, roles);
    }

    @Test
    void roleGroupInfo_inheritsAuthzInfoFields() {
        RoleInfo r1 = role("role-1", "role.r1");
        RoleGroupInfo g = new RoleGroupInfo();
        g.setBrokerGUID("broker-1");
        g.setName("group.engineers");
        g.setDescription("Engineering role group");
        g.addRoleGUID(r1);

        assertEquals("broker-1", g.getBrokerGUID());
        assertEquals("group.engineers", g.getName());
        assertEquals("Engineering role group", g.getDescription());
        assertArrayEquals(new RoleInfo[]{r1}, g.getRoleGUIDS());
    }

    // ============================================================
    //         Connectivity: Permission -> Role -> RoleGroup
    // ============================================================
    //
    // After the refactor, RoleInfo holds PermissionInfo references directly
    // and RoleGroupInfo holds RoleInfo references directly — there is no
    // GUID-indirection lookup step. These tests verify that the object
    // graph composes end-to-end.

    private static PermissionInfo permission(String guid, String token) {
        PermissionInfo p = new PermissionInfo(token);
        p.setGUID(guid);
        p.setName("perm." + token);
        return p;
    }

    private static RoleInfo role(String guid, String name, PermissionInfo... perms) {
        RoleInfo r = new RoleInfo(perms);
        r.setGUID(guid);
        r.setName(name);
        return r;
    }

    @Test
    void connectivity_roleHoldsPermissionReferences() {
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionInfo sysWrite = permission("perm-sys-write", "system:write");

        RoleInfo admin = role("role-admin", "role.admin", sysRead, sysWrite);

        assertArrayEquals(new PermissionInfo[]{sysRead, sysWrite}, admin.getPermissionGUIDS());
    }

    @Test
    void connectivity_resolvePermissionTokensThroughRole() {
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionInfo sysWrite = permission("perm-sys-write", "system:write");

        RoleInfo sysAdmin = role("role-sys-admin", "role.sysAdmin", sysRead, sysWrite);

        PermissionInfo[] perms = sysAdmin.getPermissionGUIDS();
        assertEquals(2, perms.length);
        assertEquals("system:read", perms[0].getPermissionToken());
        assertEquals("system:write", perms[1].getPermissionToken());
    }

    @Test
    void connectivity_roleGroupHoldsMultipleRoleReferences() {
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionInfo sysWrite = permission("perm-sys-write", "system:write");
        PermissionInfo userRead = permission("perm-user-read", "user:read");

        RoleInfo sysAdmin = role("role-sys-admin", "role.sysAdmin", sysRead, sysWrite);
        RoleInfo userViewer = role("role-user-viewer", "role.userViewer", userRead);

        RoleGroupInfo platformOps = new RoleGroupInfo();
        platformOps.setGUID("group-platform-ops");
        platformOps.setName("group.platformOps");
        platformOps.addRoleGUID(sysAdmin);
        platformOps.addRoleGUID(userViewer);

        assertArrayEquals(new RoleInfo[]{sysAdmin, userViewer}, platformOps.getRoleGUIDS());
    }

    @Test
    void connectivity_fullChain_groupResolvesToPermissionTokens() {
        // Permission -> Role -> RoleGroup, end-to-end resolution without
        // any external GUID lookup — the references are object references.
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionInfo sysWrite = permission("perm-sys-write", "system:write");
        PermissionInfo sysAdminPerm = permission("perm-sys-admin", "system:admin");
        PermissionInfo userRead = permission("perm-user-read", "user:read");
        PermissionInfo userWrite = permission("perm-user-write", "user:write");

        RoleInfo sysAdmin = role("role-sys-admin", "role.sysAdmin", sysRead, sysWrite, sysAdminPerm);
        RoleInfo userEditor = role("role-user-editor", "role.userEditor", userRead, userWrite);
        RoleInfo userViewer = role("role-user-viewer", "role.userViewer", userRead);

        RoleGroupInfo platformOps = new RoleGroupInfo();
        platformOps.setGUID("group-platform-ops");
        platformOps.setName("group.platformOps");
        platformOps.addRoleGUID(sysAdmin);
        platformOps.addRoleGUID(userEditor);
        platformOps.addRoleGUID(userViewer);

        Set<String> effectiveTokens = new LinkedHashSet<>();
        for (RoleInfo r : platformOps.getRoleGUIDS()) {
            for (PermissionInfo p : r.getPermissionGUIDS()) {
                effectiveTokens.add(p.getPermissionToken());
            }
        }

        Set<String> expected = new LinkedHashSet<>();
        expected.add("system:read");
        expected.add("system:write");
        expected.add("system:admin");
        expected.add("user:read");
        expected.add("user:write");
        assertEquals(expected, effectiveTokens);
    }

    @Test
    void connectivity_roleGroupConstructorFromRoles() {
        RoleInfo sysAdmin = role("role-sys-admin", "role.sysAdmin",
                permission("perm-sys-read", "system:read"));
        RoleInfo userViewer = role("role-user-viewer", "role.userViewer",
                permission("perm-user-read", "user:read"));

        RoleGroupInfo platformOps = new RoleGroupInfo(sysAdmin, userViewer);

        assertArrayEquals(new RoleInfo[]{sysAdmin, userViewer}, platformOps.getRoleGUIDS());
    }

    @Test
    void connectivity_dedupeAcrossLayers() {
        // Adding the same permission twice to a role and the same role
        // twice to a group must both dedupe (by NVEntity name).
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        RoleInfo admin = role("role-admin", "role.admin", sysRead);
        admin.addPermissionGUID(sysRead);
        assertEquals(1, admin.getPermissionGUIDS().length);

        RoleGroupInfo group = new RoleGroupInfo();
        group.addRoleGUID(admin);
        group.addRoleGUID(admin);
        assertEquals(1, group.getRoleGUIDS().length);
    }

    @Test
    void connectivity_removalDoesNotCascadeAcrossLayers() {
        // Removing a role from a group must not touch the role's permissions,
        // and removing a permission from a role must not touch the group.
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionInfo sysWrite = permission("perm-sys-write", "system:write");
        RoleInfo admin = role("role-admin", "role.admin", sysRead, sysWrite);

        RoleGroupInfo group = new RoleGroupInfo();
        group.addRoleGUID(admin);

        admin.removePermissionGUID(sysWrite);
        assertArrayEquals(new PermissionInfo[]{sysRead}, admin.getPermissionGUIDS());
        assertArrayEquals(new RoleInfo[]{admin}, group.getRoleGUIDS());

        group.removeRoleGUID(admin);
        assertEquals(0, group.getRoleGUIDS().length);
        assertArrayEquals(new PermissionInfo[]{sysRead}, admin.getPermissionGUIDS());
    }
}
