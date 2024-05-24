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
        Appointment appointment = TaskUtil.defaultTaskScheduler().queue(500, ()-> System.out.println("**********pikaboooo******* " + Thread.currentThread()));
        System.out.println("Cancel :" + appointment.cancel()  + " exec.count: " + appointment.execCount());
        assert appointment.reset(true);
        assert appointment.cancel();
        System.out.println("isClosed :" + appointment.isClosed() + " exec.count: " + appointment.execCount());
        TaskUtil.sleep(100);

        assert appointment.reset(true);
        TaskUtil.waitIfBusy(25);
        delta = System.currentTimeMillis() - delta;
        System.out.println("pikabo took: " + Const.TimeInMillis.toString(delta));
        System.out.println("isClosed :" + appointment.isClosed() + " exec.count: " + appointment.execCount());

        assert !appointment.reset(true);
        TaskUtil.waitIfBusy(25);

    }
}
