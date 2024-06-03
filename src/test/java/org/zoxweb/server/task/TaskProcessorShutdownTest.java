package org.zoxweb.server.task;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.util.Const;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskProcessorShutdownTest {

    static final LogWrapper log = new LogWrapper(TaskProcessorShutdownTest.class);

    public static void testTaskProcessorShutdown()
    {
        TaskProcessor tp = TaskUtil.defaultTaskProcessor();
        AtomicInteger counter = new AtomicInteger();
        int max = 50;
        long delta = System.currentTimeMillis();
        for (int i = 0; i < max; i++)
        {
            tp.execute(()-> {
                try
                {
                    System.out.println(counter.incrementAndGet() + "   " + Thread.currentThread());
                    TaskUtil.sleep(500);

                    //log.getLogger().info(ival + ": " + System.nanoTime());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            });

        }



        delta = TaskUtil.completeTermination(tp, 25) - delta;

        assert max == counter.get();
        System.out.println(tp.isClosed() + " it took: " + Const.TimeInMillis.toString(delta));


        //

        try {
            log.getLogger().info("awaite termination:" +tp.awaitTermination(1000, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String ...args)
    {
        testTaskProcessorShutdown();

    }
}
