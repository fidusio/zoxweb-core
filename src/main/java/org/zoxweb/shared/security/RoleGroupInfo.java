package org.zoxweb.shared.security;

import org.zoxweb.shared.util.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A class used to define a set of Roles as a RoleGroup.
 */
public class RoleGroupInfo extends AuthzInfo {

    public enum Param implements GetNVConfig {
        ROLE_GUIDS(NVConfigManager.createNVConfig("role_guids", "a list of RoleInfo GUID references", "RoleGUIDS", false, false, NVStringList.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_ROLE_GROUP_INFO = new NVConfigEntityPortable(
            "role_group_info",
            null,
            "RoleGroupInfo",
            true,
            false,
            false,
            false,
            RoleGroupInfo.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            AuthzInfo.NVC_AUTHZ_INFO
    );

    /**
     * The default constructor for the RoleGroupInfo class
     */
    public RoleGroupInfo() {
        super(NVC_ROLE_GROUP_INFO);
    }

    /**
     * Constructor that sets a Role Group
     *
     * @param guids a list of Role GUIDS
     */
    public RoleGroupInfo(List<String> guids) {
        this();
        setRoleGUIDS(guids);
    }

    /**
     *
     * @param guids a list of Role GUIDS
     */
    public void setRoleGUIDS(List<String> guids) {
        setValue(Param.ROLE_GUIDS, guids);
    }

    /**
     *
     * @return a list of Role GUIDS
     */
    public List<String> getRoleGUIDS() {
        return lookupValue(Param.ROLE_GUIDS);
    }

    /**
     * Adds a single GUID to the Role list
     *
     * @param guid a string GUID
     */
    public void addRoleGUID(String guid) {
        List<String> list = getRoleGUIDS();
        if (list == null) {
            list = new ArrayList<>();
            setRoleGUIDS(list);
        }

        if (!list.contains(guid)) {
            list.add(guid);
        }
    }

    /**
     * Removes a single GUID from the Role list
     *
     * @param guid a string GUID
     * @return true if remove is successful, false otherwise
     */
    public boolean removeRoleGUID(String guid) {
        return getRoleGUIDS().remove(guid);
    }
}
