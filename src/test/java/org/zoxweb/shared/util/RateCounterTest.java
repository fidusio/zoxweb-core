package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;

public class RateCounterTest {

    @Test
    public void simple()
    {

        RateCounter rateCounter = new RateCounter(new NamedDescription("test"));
        RateController rateController = new RateController(new NamedDescription("test"), "1000/s");
        for(int j=0; j<10; j++) {
            long ts = System.currentTimeMillis();
            for (int i = 0; i < 10000; i++) {
                rateController.nextDelay();
            }

            ts = System.currentTimeMillis() - ts;
            rateCounter.register(ts);
            System.out.println(j + ": " +rateCounter.average() + " " + rateCounter.average(Const.TimeInMillis.SECOND.MILLIS));
        }

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

    }
}
