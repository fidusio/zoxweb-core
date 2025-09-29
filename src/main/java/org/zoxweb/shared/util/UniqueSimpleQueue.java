package org.zoxweb.shared.util;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Create a fifo queue of unique simple object
 * @param <E>
 */
public class UniqueSimpleQueue<E>
        implements SimpleQueueInterface<E> {

    private final AtomicLong totalQueued = new AtomicLong();

    private final AtomicLong totalDeQueued = new AtomicLong();
    private final LinkedHashSet<E> set = new LinkedHashSet<>();

    /**
     * Clears the queue content.
     */
    @Override
    public synchronized void clear() {
        set.clear();
    }

    /**
     * Returns the size of the queue.
     *
     * @return the size of the queue.
     */
    @Override
    public synchronized int size() {
        return set.size();
    }

    /**
     * Contract to queue an object, the object can be null.
     *
     * @param toQueue the object
     * @throws NullPointerException
     */
    @Override
    public synchronized boolean queue(E toQueue) throws NullPointerException {
        if (toQueue == null)
            return false;
        if (!contains(toQueue)) {
            set.add(toQueue);
            totalQueued.incrementAndGet();
            return true;
        }

        return false;
    }

    public synchronized E dequeue() {
        Iterator<E> it = set.iterator();
        if (!it.hasNext()) return null;
        E first = it.next();
        it.remove();
        if (first != null)
            totalDeQueued.decrementAndGet();
        return first;
    }

    /**
     * Check if the queue is empty.
     *
     * @return true if empty, false otherwise.
     */
    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    /**
     * @return the total number of object queued
     */
    @Override
    public long totalQueued() {
        return totalQueued.get();
    }

    /**
     * @return the total number of object dequeued
     */
    @Override
    public long totalDequeued() {
        return totalDeQueued.get();
    }

    /**
     * @return the maximum capacity of the implementing queue, -1 unlimited
     */
    @Override
    public int capacity() {
        return -1;
    }

    /**
     * Return true if the queue contains o.
     *
     * @param o
     * @return true if contained
     */
    @Override
    public synchronized boolean contains(E o) {
        return set.contains(o);
    }
}
