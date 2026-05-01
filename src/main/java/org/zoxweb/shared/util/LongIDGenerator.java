package org.zoxweb.shared.util;


import java.util.concurrent.atomic.AtomicLong;

public class LongIDGenerator implements IDGenerator<Long, Long> {

    public static final LongIDGenerator DEFAULT = new LongIDGenerator();


    private volatile AtomicLong currentID = new AtomicLong(0);

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "LongIDGenerator";
    }

    @Override
    public Long genID() {
        // TODO Auto-generated method stub
        return nextID();
    }

    @Override
    public Long genNativeID() {
        // TODO Auto-generated method stub
        return nextID();
    }

    public long currentID() {
        return currentID.get();
    }

    public long nextID() {
        return currentID.incrementAndGet();
    }

    /**
     *
     * @param input
     * @return
     */
    @Override
    public Long decode(Long input) {
        return input;
    }

    /**
     *
     * @param input
     * @return
     */
    @Override
    public Long encode(Long input) {
        return input;
    }
}
