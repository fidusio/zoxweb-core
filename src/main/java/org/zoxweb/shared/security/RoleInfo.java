package org.zoxweb.shared.security;

import org.zoxweb.shared.util.*;

/**
 * A class used to define a set of Permissions as a Role.
 */
public class RoleInfo extends AuthzInfo {
    public enum Param implements GetNVConfig {
        PERMISSIONS(NVConfigManager.createNVConfigEntity("permissions", "an array of PermissionInfo references", "Permissions", false, false, PermissionInfo.NVC_PERMISSION_INFO, NVConfigEntity.ArrayType.GET_NAME_MAP)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_ROLE_INFO = new NVConfigEntityPortable(
            "role_info",
            null,
            "RoleInfo",
            true,
            false,
            false,
            false,
            RoleInfo.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            AuthzInfo.NVC_AUTHZ_INFO
    );

    /**
     * The default constructor of the RoleInfo class
     */
    public RoleInfo() {
        super(NVC_ROLE_INFO);
    }

    /**
     * Constructor that sets a role
     *
     * @param permissions an array of permission GUIDS to pass in
     */
    public RoleInfo(PermissionInfo... permissions) {
        this();
        setPermissions(permissions);
    }

    /**
     *
     * @param permissions an array of PermissionInfo
     */
    public void setPermissions(PermissionInfo... permissions) {
        ArrayValues<NVEntity> values = lookup(Param.PERMISSIONS);

        for (PermissionInfo permission : permissions) {
            values.add(permission);
        }
    }

    /**
     *
     * @return an array of PermissionInfo
     */
    public PermissionInfo[] getPermissions() {
        return ((ArrayValues<NVEntity>) lookup(Param.PERMISSIONS)).valuesAs(new PermissionInfo[0]);
    }

    /**
     * Adds a single GUID to the permission list
     *
     * @param permission a PermissionInfo
     */
    public void addPermission(PermissionInfo permission) {
        ((ArrayValues<NVEntity>) lookup(Param.PERMISSIONS)).add(permission);
    }

    /**
     * Removes a single GUID from the permission list
     *
     * @param permission a PermissionInfo
     * @return true if removal works, false otherwise
     */
    public boolean removePermission(PermissionInfo permission) {
        return ((ArrayValues<NVEntity>) lookup(Param.PERMISSIONS)).remove(permission) != null;
    }
}
