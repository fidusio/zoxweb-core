package org.zoxweb.server.task;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.task.CallableConsumer;
import org.zoxweb.shared.util.Const;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorServiceTest {

    static final LogWrapper log = new LogWrapper(ExecutorServiceTest.class);

    static AtomicInteger runnableCounter = new AtomicInteger();
    static AtomicInteger callableCounter = new AtomicInteger();
    static AtomicInteger acceptCounter = new AtomicInteger();
    static AtomicInteger exceptionCounter = new AtomicInteger();


    public static void testRunnable(ExecutorService es, int max, long delay)
    {
        for (int i = 0; i < max; i++)
        {
            es.execute(()-> {


                    runnableCounter.incrementAndGet();
                    TaskUtil.sleep(delay);
            });
        }
    }


    public static void testCallableConsumer(ExecutorService es, int max, long delay)
    {
        AtomicInteger counter = new AtomicInteger();

        for (int i = 0; i < max; i++)
        {

            es.submit(new CallableConsumer<Integer>() {
                @Override
                public Integer call() throws Exception {
                    int ret = callableCounter.incrementAndGet();
                    if(ret%2 == 0)
                        throw new RuntimeException("time for Exception counter " + ret);

                    TaskUtil.sleep(delay);
                    return ret;
                }

                @Override
                public void accept(Integer counter)
                {
                    if(counter != 0)
                        acceptCounter.incrementAndGet();
                }
                @Override
                public void exception(Throwable e)
                {
                    exceptionCounter.incrementAndGet();
                }

            });
        }
    }

    public static void main(String ...args)
    {
        ExecutorService es = TaskUtil.defaultTaskProcessor();
        long delta = System.currentTimeMillis();
        int max = 100;
        testCallableConsumer(es, max, 50);
        testRunnable(es, max, 100);

        delta = TaskUtil.completeTermination(es, 25) - delta;

        System.out.println("max: " + max + " runnable counter: " + runnableCounter.get() +
                " callable counter: " + callableCounter.get() + " accept counter: " + acceptCounter.get() +
                " exception counter: " + exceptionCounter.get());

        log.getLogger().info(" Executor Service is terminated:" + es.isTerminated() + " it took: " + Const.TimeInMillis.toString(delta));

    }
}
