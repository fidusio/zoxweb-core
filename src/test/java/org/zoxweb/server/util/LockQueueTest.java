/*
 * Copyright (c) 2012-2015 ZoxWeb.com LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.server.util;

import org.zoxweb.shared.util.SimpleQueue;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;

public class LockQueueTest {

    private static LockQueue lq = new LockQueue(5);

    static class LockCleaner
            implements Runnable {

        ArrayList<Lock> locks = new ArrayList<Lock>();
        SimpleQueue<Object> sq = new SimpleQueue<Object>();

        public LockCleaner() {
            new Thread(this).start();
        }

        public void addLock() {
            sq.queue(new Object());

            synchronized (this) {
                notify();

                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void run() {

            while (locks.size() < 5) {

                while (sq.dequeue() == null && locks.size() < 5) {

                    synchronized (this) {
                        try {
                            wait(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                locks.add(lq.dequeueLock());

                synchronized (this) {
                    notify();
                }

                System.out.println(Thread.currentThread() + " " + locks.size());
            }

            for (Lock lock : locks) {
                try {
                    lq.queueLock(lock);
                    System.out.println("removing " + Thread.currentThread() + " " + lock);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            System.out.println("Run finish " + Thread.currentThread());
        }
    }

    public static void main(String[] args) {
        LockCleaner lc = new LockCleaner();

        for (int i = 0; i < 10; i++) {
            lc.addLock();
            System.out.println("[" + i + "]");

            if (((i + 1) % 5) == 0 && (i + 1) != 10) {
                lc = new LockCleaner();
            }
        }

    }
}