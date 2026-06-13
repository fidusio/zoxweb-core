package org.zoxweb.shared.security;

import org.zoxweb.shared.util.*;

/**
 * A class used to define Shiro style colon-separated string (read:system:file)
 * to define a user permission.
 */
public class PermissionInfo extends AuthzInfo {

    public enum Param implements GetNVConfig {
        PERMISSION_TOKEN(NVConfigManager.createNVConfig("permission_token", "the permission token", "PermissionToken", true, false, String.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_PERMISSION_INFO = new NVConfigEntityPortable(
            "permission_info",
            null,
            "PermissionInfo",
            true,
            false,
            false,
            false,
            PermissionInfo.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            AuthzInfo.NVC_AUTHZ_INFO
    );

    /**
     * The default constructor for the Permission Info class
     */
    public PermissionInfo() {
        super(NVC_PERMISSION_INFO);
    }

    /**
     * Constructor that sets the mandatory name and a permission token
     *
     * @param name the mandatory permission name
     * @param permissionToken string permission token
     */
    public PermissionInfo(String name, String permissionToken) {
        this();
        setName(name);
        setPermissionToken(permissionToken);
    }

    /**
     *
     * @return a string permission token
     */
    public String getPermissionToken() {
        return lookupValue(Param.PERMISSION_TOKEN);
    }

    /**
     *
     * @param token a string permission token
     */
    public void setPermissionToken(String token) {
        setValue(Param.PERMISSION_TOKEN, token);
    }
}
