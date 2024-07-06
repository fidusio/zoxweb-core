package org.zoxweb.shared.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.task.TaskUtil;

import java.util.ArrayList;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class RateControllerTest {

    @Test
    public void tpsTest()
    {
        RateController controller = new RateController("test", 300, TimeUnit.MINUTES);

        TaskUtil.sleep(1000);

        System.out.println(controller + " " + controller.nextWait());
        TaskUtil.sleep(1000);

        List<Long> vals = new ArrayList<>();

        for(int i = 0; i < 100; i++)
            vals.add(controller.nextWait());
        System.out.println(vals);


        for(int i = 0; i < vals.size(); i+=2)
        {

            assert(vals.get(i+1) - vals.get(i) == controller.getDeltaInMillis());
        }



        TaskUtil.sleep(500);

        System.out.println(controller + " " + controller.nextWait()  + " " + (controller.getNextTime() - System.currentTimeMillis()));

        TaskUtil.sleep(1100);
        System.out.println(controller + " " + controller.nextWait()  + " " + (controller.getNextTime() - System.currentTimeMillis()));
    }

    @Test
    public void tpsZeroTest()
    {
        RateController controller = new RateController("test", 1000, TimeUnit.MINUTES);
        List<Long> vals = new ArrayList<>();
        long ts = System.currentTimeMillis();
        for(int i = 0; i < 100; i++)
            vals.add(controller.nextWait());

        System.out.println(vals);


        for(int i = 0; i < vals.size(); i+=2)
        {

            assert(vals.get(i+1) - vals.get(i) == controller.getDeltaInMillis());
        }
        TaskUtil.sleep(controller.getDeltaInMillis());
        System.out.println("Zero Delay: "  + vals);
        System.out.println("Zero Delay: "  + controller);
        ts = System.currentTimeMillis() - ts;
        System.out.println(controller.nextWait() + " delta " + ts);

    }


    @Test
    public void tspRateDelta()
    {

        RateController controller = new RateController("test", 1000, TimeUnit.SECONDS);
        long[] tpses = {0, 1, 5, 7, 9, 10, 100, 250, 300, 400, 500, 750, 1000, 2000};

        String[] rates ={"500/s", "250/secs", "1000/min", "50/hour", "250/min", "0/sec"};

        for(long tps: tpses)
        {
            controller.setRate(tps, TimeUnit.SECONDS);
            System.out.println("tps: " +  tps + " delta: " + controller.getDeltaInMillis() + " in millis");
        }


        for(String rate: rates)
        {
            controller.setRate(rate);
            System.out.println("rate: " +  rate + " delta: " + controller.getDeltaInMillis() + " tps: " +controller.getTPS());
        }
    }

    @Test
    public void testRateZero()
    {
        RateController controller = new RateController("test", "0/min");
        System.out.println(controller);

        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, controller::nextWait);

        System.out.println("Exception: " + e + " tps asLong " + controller.getTPSAsLong());
        controller.setRate("1000/min");
        TaskUtil.waitIfBusyThenClose(controller.getDeltaInMillis());

        System.out.println("tps asLong " + controller.getTPSAsLong() + " " + controller.getTPS() + " " + controller.nextWait() + " " + controller.nextWait());


    }

    @Test
    public void testRateDelay()
    {
        String[] rates={
                "10000/min",
                "500/sec"
        };

        for (String rate: rates)
        {
            RateController rc = new RateController(rate, rate);
            System.out.println(rc);
        }
    }


    @Test
    public void counterTypeTest()
    {
        RateController controller = new RateController("test", 10, TimeUnit.SECONDS);
        controller.setRCType(RateController.RCType.COUNTER);
        List<Long> vals = new ArrayList<>();
        long ts = System.currentTimeMillis();
        for(int i = 0; i < 100; i++)
            vals.add(controller.nextWait());
        System.out.println(vals);


        TaskUtil.sleep(5000);
        for(int i = 0; i < 100; i++)
            vals.add(controller.nextWait());

        System.out.println(vals);
        TaskUtil.sleep(15500);
        vals.clear();
        for(int i = 0; i < 100; i++)
            vals.add(controller.nextWait());
        System.out.println(vals);




        ts = System.currentTimeMillis() - ts;
        System.out.println(controller.nextWait() + " delta " + ts);
    }

    @Test
    public void bellowMillis()
    {
        RateController rc = new RateController("BellowMillis", "4000/s");
        List<Long> list = new ArrayList<>();
        for (int i=0; i < 1000; i++)
        {
            list.add(rc.nextWait());
        }

        System.out.println(list);
        System.out.println(rc);
    }
}
