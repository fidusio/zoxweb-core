package org.zoxweb.shared.security;

import org.zoxweb.shared.util.*;

/**
 * A class that defines a direct grant of a specific role group to a subject.
 */
public class RoleGroupGrant extends AuthzInfo {

    public enum Param implements GetNVConfig {
        ROLE_GROUP_GUID(NVConfigManager.createNVConfig("role_group_guid", "A reference to a role group", "RoleGroupGUID", true, false, String.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_ROLE_GROUP_GRANT = new NVConfigEntityPortable(
            "role_group_grant",
            null,
            "RoleGroupGrant",
            true,
            false,
            false,
            false,
            RoleGroupGrant.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            AuthzInfo.NVC_AUTHZ_INFO
    );

    /**
     * The default constructor for the RoleGroupGrant class
     */
    public RoleGroupGrant() {
        super(NVC_ROLE_GROUP_GRANT);
    }

    /**
     * Constructor that sets the roleGroupGUID
     *
     * @param roleGroupGUID roleGroupGUID to reference
     */
    public RoleGroupGrant(String roleGroupGUID) {
        this();
        setRoleGroupGUID(roleGroupGUID);
    }

    /**
     *
     * @param roleGroupGUID roleGroupGUID to reference
     */
    public void setRoleGroupGUID(String roleGroupGUID) {
        setValue(Param.ROLE_GROUP_GUID, roleGroupGUID);
    }

    /**
     *
     * @return roleGroupGUID to reference
     */
    public String getRoleGroupGUID() {
        return lookupValue(Param.ROLE_GROUP_GUID);
    }
}
