package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class TypedCacheTest {
    @Test
    public void test()
    {
        TypedCache cache = new TypedCache();
        cache.addObject("RC", new RateController("n1", "10/s"));
        RateController rc =  cache.lookupObject("RC", "n1");
        rc.nextWait();
        rc.nextWait();
        rc.nextWait();
        System.out.println(rc);
        RateController[] rcAll = cache.getValues("RC").toArray(new RateController[0]);
        System.out.println(Arrays.toString(rcAll));
        cache.removeObject("RC", "n1");

        rcAll = cache.getValues("RC").toArray(new RateController[0]);
        System.out.println(Arrays.toString(rcAll));
    }
}
