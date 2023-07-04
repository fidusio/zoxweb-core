
package org.zoxweb.server.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentTest {


    @Test
    public void semaphore() throws InterruptedException {
        Semaphore semaphore = new Semaphore(5);
        Lock lock = new ReentrantLock();

        for(int i=0; i < 10; i++)
        {

            if (semaphore.availablePermits() == 0)
                semaphore.release();

            semaphore.acquire();
            lock.lock();
            System.out.println(i + " " + semaphore.availablePermits());
        }

    }
}
