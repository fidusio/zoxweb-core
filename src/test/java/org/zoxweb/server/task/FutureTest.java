package org.zoxweb.server.task;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.SUS;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class FutureTest {


    @AfterAll
    static void tearDownAll() {
        // Runs once after ALL tests in this class complete
        TaskUtil.waitIfBusyThenClose(50);
        System.out.println("Tear down...");
    }

    @Test
    public void testSimple() {
        CompletableFuture<String> cf = CompletableFuture.supplyAsync(() ->
        {

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread() + ":" + TaskUtil.defaultTaskProcessor().availableExecutorThreads());
            return "batata";
        }, TaskUtil.defaultTaskProcessor());
        cf.handleAsync((str, e) ->
        {
            System.out.println(Thread.currentThread() + ":" + TaskUtil.defaultTaskProcessor().availableExecutorThreads());
            System.out.println("str:" + str + " e:" + e);
            return str;
        }, TaskUtil.defaultTaskProcessor());

        System.out.println("Done");
        TaskUtil.waitIfBusy(50);
        System.out.println(cf.isDone() + " " + cf.isCompletedExceptionally());

    }

    @Test
    public void testCompleteFullAsync()  {
        for (int i = 0; i < 100; i++) {
            int temp = i;
            CompletableFuture.supplyAsync(() -> {
                        String t = SUS.toSB(temp, true) + SUS.toSB(UUID.randomUUID(), true )+ Thread.currentThread().getName() + " " + LocalDateTime.now();
                        System.out.println("Supply: " + t);
                        return t;
                    }, TaskUtil.defaultTaskProcessor())
                    .whenCompleteAsync((result, ex) -> {
                        String t = Thread.currentThread().getName();
                        System.out.println("Callback: " + t + " (supply was: " + result + ")");
                    }, TaskUtil.defaultTaskProcessor());
        }

        TaskUtil.waitIfBusy(50);
    }
    @Test
    public void testCompletePartialAsync()  {
        for (int i = 0; i < 100; i++) {
            int temp = i;
            CompletableFuture.supplyAsync(() -> {
                        String t = SUS.toSB(temp, true) + SUS.toSB(UUID.randomUUID(), true )+ Thread.currentThread().getName() + " " + LocalDateTime.now();
                        System.out.println("Supply: " + t);
                        return t;
                    }, TaskUtil.defaultTaskProcessor())
                    .whenComplete((result, ex) -> {
                        String t = Thread.currentThread().getName();
                        System.out.println("Callback: " + t + " (supply was: " + result + ")");
                    });
        }

        TaskUtil.waitIfBusy(50);
    }


}