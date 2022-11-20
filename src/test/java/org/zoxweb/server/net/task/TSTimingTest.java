package org.zoxweb.server.net.task;

//import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.util.Const;



public class TSTimingTest {

    @Test
    public void testPrecision()
    {
        System.out.println(Const.TimeInMillis.toString(TaskUtil.getDefaultTaskScheduler().waitTime()) + " " + TaskUtil.getDefaultTaskScheduler().pendingTasks());
        TaskUtil.getDefaultTaskScheduler().queue(Const.TimeInMillis.SECOND.MILLIS*2, new Runnable() {
            @Override
            public void run() {

            }
        });
        System.out.println(Const.TimeInMillis.toString(TaskUtil.getDefaultTaskScheduler().waitTime()) + " " + TaskUtil.getDefaultTaskScheduler().pendingTasks());
        TaskUtil.getDefaultTaskScheduler().queue(Const.TimeInMillis.SECOND.MILLIS, new Runnable() {
            @Override
            public void run() {

            }
        });
        System.out.println(Const.TimeInMillis.toString(TaskUtil.getDefaultTaskScheduler().waitTime()) + " " + TaskUtil.getDefaultTaskScheduler().pendingTasks());
        TaskUtil.waitIfBusyThenClose(250);
    }
}
