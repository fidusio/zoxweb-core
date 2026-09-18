package org.zoxweb.shared.security;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.security.model.SecurityModel;

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
    void resourceMap_defaultConstructor_resourceGUIDIsNull() {
        ResourceMap rm = new ResourceMap();
        assertNull(rm.getResourceGUID());
    }



    @Test
    void resourceMap_setAndGetResourceGUID() {
        ResourceMap rm = new ResourceMap();
        rm.setResourceGUID("underlying-2");
        assertEquals("underlying-2", rm.getResourceGUID());
    }



    @Test
    void resourceMap_ownEntityGUIDIsIndependentOfResourceGUIDField() {
        // ResourceMap has TWO distinct GUIDs:
        //   - its own entity GUID (the identity other entities point at)
        //   - the resourceGUID field (the underlying domain object it maps)
        // They must be settable independently and not bleed into one another.
        ResourceMap rm = new ResourceMap("underlying-1", "Object");
        rm.setGUID("rm-entity-1");
        assertEquals("rm-entity-1", rm.getGUID());
        assertEquals("underlying-1", rm.getResourceGUID());
        assertNotEquals(rm.getGUID(), rm.getResourceGUID());
    }



    @Test
    void resourceMap_isAuthzInfo() {
        assertInstanceOf(AuthzInfo.class, new ResourceMap());
    }

    @Test
    void resourceMap_isGrantBase() {
        assertInstanceOf(GrantBase.class, new ResourceMap());
    }

    @Test
    void resourceMap_inheritsAuthzInfoFields() {
        ResourceMap rm = new ResourceMap("invoice-42", "OBJECT");
        rm.setBrokerGUID("broker-1");
        rm.setName("resource.invoice42");
        rm.setDescription("Invoice #42 record");

        assertEquals("broker-1", rm.getBrokerGUID());
        assertEquals("resource.invoice42", rm.getName());
        assertEquals("Invoice #42 record", rm.getDescription());
        assertEquals("OBJECT", rm.getResourceType());
        assertEquals("invoice-42", rm.getResourceGUID());
    }

    // ============================================================
    //                       PermissionGrant
    // ============================================================

    @Test
    void permissionGrant_defaultConstructor_guidsAreNull() {
        PermissionGrant pg = new PermissionGrant();
        assertNull(pg.getPermissionGUID());
        assertNull(pg.getResourceMap());
    }

    @Test
    void permissionGrant_singleArgConstructor_setsPermissionGUID() {
        PermissionGrant pg = new PermissionGrant("perm-1");
        assertEquals("perm-1", pg.getPermissionGUID());
        assertNull(pg.getResourceMap());
    }

    @Test
    void permissionGrant_twoArgConstructor_setsPermissionGUIDAndResourceMap() {
        ResourceMap rm = new ResourceMap("resource-1", "org.zoxweb.shared.data.DocumentDAO");
        PermissionGrant pg = new PermissionGrant("perm-1", rm);
        assertEquals("perm-1", pg.getPermissionGUID());
        assertSame(rm, pg.getResourceMap());
        assertEquals("resource-1", pg.getResourceMap().getResourceGUID());
        assertEquals("org.zoxweb.shared.data.DocumentDAO", pg.getResourceMap().getResourceType());
    }

    @Test
    void permissionGrant_inlinedConstructor_setsResourceMapAndToken_permissionGUIDNull() {
        ResourceMap rm = new ResourceMap("resource-1", "org.zoxweb.shared.data.DocumentDAO");
        PermissionGrant pg = new PermissionGrant(rm, "nventity:read,share");
        assertSame(rm, pg.getResourceMap());
        assertEquals("nventity:read,share", pg.getPermissionToken());
        assertNull(pg.getPermissionGUID());
    }

    @Test
    void permissionGrant_catalogConstructor_leavesPermissionTokenNull() {
        ResourceMap rm = new ResourceMap("resource-1", "org.zoxweb.shared.data.DocumentDAO");
        PermissionGrant pg = new PermissionGrant("perm-1", rm);
        assertEquals("perm-1", pg.getPermissionGUID());
        assertNull(pg.getPermissionToken());
    }

    @Test
    void permissionGrant_setAndGetPermissionGUID() {
        PermissionGrant pg = new PermissionGrant();
        pg.setPermissionGUID("perm-2");
        assertEquals("perm-2", pg.getPermissionGUID());
    }

    @Test
    void permissionGrant_setAndGetResourceMap() {
        PermissionGrant pg = new PermissionGrant();
        ResourceMap rm = new ResourceMap("resource-2", "org.zoxweb.shared.data.DocumentDAO");
        pg.setResourceMap(rm);
        assertSame(rm, pg.getResourceMap());
        assertEquals("resource-2", pg.getResourceMap().getResourceGUID());
    }

    @Test
    void permissionGrant_setOverwritesPrevious() {
        PermissionGrant pg = new PermissionGrant("perm-1", new ResourceMap("resource-1", "org.zoxweb.shared.data.DocumentDAO"));
        ResourceMap rm2 = new ResourceMap("resource-2", "org.zoxweb.shared.data.FolderInfoDAO");
        pg.setPermissionGUID("perm-2");
        pg.setResourceMap(rm2);
        assertEquals("perm-2", pg.getPermissionGUID());
        assertSame(rm2, pg.getResourceMap());
        assertEquals("resource-2", pg.getResourceMap().getResourceGUID());
    }

    @Test
    void permissionGrant_setResourceMapToNullClears() {
        // Per spec 8.1: a grant without a resource is a global grant.
        PermissionGrant pg = new PermissionGrant("perm-1", new ResourceMap("resource-1", "org.zoxweb.shared.data.DocumentDAO"));
        pg.setResourceMap(null);
        assertNull(pg.getResourceMap());
    }

    @Test
    void permissionGrant_isAuthzInfo() {
        assertInstanceOf(AuthzInfo.class, new PermissionGrant());
    }

    @Test
    void permissionGrant_isGrantBase() {
        assertInstanceOf(GrantBase.class, new PermissionGrant());
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
    //            Inlined permission token filter + shape
    // ============================================================

    @Test
    void permissionToken_filterNormalizesCaseAndSpaces() {
        ResourceMap rm = new ResourceMap("resource-1", "org.zoxweb.shared.data.DocumentDAO");
        PermissionGrant pg = new PermissionGrant(rm, " NVEntity:Read , Share ");
        assertEquals("nventity:read,share", pg.getPermissionToken());
        pg.setPermissionToken("nventity:delete,delete,update");
        assertEquals("nventity:delete,update", pg.getPermissionToken(), "duplicates dropped, order kept");
    }

    @Test
    void permissionToken_filterRejects() {
        String[] bad = {"nventity:create", "nventity:*", "nventity:read:abc", "doc:read", "nventity:",
                "nventity:read,,share", "nventity:read,bogus", "nventity", ""};
        for (String token : bad) {
            PermissionGrant pg = new PermissionGrant();
            assertThrows(RuntimeException.class, () -> pg.setPermissionToken(token), token);
            assertNull(pg.getPermissionToken(), token);
        }
        assertThrows(RuntimeException.class, () -> SecurityModel.NVEPermissionTokenFilter.SINGLETON.validate(null));
    }

    @Test
    void permissionToken_setNullClears() {
        PermissionGrant pg = new PermissionGrant(new ResourceMap("resource-1", "x.Y"), "nventity:read");
        pg.setPermissionToken(null);
        assertNull(pg.getPermissionToken());
    }

    @Test
    void validateShape_catalogGlobal_ok() {
        assertDoesNotThrow(() -> new PermissionGrant("perm-1").validateShape());
    }

    @Test
    void validateShape_catalogScoped_ok() {
        assertDoesNotThrow(() -> new PermissionGrant("perm-1", new ResourceMap("resource-1", "x.Y")).validateShape());
    }

    @Test
    void validateShape_inlined_ok() {
        assertDoesNotThrow(() -> new PermissionGrant(new ResourceMap("resource-1", "x.Y"), "nventity:read").validateShape());
    }

    @Test
    void validateShape_bothRejected() {
        PermissionGrant pg = new PermissionGrant("perm-1", new ResourceMap("resource-1", "x.Y"));
        pg.setPermissionToken("nventity:read");
        assertThrows(IllegalArgumentException.class, pg::validateShape);
    }

    @Test
    void validateShape_neitherRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PermissionGrant().validateShape());
        assertThrows(IllegalArgumentException.class,
                () -> new PermissionGrant(null, new ResourceMap("resource-1", "x.Y")).validateShape());
    }

    @Test
    void validateShape_inlinedWithoutMapRejected() {
        PermissionGrant pg = new PermissionGrant();
        pg.setPermissionToken("nventity:read");
        assertThrows(IllegalArgumentException.class, pg::validateShape);
    }

    @Test
    void validateShape_mapWithBlankTypeOrGuidRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new PermissionGrant("perm-1", new ResourceMap("resource-1", null)).validateShape());
        assertThrows(IllegalArgumentException.class,
                () -> new PermissionGrant("perm-1", new ResourceMap(null, "x.Y")).validateShape());
        assertThrows(IllegalArgumentException.class,
                () -> new PermissionGrant(new ResourceMap(), "nventity:read").validateShape());
    }

    @Test
    void securityModel_toNVEToken_andIsInstanceScopable() {
        assertEquals("nventity:read,share", SecurityModel.toNVEToken("Read", "share"));
        assertThrows(IllegalArgumentException.class, () -> SecurityModel.toNVEToken("create"));
        assertThrows(IllegalArgumentException.class, SecurityModel::toNVEToken);
        assertTrue(SecurityModel.isInstanceScopable("nventity:read"));
        assertTrue(SecurityModel.isInstanceScopable("nventity:read,update"));
        assertFalse(SecurityModel.isInstanceScopable("nventity:read:*"));
        assertFalse(SecurityModel.isInstanceScopable("nventity:read:abc"));
        assertFalse(SecurityModel.isInstanceScopable("nventity"));
        assertFalse(SecurityModel.isInstanceScopable("nventity:"));
        assertFalse(SecurityModel.isInstanceScopable(null));
    }

    @Test
    void securityModel_nveShareAllPattern() {
        assertEquals("nventity:share:*", SecurityModel.Permission.NVE_SHARE_ALL.getValue());
        assertEquals("nventity:read:*", SecurityModel.Permission.NVE_READ_ALL.getValue());
        assertEquals("*", SecurityModel.Permission.SUPER_ADMIN_ALL.getValue());
    }

    @Test
    void resourceMap_entityConstructor() {
        PermissionInfo p = new PermissionInfo("perm.x", "x:read");
        p.setGUID("guid-1");
        ResourceMap rm = new ResourceMap(p);
        assertEquals("guid-1", rm.getResourceGUID());
        assertEquals(PermissionInfo.class.getName(), rm.getResourceType());
        assertThrows(NullPointerException.class, () -> new ResourceMap((org.zoxweb.shared.util.NVEntity) null));
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
    void roleGrant_isGrantBase() {
        assertInstanceOf(GrantBase.class, new RoleGrant());
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
    void roleGroupGrant_isGrantBase() {
        assertInstanceOf(GrantBase.class, new RoleGroupGrant());
    }

    // ============================================================
    //                   GrantBase shared behavior
    // ============================================================

    @Test
    void grantBase_nameNotMandatory_allSubclassesAllowNullName() {
        // GrantBase relaxes AuthzInfo's mandatory-name requirement.
        // A default-constructed grant must be usable with no name set.
        assertNull(new ResourceMap().getName());
        assertNull(new PermissionGrant().getName());
        assertNull(new RoleGrant().getName());
        assertNull(new RoleGroupGrant().getName());
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

    private static ResourceMap resource(String guid, String name, String resType) {
        ResourceMap rm = new ResourceMap(guid, resType);
        rm.setName(name);
        return rm;
    }

    @Test
    void connectivity_permissionGrantPointsToPermissionInfoByGUID() {
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionGrant grant = new PermissionGrant(sysRead.getGUID());

        assertEquals(sysRead.getGUID(), grant.getPermissionGUID());
        assertNull(grant.getResourceMap());
    }

    @Test
    void connectivity_globalPermissionGrant_resourceMapIsNull() {
        // Per spec 8.1: no resource map means a global grant.
        PermissionInfo sysRead = permission("perm-sys-read", "system:read");
        PermissionGrant grant = new PermissionGrant(sysRead.getGUID());
        assertNull(grant.getResourceMap());
    }

    @Test
    void connectivity_resourceScopedPermissionGrant_bindsToResourceMap() {
        // Per spec 8.1: an embedded ResourceMap binds the grant to a
        // single resource instance (ReBAC).
        PermissionInfo print = permission("perm-device-print", "device:print");
        ResourceMap printer = resource("res-printer-lobby", "resource.printer.lobby",
                "PRINTER");

        PermissionGrant grant = new PermissionGrant(print.getGUID(), printer);

        assertEquals(print.getGUID(), grant.getPermissionGUID());
        assertSame(printer, grant.getResourceMap());
        assertEquals("res-printer-lobby", grant.getResourceMap().getResourceGUID());
        assertEquals("PRINTER", grant.getResourceMap().getResourceType());
    }

    @Test
    void connectivity_perResourceGrants_distinctResourceGUIDs() {
        // Same permission, two resources: each grant binds to its own
        // ResourceMap instance.
        PermissionInfo print = permission("perm-device-print", "device:print");
        ResourceMap lobbyPrinter = resource("res-printer-lobby", "resource.printer.lobby",
                "PRINTER");
        ResourceMap labPrinter = resource("res-printer-lab", "resource.printer.lab",
                "PRINTER");

        PermissionGrant lobbyGrant = new PermissionGrant(print.getGUID(), lobbyPrinter);
        PermissionGrant labGrant = new PermissionGrant(print.getGUID(), labPrinter);

        assertEquals(print.getGUID(), lobbyGrant.getPermissionGUID());
        assertEquals(print.getGUID(), labGrant.getPermissionGUID());
        assertNotEquals(lobbyGrant.getResourceMap().getResourceGUID(), labGrant.getResourceMap().getResourceGUID());
        assertEquals("res-printer-lobby", lobbyGrant.getResourceMap().getResourceGUID());
        assertEquals("res-printer-lab", labGrant.getResourceMap().getResourceGUID());
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
