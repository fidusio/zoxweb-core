package org.zoxweb.shared.util;

public class ArrayQueue<O>
implements SimpleQueueInterface<O>{
    protected volatile Object[] array;
    protected int head = 0, end = 0;
    protected int size = 0;
    protected long totalDequeued=0, totalQueued=0;

    public ArrayQueue(int capacity)
    {
        array = new Object[capacity];
    }


    @Override
    public synchronized void clear() {
        head = 0;
        end = 0;
        size = 0;
        for(int i = 0; i < array.length; i++)
            array[i] = null;
    }

    public int size()
    {
        return size;
    }

    public synchronized boolean queue(O toQueue)
    {
        if(toQueue == null)
            throw new NullPointerException("Can't queue a null object");
        return int_queue(toQueue);
    }


    protected  boolean int_queue(O toQueue)
    {

        if (size != array.length) {
            if (end == array.length) {
                end = 0;
            }
         array[end] = toQueue;
         end++;
         size++;
         totalQueued++;
         return true;
        }

        return false;
    }




    public synchronized O dequeue()
    {
        return int_dequeue();
    }
    @SuppressWarnings("unchecked")
    protected O int_dequeue()
    {
        if(size != 0)
        {
            O ret = (O)array[head];
            array[head] = null;
            head++;
            if(head == array.length)
            {
                head = 0;
            }
            size--;
            totalDequeued++;
            return ret;
        }

        return null;
    }



    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public long totalQueued() {
        return totalQueued;
    }

    @Override
    public long totalDequeued() {
        return totalDequeued;
    }

    @Override
    public int capacity() {
        return array.length;
    }

    @Override
    public boolean contains(O o) {
        return false;
    }




    public String toString()
    {
        return "[" + SharedUtil.toCanonicalID(',', array.length, head, end, size()) + "]";
    }


}
