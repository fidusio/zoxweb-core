package org.zoxweb.shared.security;

import org.zoxweb.shared.data.DataConst;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.SharedUtil;

/**
 * This class extends AuthzInfo as an abstract class, but
 * it removes the necessity for mandatory naming.
 */
public abstract class GrantBase extends AuthzInfo {

    public static final NVConfigEntity NVC_GRANT_BASE = new NVConfigEntityPortable(
            "grant_base",
            null,
            "GrantBase",
            true,
            false,
            false,
            false,
            GrantBase.class,
            SharedUtil.extractNVConfigs(DataConst.DataParam.NAME),
            null,
            false,
            AuthzInfo.NVC_AUTHZ_INFO
    );

    /**
     * Constructor allowing the class to be extended
     *
     * @param nvce NVConfigEntity to pass down
     */
    protected GrantBase(NVConfigEntity nvce) {
        super(nvce);
    }
}
