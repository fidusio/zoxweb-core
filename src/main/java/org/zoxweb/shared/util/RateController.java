package org.zoxweb.shared.util;


import java.util.concurrent.TimeUnit;

public class RateController
{


    private float rate;
    private TimeUnit unit;
    private Const.TimeInMillis tim;

    private long deltaInMillis;
    private long nextTime;
    private long transactions;



    public RateController(long rate, TimeUnit unit)
    {
        this((float) rate, unit);
    }
    public RateController(float rate, TimeUnit unit)
    {
        setRate(rate, unit);
        nextTime = System.currentTimeMillis() - deltaInMillis;
    }

    public RateController(String rate)
    {
        setRate(rate);
        nextTime = System.currentTimeMillis() - deltaInMillis;
    }


    public float getTPS()
    {
        return deltaInMillis*rate;
    }

    public float getRate()
    {
        return rate;
    }

    public TimeUnit getRateUnit()
    {
        return unit;
    }
    public long getNextTime()
    {
        return nextTime;
    }

    /**
     *
     * @return delta in millis
     */
    public long getDeltaInMillis()
    {
        return deltaInMillis;
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


        long next = nextTime + deltaInMillis;
        long now = System.currentTimeMillis();
        if(next > now)
        {
            delay = next - now;
        }

        nextTime = now + delay;

        transactions++;
        return delay;
    }


    public RateController setRate(String rate)
    {
        String[] tokens = SharedStringUtil.parseString(rate, "/", true);
        float rateValue = Float.parseFloat(tokens[0]);
        Const.TimeInMillis timValue = Const.TimeInMillis.toTimeInMillis(tokens[1]);
        return setRate(rateValue, timValue.UNIT);
    }

    public RateController setRate(long rate, TimeUnit unit)
    {
        return setRate((float) rate, unit);
    }


    public synchronized RateController setRate(float rate, TimeUnit unit)
    {
        SharedUtil.checkIfNulls("TimeUnit null", unit);
        this.rate = rate;
        this.unit = unit;

        if(rate < 0)
            throw new IllegalArgumentException("Invalid tps " + rate);


        tim = Const.TimeInMillis.convert(unit);

        deltaInMillis = 0;

        if (rate != 0)
        {
            float floatDelta = (float)tim.MILLIS/rate;
            if (Math.round(floatDelta) == floatDelta)
            {
                deltaInMillis = (long)floatDelta;
            }
            else {
                deltaInMillis = (long)floatDelta + 1;
            }
        }

        return this;
    }

    @Override
    public String toString() {
        return "RateController{" +
                "rate=" + rate + "/" + tim.getTokens()[0] +
                ", delta=" + deltaInMillis +
                ", lastTime=" + nextTime +
                ", transactions=" + transactions +
                '}';
    }
}

