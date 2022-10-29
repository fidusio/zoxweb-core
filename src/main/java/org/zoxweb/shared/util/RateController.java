package org.zoxweb.shared.util;



public class RateController
{
    private float tps;

    private long delta;
    private long nextTime;
    private long transactions;


    public RateController(long tps)
    {
        this((float) tps);
    }
    public RateController(float tps)
    {
        setTPS(tps);
        nextTime = System.currentTimeMillis();
    }


    public float getTPS()
    {
        return tps;
    }
    public long getNextTime()
    {
        return nextTime;
    }

    /**
     *
     * @return delta in millis
     */
    public long getDelta()
    {
        return delta;
    }

    public long getTransactions()
    {
        return transactions;
    }

    /**
     *
     * @return the next delay based on the set rate
     */
    public synchronized long nextDelay()
    {
        long delay = 0;


        long next = nextTime + delta;
        long now = System.currentTimeMillis();
        if(next > now)
        {
            delay = next - now;
        }

        nextTime = now + delay;

        transactions++;
        return delay;
    }

    public RateController setTPS(long tps)
    {
        return setTPS((float) tps);
    }

    public synchronized RateController setTPS(float tps)
    {
        if(tps < 0)
            throw new IllegalArgumentException("Invalid tps " + tps);

        this.tps = tps;
        delta = 0;

        if (tps != 0)
        {
            float floatDelta = (float)1000/tps;
            if (Math.round(floatDelta) == floatDelta)
            {
                delta = (long)floatDelta;
            }
            else {
                delta = (long)floatDelta + 1;
            }
        }

        return this;
    }

    @Override
    public String toString() {
        return "RateController{" +
                "tps=" + tps +
                ", delta=" + delta +
                ", lastTime=" + nextTime +
                ", transactions=" + transactions +
                '}';
    }
}

