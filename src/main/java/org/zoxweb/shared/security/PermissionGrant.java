package org.zoxweb.shared.security;

import org.zoxweb.shared.security.model.SecurityModel;
import org.zoxweb.shared.util.*;

/**
 * A direct grant of a permission to a subject.
 * <p>
 * The permission comes from exactly one of two places: {@code permission_guid}, a
 * reference to a {@link PermissionInfo} catalog row, or {@code permission_token}, a
 * permission inlined on the grant for subject-to-subject sharing, where the catalog
 * is admin-only. Setting both, or neither, is an invalid grant; {@link #validateShape()}
 * and the security manager reject it.
 * <p>
 * An optional embedded {@link ResourceMap} scopes the grant to one resource instance
 * (ReBAC); without it the grant is global. The map is mandatory for an inlined
 * permission, and the resource it names must belong to the grantor. The grantor is
 * recorded in the inherited {@code broker_guid}, the grantee in {@code subject_guid}.
 * <p>
 * An inlined token is normalized and limited by {@link SecurityModel.NVEPermissionTokenFilter}
 * on write: {@code nventity:<verbs>} with the verbs {@code read}, {@code update},
 * {@code share} and {@code delete} only. The resource is never part of the token; the
 * security manager appends {@code :<resource guid>} when it flattens the grant.
 */
public class PermissionGrant extends GrantBase {

    public enum Param implements GetNVConfig {
        PERMISSION_GUID(NVConfigManager.createNVConfig("permission_guid", "A reference to a permission", "PermissionGUID", false, false, String.class)),
        RESOURCE_MAP(NVConfigManager.createNVConfigEntity("resource_map", "", "", false, false, ResourceMap.class, NVConfigEntity.ArrayType.NOT_ARRAY)),
        PERMISSION_TOKEN(NVConfigManager.createNVConfig("permission_token", "the inlined permission token", "PermissionToken", false, false, false, String.class, SecurityModel.NVEPermissionTokenFilter.SINGLETON)),

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
            SUS.extractNVConfigs(Param.values()),
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
     * @param resMap          the resource the grant is scoped to; mandatory for an inlined grant
     * @param permissionToken the inlined permission token, normalized by
     *                        {@link SecurityModel.NVEPermissionTokenFilter}
     * @throws IllegalArgumentException if the token is not {@code nventity:<verbs>} with allowed verbs
     */
    public PermissionGrant(ResourceMap resMap, String permissionToken) {
        this();
        setResourceMap(resMap);
        setPermissionToken(permissionToken);
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
    public String getPermissionToken() {
        return lookupValue(Param.PERMISSION_TOKEN);
    }

    /**
     * @param permissionToken the inlined permission token, {@code nventity:<verbs>} with the
     *                        verbs read, update, share and delete; normalized on write, null clears
     * @throws IllegalArgumentException if the token is not of that shape
     */
    public void setPermissionToken(String permissionToken) {
        setValue(Param.PERMISSION_TOKEN, permissionToken);
    }

    /**
     * Checks the structural preconditions of a grant, without touching any store:
     * exactly one of {@code permission_guid} and {@code permission_token} is set; an
     * inlined token requires a resource map; a resource map, when present, names both a
     * resource type and a resource GUID.
     *
     * @throws IllegalArgumentException if the grant violates one of those rules
     */
    public void validateShape() throws IllegalArgumentException {
        boolean catalog = !SUS.isEmpty(getPermissionGUID());
        boolean inlined = !SUS.isEmpty(getPermissionToken());
        if (catalog == inlined) {
            throw new IllegalArgumentException(catalog
                    ? "a grant is either catalog-backed (permission_guid) or inlined (permission_token), never both"
                    : "a grant needs either a permission_guid or an inlined permission_token");
        }
        ResourceMap rm = getResourceMap();
        if (inlined && rm == null) {
            throw new IllegalArgumentException("an inlined grant requires a resource_map");
        }
        if (rm != null && (SUS.isEmpty(rm.getResourceType()) || SUS.isEmpty(rm.getResourceGUID()))) {
            throw new IllegalArgumentException("resource_map requires both resource_type and resource_guid");
        }
    }

}
