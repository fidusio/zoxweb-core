package org.zoxweb.server.task;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.Appointment;
import org.zoxweb.shared.util.Const;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AppointmentResetTest {

    @AfterAll
    public static void done()
    {

        System.out.println("All test are completed");
        TaskUtil.waitIfBusyThenClose(50);
    }

    @Test
    public void test()
    {
        System.out.println("testing reset");


        long delta = System.currentTimeMillis();
        Appointment appointment = TaskUtil.defaultTaskScheduler().queue(50, ()-> System.out.println("**********peek a boo******* " + Thread.currentThread()));
        assert appointment.cancel();
        System.out.println("isClosed :" + appointment.isClosed()  + " exec.count: " + appointment.execCount() + " delay : " + appointment.getDelay(TimeUnit.MILLISECONDS));
        assert appointment.reset(true);
        assert appointment.cancel();
        assert !appointment.cancel();
        System.out.println("isClosed :" + appointment.isClosed()  + " exec.count: " + appointment.execCount() + " delay : " + appointment.getDelay(TimeUnit.MILLISECONDS));
        long sleepDelta = System.currentTimeMillis();
        TaskUtil.sleep(200);
        sleepDelta = System.currentTimeMillis() - sleepDelta;
        System.out.println("sleepDelta: " + Const.TimeInMillis.toString(sleepDelta));

        System.out.println("isClosed :" + appointment.isClosed()  + " exec.count: " + appointment.execCount() + " delay : " + appointment.getDelay(TimeUnit.MILLISECONDS));
        assert appointment.reset(true);
        System.out.println("isClosed :" + appointment.isClosed()  + " exec.count: " + appointment.execCount() + " delay : " + appointment.getDelay(TimeUnit.MILLISECONDS));
        TaskUtil.waitIfBusy(25);
        delta = System.currentTimeMillis() - delta;
        System.out.println("pikabo took: " + Const.TimeInMillis.toString(delta));
        System.out.println("isClosed :" + appointment.isClosed() + " exec.count: " + appointment.execCount());

        assert !appointment.reset(true);
        TaskUtil.waitIfBusy(25);

    }

    @Test
    public void scheduleWithFixedDelay()
    {

        long initialDelay = 200;
        long delay = 100;
        AtomicInteger ai = new AtomicInteger();
        TaskSchedulerProcessor tsp = new TaskSchedulerProcessor(TaskUtil.defaultTaskProcessor());
        long delta = System.currentTimeMillis();
        ScheduledFuture<?> sc = tsp.scheduleWithFixedDelay(()->{System.out.println(ai.incrementAndGet() + " repeat " + (System.currentTimeMillis() - delta) );}, initialDelay, delay, TimeUnit.MILLISECONDS);

        TaskUtil.sleep(initialDelay + (delay*60));

        tsp.close();
        System.out.println( "ExecCount: " + ((TaskSchedulerProcessor.TaskSchedulerAppointment)sc).execCount());
    }

    @Test
    public void scheduleAtFixedRate()
    {

        long initialDelay = 300;
        long delay = 100;
        AtomicInteger ai = new AtomicInteger();
        TaskSchedulerProcessor tsp = new TaskSchedulerProcessor(TaskUtil.defaultTaskProcessor());

        long delta = System.currentTimeMillis();
        ScheduledFuture<?> sc = tsp.scheduleAtFixedRate(()->{System.out.println(ai.incrementAndGet() + " repeat " + (System.currentTimeMillis() - delta) );}, initialDelay, delay, TimeUnit.MILLISECONDS);

        TaskUtil.sleep(initialDelay + (delay*60));
        tsp.close();
        System.out.println( "ExecCount: " + ((TaskSchedulerProcessor.TaskSchedulerAppointment)sc).execCount());
    }
}
