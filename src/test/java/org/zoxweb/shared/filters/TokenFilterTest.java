package org.zoxweb.shared.filters;

import org.junit.jupiter.api.Test;

public class TokenFilterTest {
    @Test
    public void colonPositive()
    {
        String[] tokens = {
                "heLlo",
                "fooD:batata",
        };
        TokenFilter[] tfs = {TokenFilter.UPPER_COLON, TokenFilter.LOWER_COLON, TokenFilter.COLON};
        for (String token: tokens)
        {
            for (TokenFilter tf : tfs)
            {
                assert(tf.isValid(token));
                System.out.println(tf.toCanonicalID() + "*" + token + "*"  + tf.validate(token) + "*");
            }
        }
    }

    @Test
    public void colonNegative()
    {
        String[] tokens = {
                "   ",
                null,
                ":batata",
        };
        TokenFilter[] tfs = {TokenFilter.UPPER_COLON, TokenFilter.LOWER_COLON, TokenFilter.COLON};
        for (String token: tokens)
        {
            for (TokenFilter tf : tfs)
            {
                assert(!tf.isValid(token));
                try
                {
                    boolean fail = false;
                    tf.validate(token);
                    assert(fail);
                }
                catch(Exception e){System.out.println(tf.toCanonicalID() + " FAILED*" + token +"*");}
            }


        }
    }
}
