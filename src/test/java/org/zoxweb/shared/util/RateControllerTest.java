package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.task.TaskUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RateControllerTest {

    @Test
    public void tpsTest()
    {
        RateController controller = new RateController(300, TimeUnit.MINUTES);

        TaskUtil.sleep(1000);

        System.out.println(controller + " " + controller.nextDelay());
        TaskUtil.sleep(1000);

        List<Long> vals = new ArrayList<>();

        for(int i = 0; i < 100; i++)
            vals.add(controller.nextDelay());


        for(int i = 0; i < vals.size(); i+=2)
        {

            assert(vals.get(i+1) - vals.get(i) == controller.getDeltaInMillis());
        }


        System.out.println("Delay: "  + vals);


        TaskUtil.sleep(500);

        System.out.println(controller + " " + controller.nextDelay()  + " " + (controller.getNextTime() - System.currentTimeMillis()));

        TaskUtil.sleep(1100);
        System.out.println(controller + " " + controller.nextDelay()  + " " + (controller.getNextTime() - System.currentTimeMillis()));
    }

    @Test
    public void tpsZeroTest()
    {
        RateController controller = new RateController(1000, TimeUnit.MINUTES);
        List<Long> vals = new ArrayList<>();

        for(int i = 0; i < 100; i++)
            vals.add(controller.nextDelay());


        System.out.println("Zero Delay: "  + vals);
        System.out.println("Zero Delay: "  + controller);


        for(int i = 0; i < vals.size(); i+=2)
        {

            assert(vals.get(i+1) - vals.get(i) == controller.getDeltaInMillis());
        }

    }


    @Test
    public void tspRateDelta()
    {

        RateController controller = new RateController(1000, TimeUnit.SECONDS);
        long[] tpses = {0, 1, 5, 7, 9, 10, 100, 250, 300, 400, 500, 750, 1000, 2000};

        String[] rates ={"500/s", "250/secs", "1000/min", "50/hour", "250/min"};

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
}
