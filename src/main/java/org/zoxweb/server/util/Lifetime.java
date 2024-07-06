package org.zoxweb.server.util;

import org.zoxweb.shared.util.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Lifetime
    implements WaitTime<Lifetime>,
        IsClosed, Identifier<Long>
{

    private static final AtomicLong INSTANCE_COUNTER = new AtomicLong();
    private final long id = INSTANCE_COUNTER.incrementAndGet();
    private final long creationTimestamp;
    private long lastUseTimestamp = 0;
    private final long delayBetweenUsage;
    private final long maxUse;
    private long usageCounter = 0;
    private final AtomicBoolean closed = new AtomicBoolean(false);


    /**
     * Create a Lifetime object.
     * @param creationTimestamp when it was created
     * @param maxUse maximum usage 0 fore ever
     * @param tu user time to convert to millis if null delay is in millis
     * @param delayBetweenUsage delay between usage in millis 0 not used
     */
    public Lifetime(long creationTimestamp, long maxUse, TimeUnit tu, long delayBetweenUsage)
    {
        this.creationTimestamp = creationTimestamp;
        if(maxUse < 0)
            throw new IllegalArgumentException("negative maxUse: " + maxUse);
        this.maxUse = maxUse;
        if(delayBetweenUsage < 0)
            throw new IllegalArgumentException("negative delay: " + delayBetweenUsage);
        this.delayBetweenUsage = tu != null ? tu.toMillis(delayBetweenUsage) : delayBetweenUsage;
    }

    /**
     * Default to current time in millis
     */
    public Lifetime()
    {
        this(System.currentTimeMillis(), 0, null, 0);
    }

    /**
     * @return when it was created
     */
    public long getCreationTimestamp()
    {
        return creationTimestamp;
    }


    /**
     * @return time since creation in millis
     */
    public long timeSinceCreation()
    {
        return System.currentTimeMillis() - creationTimestamp;
    }


    /**
     * @param tu time unit conversion
     * @return duration converted to tu
     */
    public long timeSinceCreation(TimeUnit tu)
    {
        SharedUtil.checkIfNulls("TimeUnit can't be null", tu);
        return tu.convert(System.currentTimeMillis() - creationTimestamp, TimeUnit.MILLISECONDS);

    }

    /**
     * @return how many times incUsage ins called
     */
    public long getUsageCounter()
    {
        return usageCounter;
    }

    /**
     * Increment the usage by one
     * @return this
     */
    public Lifetime incUsage()
    {
        return incUsage(1);
    }

    /**
     * Increment usage by inc
     * @param inc usage increment
     * @return this
     */
    public Lifetime incUsage(long inc)
    {
        if (isClosed())
            throw new IllegalStateException("lifetime expired");
        synchronized (this)
        {
            long temp = usageCounter + inc;
            if(maxUse != 0 && temp > maxUse)
                throw new IllegalStateException(temp + " > maxUsage: " + maxUse);
            usageCounter = temp;
            lastUseTimestamp = System.currentTimeMillis();
        }
        return this;
    }

    /**
     * @return How many millis sec wait for the
     */
    public long nextWait()
    {
        if (closed.get() || isMaxUsed())
            throw new IllegalStateException("lifetime expired");

        if (lastUseTimestamp == 0)
        {
            synchronized (this)
            {
                if(lastUseTimestamp == 0)
                {
                    lastUseTimestamp = System.currentTimeMillis();
                }
            }
        }

        if (delayBetweenUsage == 0 )
            return 0;

        return delayBetweenUsage - (System.currentTimeMillis() - lastUseTimestamp);
    }

    /**
     * @return this;
     */
    public Lifetime getType()
    {
        return this;
    }


    /**
     * @return maximum number of usage, 0 forever
     */
    public long getMaxUse()
    {
        return maxUse;
    }

    /**
     * @return last time incUsage called, 0 never
     */
    public long getLastTimeUsed()
    {
        return lastUseTimestamp;
    }


    /**
     * @return the allowed delays in millis between inc()s
     */
    public long getDelayInMillis()
    {
        return delayBetweenUsage;
    }
    /**
     * @return true if the maximum inc are reached
     */
    public boolean isMaxUsed()
    {
        return maxUse != 0 &&  usageCounter >= maxUse;
    }

    @Override
    public void close()
    {
        closed.set(true);
    }


    @Override
    public boolean isClosed() {
        return closed.get() || isMaxUsed() || nextWait() < 0;
    }

    /**
     * @return the instance id
     */
    @Override
    public Long getID() {
        return id;
    }

    @Override
    public String toString() {
        return "Lifetime{" +
                "id= " + id +
                ", creationTimestamp= " + DateUtil.DEFAULT_DATE_FORMAT_TZ.format(creationTimestamp) +
                ", lastUseTimestamp= " + DateUtil.DEFAULT_DATE_FORMAT_TZ.format(lastUseTimestamp) +
                ", delayBetweenUsage= " + Const.TimeInMillis.toString(delayBetweenUsage) +
                ", maxUse= " + maxUse +
                ", usageCounter= " + usageCounter +
                ", closed= " + closed +
                '}';
    }
}
