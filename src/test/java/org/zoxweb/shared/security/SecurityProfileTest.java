package org.zoxweb.shared.security;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.security.model.SecurityModel;
import org.zoxweb.shared.util.NVStringSet;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

public class SecurityProfileTest {
    @Test
    public void securityProp() throws IOException {
        SecurityProfile sc = new SecurityProfile();
        sc.setAuthenticationTypes(CryptoConst.AuthenticationType.values());
        sc.setPermissions("perm1", "perm2", "perm1");
        sc.setRoles("role1", "role2");
        String json = GSONUtil.toJSON(sc, false, false, false);
        System.out.println(json);
        sc = GSONUtil.fromJSON(json, SecurityProfile.class);
        System.out.println(Arrays.toString(sc.getAuthenticationTypes()));
        assert sc.getPermissions().contains("perm1");

        assert (sc.lookup(SecurityProfile.Param.PERMISSIONS) instanceof NVStringSet);
    }

    @Test
    public void tokenTest()
    {

        String subjectGUID = UUID.randomUUID().toString();
        System.out.println(SecurityModel.PERM_SUBJECT_GUID_RESOURCE_ACCESS);
        System.out.println(SecurityModel.SecToken.updateToken(SecurityModel.PERM_SUBJECT_GUID_RESOURCE_ACCESS,
                SecurityModel.SecToken.SUBJECT_GUID.toGNV(subjectGUID)));

        System.out.println(SecurityModel.PERM_RESOURCE_ACCESS_VIA_SUBJECT_GUID);
        System.out.println(SecurityModel.SecToken.updateToken(SecurityModel.PERM_RESOURCE_ACCESS_VIA_SUBJECT_GUID,
                SecurityModel.SecToken.SUBJECT_GUID.toGNV(subjectGUID),
                SecurityModel.SecToken.CRUD.toGNV(SecurityModel.READ)));




        System.out.println(SecurityModel.PERM_RESOURCE_ACCESS);
        System.out.println(SecurityModel.SecToken.updateToken(SecurityModel.PERM_RESOURCE_ACCESS,
                SecurityModel.SecToken.RESOURCE_GUID.toGNV(UUID.randomUUID().toString()),
                SecurityModel.SecToken.CRUD.toGNV(SecurityModel.READ)));
    }
}
