package org.zoxweb.server.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.crypto.PasswordDAO;
import org.zoxweb.shared.crypto.BCryptHash;
import org.zoxweb.shared.util.SharedStringUtil;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class PasswordDAOTest {
    @Test
    public void createPassword() throws NoSuchAlgorithmException {
        PasswordDAO p = HashUtil.toPassword(CryptoConst.HASHType.SHA_256, 24, 8192, "password");
        System.out.println(GSONUtil.toJSONDefault(p, true));
       // p.setSalt(CryptoUtil.generateRandomHashedBytes());
    }



    @Test
    public void testBCrypt() throws NoSuchAlgorithmException {

        String salt = BCrypt.gensalt(10);

        System.out.println(Arrays.toString(SharedStringUtil.parseString(salt, "\\$", true)));
        System.out.println("Salt:" + salt + " " + salt.length()+" "  + salt.split("\\$")[3].length());

        BCryptHash bCrypted = HashUtil.toBCryptHash(10, "password");//BCrypt.hashpw("password", salt);
        System.out.println(bCrypted + " " + HashUtil.isBCryptPasswordValid("password", bCrypted.toString()));

        // generated online with clear password = password
        String bcryptHash = "$2y$10$yvHLPVjv6yF1MhI95Tw.TellvsfC7TDxYoNBxK8ksuEga8xkpHk7C";
        PasswordDAO parsed = PasswordDAO.fromCanonicalID(bcryptHash);
        System.out.println(bcryptHash + " " + HashUtil.isBCryptPasswordValid("password", bcryptHash));
        System.out.println(bCrypted);
        Assertions.assertTrue(HashUtil.isPasswordValid(parsed, "password"));


        PasswordDAO bPassword = HashUtil.toPassword(CryptoConst.HASHType.BCRYPT, 0, 10, "password");
        System.out.println(GSONUtil.toJSONDefault(bPassword));
        Assertions.assertTrue(HashUtil.isPasswordValid(bPassword, "password"));

        HashUtil.validatePassword(bPassword, "password");

        PasswordDAO passwordFromCanonicalID = PasswordDAO
                .fromCanonicalID(bPassword.toCanonicalID());

        System.out.println(GSONUtil.toJSONDefault(passwordFromCanonicalID));
        Assertions.assertTrue(HashUtil.isPasswordValid(passwordFromCanonicalID, "password"));




        String salted = BCrypt.gensalt(9);
        String hashedPW = BCrypt.hashpw("password", salted);
        BCrypt.checkpw("password",hashedPW);
        bPassword = HashUtil.toPassword(CryptoConst.HASHType.BCRYPT, 0, 9, "password");
        System.out.println(GSONUtil.toJSONDefault(bPassword));
        Assertions.assertTrue(HashUtil.isPasswordValid(bPassword, "password"));
    }
}
