/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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

import org.zoxweb.shared.util.ArrayQueue;
import org.zoxweb.shared.util.SharedUtil;


/**
 * The UQueueBound class is a bounded queue with a high and low marks. The
 * purpose of this type of queue is not to overflow the system by forcing the
 * queuing thread to wait when the high mark is reached till the dequeuing
 * thread reaches the low mark. <br>
 * Note: this class must be used by 2 threads simultaneously a
 * <code>queuing</code> thread and a <code>dequeuing</code>thread. The
 * <code>dequeuing</code> thread must be running continuously otherwise the
 * <code>dequeuing</code> will be pending till the dequeuing process starts
 * again.
 */
public class ThresholdQueue<O>
        extends ArrayQueue<O> {

    private final int threshold;
    private boolean thresholdEnabled = false;


    public ThresholdQueue(int capacity) {
        this(capacity / 2, capacity);
    }

    /**
     * Create a bounded queue based on a lowMark and highMark parameters.
     *
     * @param threshold of the queue size.
     * @param capacity  of the queue size.
     * @throws IllegalArgumentException if the lowMark is greater or equal to highMark or lowMark is less than 0 or highMark is less than 0.
     */
    public ThresholdQueue(int threshold, int capacity)
            throws IllegalArgumentException {
        super(capacity);
        if (capacity <= threshold || capacity < 0 || threshold < 0) {
            throw new IllegalArgumentException("Invalid queue parameters "
                    + " capacity " + capacity + " lowThreshold " + threshold);
        }


        this.threshold = threshold;
    }

    /**
     * This method will dequeue and Object, if the queue is empty it will return
     * null.
     */
    public synchronized O dequeue() {
        O ret = int_dequeue();
        if (thresholdEnabled && size <= threshold) {
            thresholdEnabled = false;
            notifyAll();
        }
        return ret;
    }

    /**
     * This method will add an object to the queue if the high mark is
     * reached the calling thread will block till the size of the queue = to the
     * lowMark.
     *
     * @param toQueue the object.
     */
    public synchronized boolean queue(O toQueue) {
        if (toQueue == null)
            throw new NullPointerException("Can't queue a null object");
        if (thresholdEnabled) {
            try {
                while (thresholdEnabled)
                    wait(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int_queue(toQueue);

        if (size == array.length) {
            thresholdEnabled = true;
        }
        return true;

    }

    /**
     * This method will return a string containing the size, highMark,
     * lowMark, and boundMode results.
     */
    @Override
    public String toString() {
        return SharedUtil.toCanonicalID(',', array.length, threshold, size(), thresholdEnabled);
    }


    /**
     * @return the low mark of the queue.
     */
    public int getThreshold() {
        return threshold;
    }

}