package org.zoxweb.shared.security;

import org.zoxweb.shared.util.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A class used to define a set of Permissions as a Role.
 */
public class RoleInfo extends AuthzInfo {
    public enum Param implements GetNVConfig {
        PERMISSION_GUIDS(NVConfigManager.createNVConfig("permission_guids", "a list of PermissionInfo GUID references", "PermissionGUIDS", false, false, NVStringList.class)),
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
     * @param guids a list of permission GUIDS to pass in
     */
    public RoleInfo(List<String> guids) {
        this();
        setPermissionGUIDS(guids);
    }

    /**
     *
     * @param guids a list of permission GUIDS
     */
    public void setPermissionGUIDS(List<String> guids) {
        setValue(Param.PERMISSION_GUIDS, guids);
    }

    /**
     *
     * @return a list of permission GUIDS
     */
    public List<String> getPermissionGUIDS() {
        return lookupValue(Param.PERMISSION_GUIDS);
    }

    /**
     * Adds a single GUID to the permission list
     *
     * @param guid a string GUID
     */
    public void addPermissionGUID(String guid) {
        List<String> list = getPermissionGUIDS();
        if (list == null) {
            list = new ArrayList<>();
            setPermissionGUIDS(list);
        }

        if (!list.contains(guid)) {
            list.add(guid);
        }
    }

    /**
     * Removes a single GUID from the permission list
     *
     * @param guid a string GUID
     * @return true if remove is successful, false otherwise
     */
    public boolean removePermissionGUID(String guid) {
        return getPermissionGUIDS().remove(guid);
    }
}
