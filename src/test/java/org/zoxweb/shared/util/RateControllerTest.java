package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.task.TaskUtil;

import java.util.ArrayList;
import java.util.List;

public class RateControllerTest {

    @Test
    public void tpsTest()
    {
        RateController controller = new RateController((float)1.66);

        TaskUtil.sleep(1000);

        System.out.println(controller + " " + controller.nextDelay());


        List<Long> vals = new ArrayList<Long>();

        for(int i = 0; i < 100; i++)
            vals.add(controller.nextDelay());


        for(int i = 0; i < vals.size(); i+=2)
        {

            assert(vals.get(i+1) - vals.get(i) == controller.getDelta());
        }


        System.out.println("Delay: "  + vals);


        TaskUtil.sleep(500);

        System.out.println(controller + " " + controller.nextDelay()  + " " + (controller.getNextTime() - System.currentTimeMillis()));

        TaskUtil.sleep(1100);
        System.out.println(controller + " " + controller.nextDelay()  + " " + (controller.getNextTime() - System.currentTimeMillis()));


    }
}
