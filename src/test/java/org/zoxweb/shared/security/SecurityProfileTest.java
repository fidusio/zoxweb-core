package org.zoxweb.shared.security;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.crypto.CryptoConst;

import java.io.IOException;
import java.util.Arrays;

public class SecurityProfileTest {
    @Test
    public void securityProp() throws IOException {
        SecurityProfile sc = new SecurityProfile();
        sc.setAuthenticationTypes(CryptoConst.AuthenticationType.values());
        sc.setPermissions("perm1", "perm2");
        sc.setRoles("role1", "role2");
        String json = GSONUtil.toJSON(sc, false, false, false);
        System.out.println(json);
        sc = GSONUtil.fromJSON(json, SecurityProfile.class);
        System.out.println(Arrays.toString(sc.getAuthenticationTypes()));
    }
}
