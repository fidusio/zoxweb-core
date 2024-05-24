package org.zoxweb.server.task;

import org.zoxweb.server.task.TaskUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class FutureTest {

    public static void main(String ...args)
    {
        CompletableFuture<String> cf = CompletableFuture.supplyAsync(()->
        {

            try
            {
                TimeUnit.SECONDS.sleep(1);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread() + ":" + TaskUtil.defaultTaskProcessor().availableExecutorThreads());
            //throw new  RuntimeException("tata");
            return "batata";
        }, TaskUtil.defaultTaskProcessor());
        cf.handleAsync(( str, e)->
        {
            System.out.println(Thread.currentThread() + ":" + TaskUtil.defaultTaskProcessor().availableExecutorThreads());
            System.out.println("str:" + str + " e:" + e);
            return str;
        },TaskUtil.defaultTaskProcessor());

        System.out.println("Done");
        TaskUtil.waitIfBusyThenClose(50);
        System.out.println(cf.isDone() + " " + cf.isCompletedExceptionally());

    }


}
