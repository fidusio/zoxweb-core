package org.zoxweb.shared.security;

import org.zoxweb.shared.util.*;

/**
 * A class that defines a direct grant of a specific role to a subject.
 */
public class RoleGrant extends AuthzInfo {

    public enum Param implements GetNVConfig {
        ROLE_GUID(NVConfigManager.createNVConfig("role_guid", "A reference to a role", "RoleGUID", true, false, String.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_ROLE_GRANT = new NVConfigEntityPortable(
            "role_grant",
            null,
            "RoleGrant",
            true,
            false,
            false,
            false,
            RoleGrant.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            AuthzInfo.NVC_AUTHZ_INFO
    );

    /**
     * The default constructor for the RoleGrant class.
     */
    public RoleGrant() {
        super(NVC_ROLE_GRANT);
    }

    /**
     * Constructor that sets the roleGUID
     *
     * @param roleGUID roleGUID to reference
     */
    public RoleGrant(String roleGUID) {
        this();
        setRoleGUID(roleGUID);
    }

    /**
     *
     * @param roleGUID roleGUID to reference
     */
    public void setRoleGUID(String roleGUID) {
        setValue(Param.ROLE_GUID, roleGUID);
    }

    /**
     *
     * @return roleGUID to reference
     */
    public String getRoleGUID() {
        return lookupValue(Param.ROLE_GUID);
    }
}
