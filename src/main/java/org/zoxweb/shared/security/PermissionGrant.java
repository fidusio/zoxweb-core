package org.zoxweb.shared.security;

import org.zoxweb.shared.util.*;

/**
 * A direct grant of a permission to a subject.
 * <p>
 * The permission comes from exactly one of two places: {@code permission_guid}, a
 * reference to a {@link PermissionInfo} catalog row, or {@code permission_token}, a
 * permission inlined on the grant for subject-to-subject sharing, where the catalog
 * is admin-only. Setting both, or neither, is an invalid grant; the security manager
 * rejects it.
 * <p>
 * An optional embedded {@link ResourceMap} scopes the grant to one resource instance
 * (ReBAC); without it the grant is global. The map is mandatory for an inlined
 * permission, and the resource it names must belong to the grantor. The grantor is
 * recorded in the inherited {@code broker_guid}, the grantee in {@code subject_guid}.
 */
public class PermissionGrant extends GrantBase {

    public enum Param implements GetNVConfig {
        PERMISSION_GUID(NVConfigManager.createNVConfig("permission_guid", "A reference to a permission", "PermissionGUID", false, false, String.class)),
        RESOURCE_MAP(NVConfigManager.createNVConfigEntity("resource_map", "", "", false, false, ResourceMap.class, NVConfigEntity.ArrayType.NOT_ARRAY)),
        PERMISSION_TOKEN(NVConfigManager.createNVConfig("permission_token", "the permission token", "PermissionToken", false, false, String.class)),

        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_PERMISSION_GRANT = new NVConfigEntityPortable(
            "permission_grant",
            null,
            "PermissionGrant",
            true,
            false,
            false,
            false,
            PermissionGrant.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            GrantBase.NVC_GRANT_BASE
    );

    /**
     * The default constructor for the PermissionGrant class.
     */
    public PermissionGrant() {
        super(NVC_PERMISSION_GRANT);
    }

    /**
     * Constructor for a global, catalog-backed grant.
     *
     * @param permissionGUID GUID of the {@link PermissionInfo} row
     */
    public PermissionGrant(String permissionGUID) {
        this(permissionGUID, null);
    }

    /**
     * Constructor for a catalog-backed grant scoped to one resource.
     *
     * @param permissionGUID GUID of the {@link PermissionInfo} row
     * @param resMap         the resource the grant is scoped to
     */
    public PermissionGrant(String permissionGUID, ResourceMap resMap) {
        this();
        setPermissionGUID(permissionGUID);
        setResourceMap(resMap);
    }

    /**
     * Constructor for an inlined grant: the permission is carried on the grant itself and
     * scoped to one resource. {@code permission_guid} stays null.
     *
     * @param resMap            the resource the grant is scoped to; mandatory for an inlined grant
     * @param inlinedPermission the inlined permission token
     */
    public PermissionGrant(ResourceMap resMap, String inlinedPermission) {
        this();
        setResourceMap(resMap);
        setInlinedPermission(inlinedPermission);
    }

    /**
     * @param permissionGUID GUID of the {@link PermissionInfo} row; null when the
     *                       permission is inlined
     */
    public void setPermissionGUID(String permissionGUID) {
        setValue(Param.PERMISSION_GUID, permissionGUID);
    }

    /**
     * @return GUID of the {@link PermissionInfo} row, or null when the permission is inlined
     */
    public String getPermissionGUID() {
        return lookupValue(Param.PERMISSION_GUID);
    }

    /**
     * @param resourceMap the resource the grant is scoped to; null makes the grant global
     */
    public void setResourceMap(ResourceMap resourceMap) {
        setValue(Param.RESOURCE_MAP, resourceMap);
    }

    /**
     * @return the resource the grant is scoped to, or null for a global grant
     */
    public ResourceMap getResourceMap() {
        return lookupValue(Param.RESOURCE_MAP);
    }

    /**
     * @return the inlined permission token, or null when the grant references the catalog
     */
    public String getInlinedPermission() {
        return lookupValue(Param.PERMISSION_TOKEN);
    }

    /**
     * @param permission the inlined permission token; the security manager limits it to
     *                   the verbs read, update, share and delete under the nventity namespace
     */
    public void setInlinedPermission(String permission) {
        setValue(Param.PERMISSION_TOKEN, permission);
    }

}
