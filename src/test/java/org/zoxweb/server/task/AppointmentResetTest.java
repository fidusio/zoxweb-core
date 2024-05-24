package org.zoxweb.server.task;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.Appointment;
import org.zoxweb.shared.util.Const;

public class AppointmentResetTest {

    @AfterAll
    public static void done()
    {
        TaskUtil.waitIfBusyThenClose(50);
    }

    @Test
    public void test()
    {
        System.out.println("testing reset");


        long delta = System.currentTimeMillis();
        Appointment appointment = TaskUtil.defaultTaskScheduler().queue(50, ()-> System.out.println("**********peek a boo******* " + Thread.currentThread()));
        assert appointment.cancel();
        System.out.println("isClosed :" + appointment.isClosed()  + " exec.count: " + appointment.execCount());
        assert appointment.reset(true);
        assert appointment.cancel();
        assert !appointment.cancel();
        System.out.println("isClosed :" + appointment.isClosed() + " exec.count: " + appointment.execCount());
        long sleepDelta = System.currentTimeMillis();
        TaskUtil.sleep(100);
        sleepDelta = System.currentTimeMillis() - sleepDelta;
        System.out.println("sleepDelta: " + Const.TimeInMillis.toString(sleepDelta));

        assert appointment.reset(true);
        TaskUtil.waitIfBusy(25);
        delta = System.currentTimeMillis() - delta;
        System.out.println("pikabo took: " + Const.TimeInMillis.toString(delta));
        System.out.println("isClosed :" + appointment.isClosed() + " exec.count: " + appointment.execCount());

        assert !appointment.reset(true);
        TaskUtil.waitIfBusy(25);

    }
}
