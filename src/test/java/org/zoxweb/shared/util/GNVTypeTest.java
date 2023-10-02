package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;

public class GNVTypeTest
{
    @Test
    public void testGVN()
    {
        String[] tokens =
                {
                        "int:iVar",
                        "float:fVar",
                        "byte[]:bArray",
                        "Double:dVar"
                };

        for(String token: tokens)
        {
            System.out.println(Const.GNVType.toGNVTypeName(':', token));
        }
    }
}
