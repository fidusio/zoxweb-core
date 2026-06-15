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
     * @param roles an array of RoleInfo
     */
    public RoleGroupInfo(RoleInfo... roles) {
        this();
        setRoles(roles);
    }

    /**
     *
     * @param roles an array of RoleInfo
     */
    public void setRoles(RoleInfo... roles) {
        ArrayValues<NVEntity> list = lookup(Param.ROLES);

        for (RoleInfo role : roles) {
            list.add(role);
        }

    }

    /**
     *
     * @return an array of RoleInfo
     */
    public RoleInfo[] getRoles() {
        return ((ArrayValues<NVEntity>) lookup(Param.ROLES)).valuesAs(new RoleInfo[0]);
    }

    /**
     * Adds a single Role to the Role list
     *
     * @param role a RoleInfo
     */
    public void addRole(RoleInfo role) {
        ((ArrayValues<NVEntity>) lookup(Param.ROLES)).add(role);
    }

    /**
     * Removes a single Role from the Role list
     *
     * @param role a RoleInfo
     * @return true if removal works, false otherwise
     */
    public boolean removeRole(RoleInfo role) {
        return ((ArrayValues<NVEntity>) lookup(Param.ROLES)).remove(role) != null;
    }
}
