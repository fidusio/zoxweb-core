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
            rateCounter.inc(ts);
            System.out.println(j + ": " +rateCounter.rate() + " " + rateCounter.rate(Const.TimeInMillis.SECOND.MILLIS));
        }

    }
}
