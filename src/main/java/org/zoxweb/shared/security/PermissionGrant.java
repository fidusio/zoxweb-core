package org.zoxweb.shared.security;

import org.zoxweb.shared.util.*;

/**
 * A class that defines a direct grant of a specific permission to a subject.
 * Optional ResourceMapGUID binds the grant to a single resource instance (ReBAC).
 */
public class PermissionGrant extends GrantBase {

    public enum Param implements GetNVConfig {
        PERMISSION_GUID(NVConfigManager.createNVConfig("permission_guid", "A reference to a permission", "PermissionGUID", true, false, String.class)),
        RESOURCE_GUID(NVConfigManager.createNVConfig("resource_guid", "A reference to a resource", "ResourceGUID", false, false, String.class)),
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
     * Constructor that sets the mandatory permissionGUID.
     *
     * @param permissionGUID permission reference
     */
    public PermissionGrant(String permissionGUID) {
        this();
        setPermissionGUID(permissionGUID);
    }

    /**
     * Constructor that sets the mandatory permissionGUID,
     * and the resourceGUID.
     *
     * @param permissionGUID permission reference
     * @param resourceGUID   resource reference
     */
    public PermissionGrant(String permissionGUID, String resourceGUID) {
        this(permissionGUID);
        setResourceGUID(resourceGUID);
    }

    /**
     *
     * @param permissionGUID permission reference
     */
    public void setPermissionGUID(String permissionGUID) {
        setValue(Param.PERMISSION_GUID, permissionGUID);
    }

    /**
     *
     * @return permission reference
     */
    public String getPermissionGUID() {
        return lookupValue(Param.PERMISSION_GUID);
    }

    /**
     *
     * @param resourceGUID resource reference
     */
    public void setResourceGUID(String resourceGUID) {
        setValue(Param.RESOURCE_GUID, resourceGUID);
    }

    /**
     *
     * @return resource reference
     */
    public String getResourceGUID() {
        return lookupValue(Param.RESOURCE_GUID);
    }
}
