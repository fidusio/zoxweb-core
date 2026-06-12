package org.zoxweb.shared.security;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    void roleInfo_defaultConstructor_permissionGuidsIsEmpty() {
        RoleInfo r = new RoleInfo();
        List<String> guids = r.getPermissionGUIDS();
        assertNotNull(guids);
        assertTrue(guids.isEmpty());
    }

    @Test
    void roleInfo_listConstructor_setsPermissionGuids() {
        RoleInfo r = new RoleInfo(Arrays.asList("p1", "p2"));
        assertEquals(Arrays.asList("p1", "p2"), r.getPermissionGUIDS());
    }

    @Test
    void roleInfo_setAndGetPermissionGuids() {
        RoleInfo r = new RoleInfo();
        r.setPermissionGUIDS(Arrays.asList("a", "b", "c"));
        assertEquals(Arrays.asList("a", "b", "c"), r.getPermissionGUIDS());
    }

    @Test
    void roleInfo_setPermissionGuidsOverwritesPrevious() {
        RoleInfo r = new RoleInfo(Arrays.asList("old-1", "old-2"));
        r.setPermissionGUIDS(Collections.singletonList("new-1"));
        assertEquals(Collections.singletonList("new-1"), r.getPermissionGUIDS());
    }

    @Test
    void roleInfo_addPermissionGuid_appendsToList() {
        RoleInfo r = new RoleInfo();
        r.addPermissionGUID("p1");
        r.addPermissionGUID("p2");
        assertEquals(Arrays.asList("p1", "p2"), r.getPermissionGUIDS());
    }

    @Test
    void roleInfo_addPermissionGuid_dedupesDuplicates() {
        RoleInfo r = new RoleInfo();
        r.addPermissionGUID("p1");
        r.addPermissionGUID("p1");
        r.addPermissionGUID("p1");
        assertEquals(Collections.singletonList("p1"), r.getPermissionGUIDS());
    }

    @Test
    void roleInfo_removePermissionGuid_returnsTrueWhenPresent() {
        RoleInfo r = new RoleInfo(new ArrayList<>(Arrays.asList("p1", "p2")));
        assertTrue(r.removePermissionGUID("p1"));
        assertEquals(Collections.singletonList("p2"), r.getPermissionGUIDS());
    }

    @Test
    void roleInfo_removePermissionGuid_returnsFalseWhenAbsent() {
        RoleInfo r = new RoleInfo();
        r.addPermissionGUID("p1");
        assertFalse(r.removePermissionGUID("does-not-exist"));
        assertEquals(Collections.singletonList("p1"), r.getPermissionGUIDS());
    }

    @Test
    void roleInfo_isAuthzInfo() {
        assertInstanceOf(AuthzInfo.class, new RoleInfo());
    }

    @Test
    void roleInfo_inheritsAuthzInfoFields() {
        RoleInfo r = new RoleInfo();
        r.setBrokerGUID("broker-1");
        r.setName("role.admin");
        r.setDescription("Administrator role");
        r.addPermissionGUID("perm-1");

        assertEquals("broker-1", r.getBrokerGUID());
        assertEquals("role.admin", r.getName());
        assertEquals("Administrator role", r.getDescription());
        assertEquals(Collections.singletonList("perm-1"), r.getPermissionGUIDS());
    }

    // ============================================================
    //                        RoleGroupInfo
    // ============================================================

    @Test
    void roleGroupInfo_defaultConstructor_roleGuidsIsEmpty() {
        RoleGroupInfo g = new RoleGroupInfo();
        List<String> guids = g.getRoleGUIDS();
        assertNotNull(guids);
        assertTrue(guids.isEmpty());
    }

    @Test
    void roleGroupInfo_listConstructor_setsRoleGuids() {
        RoleGroupInfo g = new RoleGroupInfo(Arrays.asList("r1", "r2"));
        assertEquals(Arrays.asList("r1", "r2"), g.getRoleGUIDS());
    }

    @Test
    void roleGroupInfo_setAndGetRoleGuids() {
        RoleGroupInfo g = new RoleGroupInfo();
        g.setRoleGUIDS(Arrays.asList("a", "b"));
        assertEquals(Arrays.asList("a", "b"), g.getRoleGUIDS());
    }

    @Test
    void roleGroupInfo_setRoleGuidsOverwritesPrevious() {
        RoleGroupInfo g = new RoleGroupInfo(Arrays.asList("old-1", "old-2"));
        g.setRoleGUIDS(Arrays.asList("new-1"));
        assertEquals(Arrays.asList("new-1"), g.getRoleGUIDS());
    }

    @Test
    void roleGroupInfo_addRoleGuid_appendsToList() {
        RoleGroupInfo g = new RoleGroupInfo();
        g.addRoleGUID("r1");
        g.addRoleGUID("r2");
        assertEquals(Arrays.asList("r1", "r2"), g.getRoleGUIDS());
    }

    @Test
    void roleGroupInfo_addRoleGuid_dedupesDuplicates() {
        RoleGroupInfo g = new RoleGroupInfo();
        g.addRoleGUID("r1");
        g.addRoleGUID("r1");
        g.addRoleGUID("r1");
        assertEquals(Arrays.asList("r1"), g.getRoleGUIDS());
    }

    @Test
    void roleGroupInfo_removeRoleGuid_returnsTrueWhenPresent() {
        RoleGroupInfo g = new RoleGroupInfo(new ArrayList<>(Arrays.asList("r1", "r2")));
        assertTrue(g.removeRoleGUID("r2"));
        assertEquals(Arrays.asList("r1"), g.getRoleGUIDS());
    }

    @Test
    void roleGroupInfo_removeRoleGuid_returnsFalseWhenAbsent() {
        RoleGroupInfo g = new RoleGroupInfo();
        g.addRoleGUID("r1");
        assertFalse(g.removeRoleGUID("does-not-exist"));
        assertEquals(Arrays.asList("r1"), g.getRoleGUIDS());
    }

    @Test
    void roleGroupInfo_isAuthzInfo() {
        assertInstanceOf(AuthzInfo.class, new RoleGroupInfo());
    }

    @Test
    void roleGroupInfo_flatCollection_noNesting() {
        // RoleGroupInfo holds only RoleInfo GUIDs — no RoleGroupInfo nesting.
        RoleGroupInfo g = new RoleGroupInfo();
        g.addRoleGUID("role-a");
        g.addRoleGUID("role-b");
        g.addRoleGUID("role-c");

        assertEquals(3, g.getRoleGUIDS().size());
        assertTrue(g.getRoleGUIDS().contains("role-a"));
        assertTrue(g.getRoleGUIDS().contains("role-b"));
        assertTrue(g.getRoleGUIDS().contains("role-c"));
    }

    @Test
    void roleGroupInfo_inheritsAuthzInfoFields() {
        RoleGroupInfo g = new RoleGroupInfo();
        g.setBrokerGUID("broker-1");
        g.setName("group.engineers");
        g.setDescription("Engineering role group");
        g.addRoleGUID("role-1");

        assertEquals("broker-1", g.getBrokerGUID());
        assertEquals("group.engineers", g.getName());
        assertEquals("Engineering role group", g.getDescription());
        assertEquals(Arrays.asList("role-1"), g.getRoleGUIDS());
    }

    // ============================================================
    //         Connectivity: Permission -> Role -> RoleGroup
    // ============================================================
    //
    // The model wires the three entities together by GUID references:
    //   PermissionInfo.GUID  -> referenced by RoleInfo.permissionGUIDS
    //   RoleInfo.GUID        -> referenced by RoleGroupInfo.roleGUIDS
    //
    // The tests below build realistic Shiro-style permission tokens
    // (e.g. "system:read"), drop them into a role, and chain multiple
    // roles into a role group, then verify the references resolve.

    private static PermissionInfo permission(String guid, String token) {
        PermissionInfo p = new PermissionInfo(token);
        p.setGUID(guid);
        p.setName("perm." + token);
        return p;
    }

    private static RoleInfo role(String guid, String name, PermissionInfo... perms) {
        RoleInfo r = new RoleInfo();
        r.setGUID(guid);
        r.setName(name);
        for (PermissionInfo p : perms) {
            r.addPermissionGUID(p.getGUID());
        }
        return r;
    }

    @Test
    void connectivity_roleReferencesPermissionsByGuid() {
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionInfo sysWrite = permission("perm-sys-write", "system:write");

        RoleInfo admin = role("role-admin", "role.admin", sysRead, sysWrite);

        assertEquals(Arrays.asList("perm-sys-read", "perm-sys-write"), admin.getPermissionGUIDS());
        assertTrue(admin.getPermissionGUIDS().contains(sysRead.getGUID()));
        assertTrue(admin.getPermissionGUIDS().contains(sysWrite.getGUID()));
    }

    @Test
    void connectivity_resolvePermissionTokensThroughRole() {
        // Simulate a tiny "permission store" the way an authz layer
        // would resolve a role's permission GUIDs back to tokens.
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionInfo sysWrite = permission("perm-sys-write", "system:write");
        PermissionInfo userRead = permission("perm-user-read", "user:read");

        java.util.Map<String, PermissionInfo> store = new java.util.HashMap<>();
        store.put(sysRead.getGUID(), sysRead);
        store.put(sysWrite.getGUID(), sysWrite);
        store.put(userRead.getGUID(), userRead);

        RoleInfo sysAdmin = role("role-sys-admin", "role.sysAdmin", sysRead, sysWrite);

        List<String> resolved = new ArrayList<>();
        for (String guid : sysAdmin.getPermissionGUIDS()) {
            resolved.add(store.get(guid).getPermissionToken());
        }

        assertEquals(Arrays.asList("system:read", "system:write"), resolved);
    }

    @Test
    void connectivity_roleGroupReferencesMultipleRolesByGuid() {
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionInfo sysWrite = permission("perm-sys-write", "system:write");
        PermissionInfo userRead = permission("perm-user-read", "user:read");

        RoleInfo sysAdmin = role("role-sys-admin", "role.sysAdmin", sysRead, sysWrite);
        RoleInfo userViewer = role("role-user-viewer", "role.userViewer", userRead);

        RoleGroupInfo platformOps = new RoleGroupInfo();
        platformOps.setGUID("group-platform-ops");
        platformOps.setName("group.platformOps");
        platformOps.addRoleGUID(sysAdmin.getGUID());
        platformOps.addRoleGUID(userViewer.getGUID());

        assertEquals(Arrays.asList("role-sys-admin", "role-user-viewer"), platformOps.getRoleGUIDS());
        assertTrue(platformOps.getRoleGUIDS().contains(sysAdmin.getGUID()));
        assertTrue(platformOps.getRoleGUIDS().contains(userViewer.getGUID()));
    }

    @Test
    void connectivity_fullChain_groupResolvesToPermissionTokens() {
        // Permission -> Role -> RoleGroup, end-to-end resolution.
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionInfo sysWrite = permission("perm-sys-write", "system:write");
        PermissionInfo sysAdminPerm = permission("perm-sys-admin", "system:admin");
        PermissionInfo userRead = permission("perm-user-read", "user:read");
        PermissionInfo userWrite = permission("perm-user-write", "user:write");

        java.util.Map<String, PermissionInfo> permStore = new java.util.HashMap<>();
        for (PermissionInfo p : new PermissionInfo[]{sysRead, sysWrite, sysAdminPerm, userRead, userWrite}) {
            permStore.put(p.getGUID(), p);
        }

        RoleInfo sysAdmin = role("role-sys-admin", "role.sysAdmin", sysRead, sysWrite, sysAdminPerm);
        RoleInfo userEditor = role("role-user-editor", "role.userEditor", userRead, userWrite);
        RoleInfo userViewer = role("role-user-viewer", "role.userViewer", userRead);

        java.util.Map<String, RoleInfo> roleStore = new java.util.HashMap<>();
        roleStore.put(sysAdmin.getGUID(), sysAdmin);
        roleStore.put(userEditor.getGUID(), userEditor);
        roleStore.put(userViewer.getGUID(), userViewer);

        RoleGroupInfo platformOps = new RoleGroupInfo();
        platformOps.setGUID("group-platform-ops");
        platformOps.setName("group.platformOps");
        platformOps.addRoleGUID(sysAdmin.getGUID());
        platformOps.addRoleGUID(userEditor.getGUID());
        platformOps.addRoleGUID(userViewer.getGUID());

        java.util.Set<String> effectiveTokens = new java.util.LinkedHashSet<>();
        for (String roleGuid : platformOps.getRoleGUIDS()) {
            RoleInfo r = roleStore.get(roleGuid);
            assertNotNull(r, "role lookup failed for " + roleGuid);
            for (String permGuid : r.getPermissionGUIDS()) {
                PermissionInfo p = permStore.get(permGuid);
                assertNotNull(p, "permission lookup failed for " + permGuid);
                effectiveTokens.add(p.getPermissionToken());
            }
        }

        assertEquals(
                new java.util.LinkedHashSet<>(Arrays.asList(
                        "system:read", "system:write", "system:admin", "user:read", "user:write")),
                effectiveTokens);
    }

    @Test
    void connectivity_roleGroupConstructorFromRoleGuids() {
        RoleInfo sysAdmin = role("role-sys-admin", "role.sysAdmin",
                permission("perm-sys-read", "system:read"));
        RoleInfo userViewer = role("role-user-viewer", "role.userViewer",
                permission("perm-user-read", "user:read"));

        RoleGroupInfo platformOps = new RoleGroupInfo(
                Arrays.asList(sysAdmin.getGUID(), userViewer.getGUID()));

        assertEquals(Arrays.asList("role-sys-admin", "role-user-viewer"), platformOps.getRoleGUIDS());
    }

    @Test
    void connectivity_dedupeAcrossLayers() {
        // Adding the same permission GUID twice to a role and the same
        // role GUID twice to a group must both dedupe.
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        RoleInfo admin = role("role-admin", "role.admin", sysRead);
        admin.addPermissionGUID(sysRead.getGUID());
        assertEquals(Collections.singletonList("perm-sys-read"), admin.getPermissionGUIDS());

        RoleGroupInfo group = new RoleGroupInfo();
        group.addRoleGUID(admin.getGUID());
        group.addRoleGUID(admin.getGUID());
        assertEquals(Collections.singletonList("role-admin"), group.getRoleGUIDS());
    }

    @Test
    void connectivity_removalDoesNotCascadeAcrossLayers() {
        // Removing a role from a group must not touch the role's permissions,
        // and removing a permission from a role must not touch the group.
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionInfo sysWrite = permission("perm-sys-write", "system:write");
        RoleInfo admin = role("role-admin", "role.admin", sysRead, sysWrite);

        RoleGroupInfo group = new RoleGroupInfo();
        group.addRoleGUID(admin.getGUID());

        assertTrue(admin.removePermissionGUID(sysWrite.getGUID()));
        assertEquals(Collections.singletonList("perm-sys-read"), admin.getPermissionGUIDS());
        assertEquals(Collections.singletonList("role-admin"), group.getRoleGUIDS());

        assertTrue(group.removeRoleGUID(admin.getGUID()));
        assertTrue(group.getRoleGUIDS().isEmpty());
        assertEquals(Collections.singletonList("perm-sys-read"), admin.getPermissionGUIDS());
    }
}
