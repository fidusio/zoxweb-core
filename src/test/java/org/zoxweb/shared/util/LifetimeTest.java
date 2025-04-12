package org.zoxweb.shared.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.task.TaskUtil;

public class LifetimeTest
{
    @Test
    public void testLifetime()
    {
        Lifetime lt = new Lifetime(System.currentTimeMillis(), 10, null, Const.TimeInMillis.SECOND.MILLIS*5);
        System.out.println(lt);
        for (int i=0; i < lt.getMaxUse(); i++)
        {
            lt.incUsage();
            System.out.println(lt);
        }

        assert lt.isClosed();

    }

    @Test
    public void testDelayLifetime()
    {
        Lifetime lt = new Lifetime(System.currentTimeMillis(), 10, null, Const.TimeInMillis.MILLI.MILLIS*100);
        System.out.println(lt);
        for (int i=0; i < lt.getMaxUse() - 2; i++)
        {
            lt.incUsage();
            System.out.println(lt + "nextWait: " + lt.nextWait());

        }

        assert !lt.isClosed();
        System.out.println("next wait: " + lt.nextWait());
        TaskUtil.sleep(lt.nextWait() + lt.getDelayInMillis());
        System.out.println(lt);
        assert lt.isClosed();



    }

    @Test
    public void testExpireLifetime()
    {
        Lifetime lt = new Lifetime(System.currentTimeMillis(), 10, null, Const.TimeInMillis.MILLI.MILLIS*200);
        System.out.println("isMAxUsed : " + lt.isExpired());
        System.out.println("nextWait : " + lt.nextWait());
        lt.close();
        assert lt.isClosed();
        System.out.println("next wait: " + Assertions.assertThrows(IllegalStateException.class, lt::nextWait));

    }
}
