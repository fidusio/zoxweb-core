package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;

import java.util.Date;
import java.util.concurrent.TimeUnit;

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

    @Test
    public void json()
    {
        NVEnum nve = new NVEnum("seconds", TimeUnit.SECONDS);
        NVGenericMap nvgm = new NVGenericMap();
        nvgm.add(nve);
        String json = GSONUtil.toJSONDefault(nvgm);
        System.out.println(json);

        nvgm = GSONUtil.fromJSONDefault(json, NVGenericMap.class);
        System.out.println(nvgm);
        System.out.println(((TimeUnit)nvgm.getValue("seconds")).getClass());
        System.out.println(GSONUtil.toJSONDefault(nvgm));
    }

}
