package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;

public class RateCounterTest {

    @Test
    public void simple()
    {

        RateCounter rateCounter = new RateCounter("test");
        RateController rateController = new RateController("test", "100/s");

        for(int j=0; j<10; j++) {
            long ts = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                rateController.nextWait();
            }

            ts = System.currentTimeMillis() - ts;
            rateCounter.register(ts);
            System.out.println(j + ": " +rateCounter.average() + " " + rateCounter.average(Const.TimeInMillis.SECOND.MILLIS));
        }
        System.out.println(rateController);
        rateController = new RateController("test", "100/s");
        for(int i = 0; i <= 200; i++)
        {
            long waitTime = rateController.nextWait();
            if(i%25 == 0)
                System.out.println( i + " " +rateController);

        }

        TaskUtil.sleep(2000);
        System.out.println("last: " + rateController);



    }
    @Test
    public void timeStampTest()
    {
        RateCounter rateCounter = new RateCounter("timestamp");
        long millis = System.currentTimeMillis();
        millis = rateCounter.registerTimeStamp(millis);
        System.out.println(millis + ": " +rateCounter.average() + " " + rateCounter.average(Const.TimeInMillis.SECOND.MILLIS));
        rateCounter.reset();
        long nanos = System.nanoTime();
        nanos = rateCounter.registerTimeStamp(false, nanos, 1);
        System.out.println(nanos + ": " +rateCounter.average() + " " + rateCounter.average(Const.TimeInMillis.SECOND.MILLIS));
        rateCounter.inc().inc(5);
    }

    @Test
    public void rate()
    {
        RateCounter rc = new RateCounter("test");
        rc.register(100);
        System.out.println(rc + " " + rc.average() + " " + rc.rate(Const.TimeInMillis.SECOND.MILLIS));
        rc.register(1000);
        System.out.println(rc + " " + rc.average() + " " + rc.rate(Const.TimeInMillis.SECOND.MILLIS));
        rc.register(2000);
        System.out.println(rc + " " + rc.average() + " " + rc.rate(Const.TimeInMillis.SECOND.MILLIS));
        for(int i =0; i < 100; i++)
            rc.register(20);

        System.out.println(rc + " " + rc.average() + " " + rc.rate());
        System.out.println(GSONUtil.toJSONDefault(rc, true));

    }
}
