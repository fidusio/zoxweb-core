package org.zoxweb.server.task;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleTaskSchedulerTest {

    public static class Test
        implements Runnable
    {
        static AtomicInteger counter = new AtomicInteger(0);

        final long ts;
        final long val;
        final String tag;
        final int index  = counter.incrementAndGet();
        public Test(String tag, int val)
        {
            this.tag = tag;
            ts = System.currentTimeMillis();
            this.val = val;
        }

        public void run()
        {
            System.out.println("[" + index +"] " + tag + ": "+ val + " " + (System.currentTimeMillis() - ts));
        }

    }

    public static void main(String ...args)
    {
        for(int i = 0; i < 100; i++)
        {
            int val = i + 1000;
            ScheduledFuture<?> sf = TaskUtil.simpleTaskScheduler().schedule(new Test("first", val), val, TimeUnit.MILLISECONDS);
            if(i%2 != 0)
                sf.cancel(true);
        }


        for(int i = 0; i < 50; i++)
        {
            ScheduledFuture<?> sf = TaskUtil.simpleTaskScheduler().schedule(new Test("second", i), i, TimeUnit.MILLISECONDS);
            if(i%2 == 0)
                sf.cancel(true);
        }

        TaskUtil.waitIfBusyThenClose(50);

    }
}
