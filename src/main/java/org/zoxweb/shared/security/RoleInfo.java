package org.zoxweb.shared.security;

import org.zoxweb.shared.util.*;

/**
 * A class used to define a set of Permissions as a Role.
 */
public class RoleInfo extends AuthzInfo {
    public enum Param implements GetNVConfig {
        PERMISSION_GUIDS(NVConfigManager.createNVConfigEntity("permission_guids", "an array of PermissionInfo GUID references", "PermissionGUIDS", false, false, PermissionInfo.NVC_PERMISSION_INFO, NVConfigEntity.ArrayType.GET_NAME_MAP)),
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
     * @param guids an array of permission GUIDS to pass in
     */
    public RoleInfo(PermissionInfo... guids) {
        this();
        setPermissionGUIDS(guids);
    }

    /**
     *
     * @param guids an array of permission GUIDS
     */
    public void setPermissionGUIDS(PermissionInfo... guids) {
        ArrayValues<NVEntity> values = lookup(Param.PERMISSION_GUIDS);

        for (PermissionInfo guid : guids) {
            values.add(guid);
        }
    }

    /**
     *
     * @return an array of permission GUIDS
     */
    public PermissionInfo[] getPermissionGUIDS() {
        return ((ArrayValues<NVEntity>) lookup(Param.PERMISSION_GUIDS)).valuesAs(new PermissionInfo[0]);
    }

    /**
     * Adds a single GUID to the permission list
     *
     * @param guid a PermissionInfo GUID
     */
    public void addPermissionGUID(PermissionInfo guid) {
        ((ArrayValues<NVEntity>) lookup(Param.PERMISSION_GUIDS)).add(guid);
    }

    /**
     * Removes a single GUID from the permission list
     *
     * @param guid a PermissionInfo GUID
     * @return true if removal works, false otherwise
     */
    public boolean removePermissionGUID(PermissionInfo guid) {
        return ((ArrayValues<NVEntity>) lookup(Param.PERMISSION_GUIDS)).remove(guid) != null;
    }
}
