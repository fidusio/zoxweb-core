package org.zoxweb.server.net.task;

//import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.util.Const;



public class TSTimingTest {

    @Test
    public void testPrecision()
    {
        System.out.println(Const.TimeInMillis.toString(TaskUtil.defaultTaskScheduler().waitTime()) + " " + TaskUtil.defaultTaskScheduler().pendingTasks());
        TaskUtil.defaultTaskScheduler().queue(Const.TimeInMillis.SECOND.MILLIS*2, new Runnable() {
            @Override
            public void run() {

            }
        });
        System.out.println(Const.TimeInMillis.toString(TaskUtil.defaultTaskScheduler().waitTime()) + " " + TaskUtil.defaultTaskScheduler().pendingTasks());
        TaskUtil.defaultTaskScheduler().queue(Const.TimeInMillis.SECOND.MILLIS, new Runnable() {
            @Override
            public void run() {

            }
        });
        System.out.println(Const.TimeInMillis.toString(TaskUtil.defaultTaskScheduler().waitTime()) + " " + TaskUtil.defaultTaskScheduler().pendingTasks());
        TaskUtil.waitIfBusyThenClose(250);
    }
}
