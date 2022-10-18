package org.zoxweb.server.security;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.SharedStringUtil;

import java.util.Arrays;

public class BCryptTest {

    @Test
    public void testBCrypt()
    {

        String salt = BCrypt.gensalt(10);

        System.out.println(Arrays.toString(SharedStringUtil.parseString(salt, "\\$", true)));
        System.out.println("Salt:" + salt + " " + salt.split("\\$")[3].length());

        String bCrypted = BCrypt.hashpw("password", salt);
        System.out.println(bCrypted + " " + BCrypt.checkpw("password", bCrypted));
        System.out.println(new BCrypt.BCryptHash(bCrypted));

        ;
    }

}
