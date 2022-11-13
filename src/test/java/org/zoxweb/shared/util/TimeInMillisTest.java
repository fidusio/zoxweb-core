package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;

import java.util.Date;

public class TimeInMillisTest {
    @Test
    public void testTimeToString()
    {
        long ts = System.currentTimeMillis();
        long[] times = {
                0,
                50,
                Const.TimeInMillis.MILLI.MILLIS,
                Const.TimeInMillis.SECOND.MILLIS,
                Const.TimeInMillis.MINUTE.MILLIS,
                Const.TimeInMillis.HOUR.MILLIS,
                Const.TimeInMillis.DAY.MILLIS,
                Const.TimeInMillis.WEEK.MILLIS,
        };

        for(long t : times)
            System.out.println(Const.TimeInMillis.toString(t));
        System.out.println();
        for(long t : times)
            System.out.println(Const.TimeInMillis.toString(t*25));


        System.out.println(Const.TimeInMillis.toString(Const.TimeInMillis.WEEK.MILLIS + Const.TimeInMillis.DAY.MILLIS +
                Const.TimeInMillis.HOUR.MILLIS*26 + Const.TimeInMillis.MINUTE.MILLIS*70 +50));


        System.out.println(Const.TimeInMillis.toString(System.currentTimeMillis()) + " " + new Date(0));
        System.out.println(Const.TimeInMillis.toString(-System.currentTimeMillis()) + " " + new Date(0));
        ts = System.currentTimeMillis() - ts;
        System.out.println(Const.TimeInMillis.toString(ts) + " " + ts);

    }
}
