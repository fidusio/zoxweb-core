package org.zoxweb.shared.security;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionGrantTest {

    // ============================================================
    //                         ResourceMap
    // ============================================================

    @Test
    void resourceMap_defaultConstructor_typeIsNull() {
        ResourceMap rm = new ResourceMap();
        assertNull(rm.getResourceType());
    }

    @Test
    void resourceMap_constructorWithType_setsType() {
        ResourceMap rm = new ResourceMap(ResourceMap.ResourceType.URI);
        assertEquals(ResourceMap.ResourceType.URI, rm.getResourceType());
    }

    @Test
    void resourceMap_setAndGetType() {
        ResourceMap rm = new ResourceMap();
        rm.setResourceType(ResourceMap.ResourceType.PRINTER);
        assertEquals(ResourceMap.ResourceType.PRINTER, rm.getResourceType());
    }

    @Test
    void resourceMap_setTypeOverwritesPrevious() {
        ResourceMap rm = new ResourceMap(ResourceMap.ResourceType.OBJECT);
        rm.setResourceType(ResourceMap.ResourceType.METHOD);
        assertEquals(ResourceMap.ResourceType.METHOD, rm.getResourceType());
    }

    @Test
    void resourceMap_allResourceTypes_roundTrip() {
        for (ResourceMap.ResourceType t : ResourceMap.ResourceType.values()) {
            ResourceMap rm = new ResourceMap(t);
            assertEquals(t, rm.getResourceType());
        }
    }

    @Test
    void resourceMap_isAuthzInfo() {
        assertInstanceOf(AuthzInfo.class, new ResourceMap());
    }

    @Test
    void resourceMap_inheritsAuthzInfoFields() {
        ResourceMap rm = new ResourceMap(ResourceMap.ResourceType.OBJECT);
        rm.setBrokerGUID("broker-1");
        rm.setName("resource.invoice42");
        rm.setDescription("Invoice #42 record");

        assertEquals("broker-1", rm.getBrokerGUID());
        assertEquals("resource.invoice42", rm.getName());
        assertEquals("Invoice #42 record", rm.getDescription());
        assertEquals(ResourceMap.ResourceType.OBJECT, rm.getResourceType());
    }

    // ============================================================
    //                       PermissionGrant
    // ============================================================

    @Test
    void permissionGrant_defaultConstructor_guidsAreNull() {
        PermissionGrant pg = new PermissionGrant();
        assertNull(pg.getPermissionGUID());
        assertNull(pg.getResourceGUID());
    }

    @Test
    void permissionGrant_singleArgConstructor_setsPermissionGUID() {
        PermissionGrant pg = new PermissionGrant("perm-1");
        assertEquals("perm-1", pg.getPermissionGUID());
        assertNull(pg.getResourceGUID());
    }

    @Test
    void permissionGrant_twoArgConstructor_setsBothGUIDs() {
        PermissionGrant pg = new PermissionGrant("perm-1", "resource-1");
        assertEquals("perm-1", pg.getPermissionGUID());
        assertEquals("resource-1", pg.getResourceGUID());
    }

    @Test
    void permissionGrant_setAndGetPermissionGUID() {
        PermissionGrant pg = new PermissionGrant();
        pg.setPermissionGUID("perm-2");
        assertEquals("perm-2", pg.getPermissionGUID());
    }

    @Test
    void permissionGrant_setAndGetResourceGUID() {
        PermissionGrant pg = new PermissionGrant();
        pg.setResourceGUID("resource-2");
        assertEquals("resource-2", pg.getResourceGUID());
    }

    @Test
    void permissionGrant_setOverwritesPrevious() {
        PermissionGrant pg = new PermissionGrant("perm-1", "resource-1");
        pg.setPermissionGUID("perm-2");
        pg.setResourceGUID("resource-2");
        assertEquals("perm-2", pg.getPermissionGUID());
        assertEquals("resource-2", pg.getResourceGUID());
    }

    @Test
    void permissionGrant_setResourceGUIDToNullClears() {
        // Per spec 8.1: NULL ResourceMapGUID means a global grant.
        PermissionGrant pg = new PermissionGrant("perm-1", "resource-1");
        pg.setResourceGUID(null);
        assertNull(pg.getResourceGUID());
    }

    @Test
    void permissionGrant_isAuthzInfo() {
        assertInstanceOf(AuthzInfo.class, new PermissionGrant());
    }

    @Test
    void permissionGrant_inheritsAuthzInfoFields() {
        PermissionGrant pg = new PermissionGrant("perm-1");
        pg.setBrokerGUID("broker-1");
        pg.setName("grant.read-system");
        pg.setDescription("Grants system:read to user-42");

        assertEquals("broker-1", pg.getBrokerGUID());
        assertEquals("grant.read-system", pg.getName());
        assertEquals("Grants system:read to user-42", pg.getDescription());
        assertEquals("perm-1", pg.getPermissionGUID());
    }

    // ============================================================
    //                         RoleGrant
    // ============================================================

    @Test
    void roleGrant_defaultConstructor_roleGUIDIsNull() {
        RoleGrant rg = new RoleGrant();
        assertNull(rg.getRoleGUID());
    }

    @Test
    void roleGrant_constructorSetsRoleGUID() {
        RoleGrant rg = new RoleGrant("role-1");
        assertEquals("role-1", rg.getRoleGUID());
    }

    @Test
    void roleGrant_setAndGetRoleGUID() {
        RoleGrant rg = new RoleGrant();
        rg.setRoleGUID("role-2");
        assertEquals("role-2", rg.getRoleGUID());
    }

    @Test
    void roleGrant_setRoleGUIDOverwrites() {
        RoleGrant rg = new RoleGrant("role-1");
        rg.setRoleGUID("role-2");
        assertEquals("role-2", rg.getRoleGUID());
    }

    @Test
    void roleGrant_setRoleGUIDToNullClears() {
        RoleGrant rg = new RoleGrant("role-1");
        rg.setRoleGUID(null);
        assertNull(rg.getRoleGUID());
    }

    @Test
    void roleGrant_isAuthzInfo() {
        assertInstanceOf(AuthzInfo.class, new RoleGrant());
    }

    @Test
    void roleGrant_inheritsAuthzInfoFields() {
        RoleGrant rg = new RoleGrant("role-1");
        rg.setBrokerGUID("broker-1");
        rg.setName("grant.role.sysAdmin");
        rg.setDescription("Grants sysAdmin role to user-42");

        assertEquals("broker-1", rg.getBrokerGUID());
        assertEquals("grant.role.sysAdmin", rg.getName());
        assertEquals("Grants sysAdmin role to user-42", rg.getDescription());
        assertEquals("role-1", rg.getRoleGUID());
    }

    // ============================================================
    //                       RoleGroupGrant
    // ============================================================

    @Test
    void roleGroupGrant_defaultConstructor_roleGroupGUIDIsNull() {
        RoleGroupGrant rgg = new RoleGroupGrant();
        assertNull(rgg.getRoleGroupGUID());
    }

    @Test
    void roleGroupGrant_constructorSetsRoleGroupGUID() {
        RoleGroupGrant rgg = new RoleGroupGrant("group-1");
        assertEquals("group-1", rgg.getRoleGroupGUID());
    }

    @Test
    void roleGroupGrant_setAndGetRoleGroupGUID() {
        RoleGroupGrant rgg = new RoleGroupGrant();
        rgg.setRoleGroupGUID("group-2");
        assertEquals("group-2", rgg.getRoleGroupGUID());
    }

    @Test
    void roleGroupGrant_setRoleGroupGUIDOverwrites() {
        RoleGroupGrant rgg = new RoleGroupGrant("group-1");
        rgg.setRoleGroupGUID("group-2");
        assertEquals("group-2", rgg.getRoleGroupGUID());
    }

    @Test
    void roleGroupGrant_setRoleGroupGUIDToNullClears() {
        RoleGroupGrant rgg = new RoleGroupGrant("group-1");
        rgg.setRoleGroupGUID(null);
        assertNull(rgg.getRoleGroupGUID());
    }

    @Test
    void roleGroupGrant_isAuthzInfo() {
        assertInstanceOf(AuthzInfo.class, new RoleGroupGrant());
    }

    @Test
    void roleGroupGrant_inheritsAuthzInfoFields() {
        RoleGroupGrant rgg = new RoleGroupGrant("group-1");
        rgg.setBrokerGUID("broker-1");
        rgg.setName("grant.group.platformOps");
        rgg.setDescription("Grants the platformOps group to team-7");

        assertEquals("broker-1", rgg.getBrokerGUID());
        assertEquals("grant.group.platformOps", rgg.getName());
        assertEquals("Grants the platformOps group to team-7", rgg.getDescription());
        assertEquals("group-1", rgg.getRoleGroupGUID());
    }

    // ============================================================
    //          Connectivity: Grants <-> Catalog (Section 6/7/8)
    // ============================================================
    // Catalog tests in PermissionInfoTest cover the Permission -> Role
    // -> RoleGroup object graph. These tests cover the second half:
    // grants reference catalog entities by GUID, and the resolution chain
    //   RoleGroupGrant -> RoleGroupInfo -> RoleInfo -> PermissionInfo
    // surfaces the right Shiro-style permission tokens.

    private static PermissionInfo permission(String guid, String token) {
        PermissionInfo p = new PermissionInfo("perm." + token, token);
        p.setGUID(guid);
        return p;
    }

    private static RoleInfo role(String guid, String name, PermissionInfo... perms) {
        RoleInfo r = new RoleInfo(perms);
        r.setGUID(guid);
        r.setName(name);
        return r;
    }

    private static RoleGroupInfo group(String guid, String name, RoleInfo... roles) {
        RoleGroupInfo g = new RoleGroupInfo(roles);
        g.setGUID(guid);
        g.setName(name);
        return g;
    }

    private static ResourceMap resource(String guid, String name, ResourceMap.ResourceType type) {
        ResourceMap rm = new ResourceMap(type);
        rm.setGUID(guid);
        rm.setName(name);
        return rm;
    }

    @Test
    void connectivity_permissionGrantPointsToPermissionInfoByGUID() {
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionGrant grant = new PermissionGrant(sysRead.getGUID());

        assertEquals(sysRead.getGUID(), grant.getPermissionGUID());
        assertNull(grant.getResourceGUID());
    }

    @Test
    void connectivity_globalPermissionGrant_resourceGUIDIsNull() {
        // Per spec 8.1: NULL ResourceMapGUID means a global grant.
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionGrant grant = new PermissionGrant(sysRead.getGUID());
        assertNull(grant.getResourceGUID());
    }

    @Test
    void connectivity_resourceScopedPermissionGrant_bindsToResourceMap() {
        // Per spec 8.1: a non-null ResourceMapGUID binds the grant to a
        // single resource instance (ReBAC).
        PermissionInfo print = permission("perm-device-print", "device:print");
        ResourceMap printer = resource("res-printer-lobby", "resource.printer.lobby",
                ResourceMap.ResourceType.PRINTER);

        PermissionGrant grant = new PermissionGrant(print.getGUID(), printer.getGUID());

        assertEquals(print.getGUID(), grant.getPermissionGUID());
        assertEquals(printer.getGUID(), grant.getResourceGUID());
    }

    @Test
    void connectivity_perResourceGrants_distinctResourceGUIDs() {
        // Same permission, two resources: each grant binds to its own
        // ResourceMap instance.
        PermissionInfo print = permission("perm-device-print", "device:print");
        ResourceMap lobbyPrinter = resource("res-printer-lobby", "resource.printer.lobby",
                ResourceMap.ResourceType.PRINTER);
        ResourceMap labPrinter = resource("res-printer-lab", "resource.printer.lab",
                ResourceMap.ResourceType.PRINTER);

        PermissionGrant lobbyGrant = new PermissionGrant(print.getGUID(), lobbyPrinter.getGUID());
        PermissionGrant labGrant = new PermissionGrant(print.getGUID(), labPrinter.getGUID());

        assertEquals(print.getGUID(), lobbyGrant.getPermissionGUID());
        assertEquals(print.getGUID(), labGrant.getPermissionGUID());
        assertNotEquals(lobbyGrant.getResourceGUID(), labGrant.getResourceGUID());
        assertEquals(lobbyPrinter.getGUID(), lobbyGrant.getResourceGUID());
        assertEquals(labPrinter.getGUID(), labGrant.getResourceGUID());
    }

    @Test
    void connectivity_roleGrantPointsToRoleInfoByGUID() {
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        RoleInfo sysAdmin = role("role-sys-admin", "role.sysAdmin", sysRead);

        RoleGrant grant = new RoleGrant(sysAdmin.getGUID());

        assertEquals(sysAdmin.getGUID(), grant.getRoleGUID());
    }

    @Test
    void connectivity_roleGroupGrantPointsToRoleGroupInfoByGUID() {
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        RoleInfo sysAdmin = role("role-sys-admin", "role.sysAdmin", sysRead);
        RoleGroupInfo ops = group("group-ops", "group.ops", sysAdmin);

        RoleGroupGrant grant = new RoleGroupGrant(ops.getGUID());

        assertEquals(ops.getGUID(), grant.getRoleGroupGUID());
    }

    @Test
    void connectivity_permissionGrantResolvesToPermissionToken() {
        // Grant references the permission by GUID; the catalog resolves
        // it to a Shiro-style token.
        PermissionInfo sysWrite = permission("perm-sys-write", "system:write");
        Map<String, PermissionInfo> catalog = new HashMap<>();
        catalog.put(sysWrite.getGUID(), sysWrite);

        PermissionGrant grant = new PermissionGrant(sysWrite.getGUID());
        PermissionInfo resolved = catalog.get(grant.getPermissionGUID());

        assertNotNull(resolved);
        assertEquals("system:write", resolved.getPermissionToken());
    }

    @Test
    void connectivity_roleGrantResolvesToAllPermissionTokens() {
        // RoleGrant -> RoleInfo -> PermissionInfo[]: one role grant
        // covers every permission baked into the role.
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionInfo sysWrite = permission("perm-sys-write", "system:write");
        RoleInfo sysAdmin = role("role-sys-admin", "role.sysAdmin", sysRead, sysWrite);

        Map<String, RoleInfo> roleCatalog = new HashMap<>();
        roleCatalog.put(sysAdmin.getGUID(), sysAdmin);

        RoleGrant grant = new RoleGrant(sysAdmin.getGUID());
        RoleInfo resolved = roleCatalog.get(grant.getRoleGUID());

        Set<String> tokens = new LinkedHashSet<>();
        for (PermissionInfo p : resolved.getPermissions()) {
            tokens.add(p.getPermissionToken());
        }
        Set<String> expected = new LinkedHashSet<>();
        expected.add("system:read");
        expected.add("system:write");
        assertEquals(expected, tokens);
    }

    @Test
    void connectivity_roleGroupGrant_fullResolutionChain() {
        // Per spec resolution chain:
        //   RoleGroupGrant -> RoleGroupInfo -> RoleInfo -> PermissionInfo
        // A single role-group grant must surface every permission token
        // belonging to every role in the group.
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionInfo sysWrite = permission("perm-sys-write", "system:write");
        PermissionInfo userRead = permission("perm-user-read", "user:read");
        PermissionInfo userWrite = permission("perm-user-write", "user:write");

        RoleInfo sysAdmin = role("role-sys-admin", "role.sysAdmin", sysRead, sysWrite);
        RoleInfo userEditor = role("role-user-editor", "role.userEditor", userRead, userWrite);

        RoleGroupInfo platformOps = group("group-platform-ops", "group.platformOps",
                sysAdmin, userEditor);

        Map<String, RoleGroupInfo> groupCatalog = new HashMap<>();
        groupCatalog.put(platformOps.getGUID(), platformOps);

        RoleGroupGrant grant = new RoleGroupGrant(platformOps.getGUID());
        RoleGroupInfo resolved = groupCatalog.get(grant.getRoleGroupGUID());

        Set<String> tokens = new LinkedHashSet<>();
        for (RoleInfo r : resolved.getRoles()) {
            for (PermissionInfo p : r.getPermissions()) {
                tokens.add(p.getPermissionToken());
            }
        }
        Set<String> expected = new LinkedHashSet<>();
        expected.add("system:read");
        expected.add("system:write");
        expected.add("user:read");
        expected.add("user:write");
        assertEquals(expected, tokens);
    }

    @Test
    void connectivity_grantsReferenceCatalogByGUID_notByEmbedding() {
        // Renaming a catalog entity must NOT break existing grants —
        // grants reference by GUID, so the link survives renames and
        // token updates.
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionGrant grant = new PermissionGrant(sysRead.getGUID());

        sysRead.setName("perm.renamed");
        sysRead.setPermissionToken("system:read:v2");

        assertEquals("perm-sys-read", grant.getPermissionGUID());
        assertEquals(sysRead.getGUID(), grant.getPermissionGUID());
    }

    @Test
    void connectivity_multipleGrantKinds_independentAuthzEntities() {
        // A subject can hold grants of different kinds; each is its own
        // AuthzInfo entity with independent name and target pointer.
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        RoleInfo sysAdmin = role("role-sys-admin", "role.sysAdmin", sysRead);
        RoleGroupInfo ops = group("group-ops", "group.ops", sysAdmin);

        PermissionGrant pg = new PermissionGrant(sysRead.getGUID());
        pg.setName("grant.user-42.sys-read");
        RoleGrant rg = new RoleGrant(sysAdmin.getGUID());
        rg.setName("grant.user-42.sys-admin-role");
        RoleGroupGrant rgg = new RoleGroupGrant(ops.getGUID());
        rgg.setName("grant.user-42.ops-group");

        assertEquals("grant.user-42.sys-read", pg.getName());
        assertEquals("grant.user-42.sys-admin-role", rg.getName());
        assertEquals("grant.user-42.ops-group", rgg.getName());
        assertEquals(sysRead.getGUID(), pg.getPermissionGUID());
        assertEquals(sysAdmin.getGUID(), rg.getRoleGUID());
        assertEquals(ops.getGUID(), rgg.getRoleGroupGUID());
    }
}
