package org.zoxweb.server.security;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.crypto.PasswordDAO;

import java.security.NoSuchAlgorithmException;

public class PasswordDAOTest {
    @Test
    public void createPassword() throws NoSuchAlgorithmException {
        PasswordDAO p = CryptoUtil.hashedPassword(CryptoConst.MDType.SHA_256, 24, 8192, "password");
        System.out.println(GSONUtil.toJSONDefault(p, true));
       // p.setSalt(CryptoUtil.generateRandomHashedBytes());
    }
}
