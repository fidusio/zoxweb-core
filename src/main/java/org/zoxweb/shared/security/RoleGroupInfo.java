package org.zoxweb.shared.security;

import org.zoxweb.shared.util.*;

/**
 * A class used to define a set of Roles as a RoleGroup.
 */
public class RoleGroupInfo extends AuthzInfo {

    public enum Param implements GetNVConfig {
        ROLES(NVConfigManager.createNVConfigEntity("roles", "An array of RoleInfo references", "Roles", false, false, RoleInfo.NVC_ROLE_INFO, NVConfigEntity.ArrayType.GET_NAME_MAP)),
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
     * @param guids an array of Role GUIDS
     */
    public RoleGroupInfo(RoleInfo... guids) {
        this();
        setRoles(guids);
    }

    /**
     *
     * @param guids an array of Role GUIDS
     */
    public void setRoles(RoleInfo... guids) {
        ArrayValues<NVEntity> list = lookup(Param.ROLES);

        for (RoleInfo guid : guids) {
            list.add(guid);
        }

    }

    /**
     *
     * @return an array of Role GUIDS
     */
    public RoleInfo[] getRoleGUIDS() {
        return ((ArrayValues<NVEntity>) lookup(Param.ROLES)).valuesAs(new RoleInfo[0]);
    }

    /**
     * Adds a single GUID to the Role list
     *
     * @param guid a RoleInfo GUID
     */
    public void addRoleGUID(RoleInfo guid) {
        ((ArrayValues<NVEntity>) lookup(Param.ROLES)).add(guid);
    }

    /**
     * Removes a single GUID from the Role list
     *
     * @param guid a RoleInfo GUID
     * @return true if removal works, false otherwise
     */
    public boolean removeRoleGUID(RoleInfo guid) {
        return ((ArrayValues<NVEntity>) lookup(Param.ROLES)).remove(guid) != null;
    }
}
