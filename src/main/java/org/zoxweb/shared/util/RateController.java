package org.zoxweb.shared.util;


import java.util.concurrent.TimeUnit;

public class RateController
{

    public enum Mode
    {
        TIME_BASED,
        RATE_COUNTER
    }


    private float rate;

    private TimeUnit unit;
    private Const.TimeInMillis tim;

    private long deltaInMillis;
    private long nextTime;
    private long transactions;
    private final Mode modeType;
//    private int rateCounter = 0;
//    private long startTime;
    private long duration;



    public RateController(long rate, TimeUnit unit)
    {
        this((float) rate, unit);
    }
    public RateController(float rate, TimeUnit unit)
    {
        setRate(rate, unit);
        modeType = Mode.TIME_BASED;
        nextTime = System.currentTimeMillis() - deltaInMillis;
    }

    public RateController(String rate)
    {
        this(Mode.TIME_BASED, rate);
    }
    private RateController(Mode modeType, String rate)
    {
        this.modeType = modeType;
        setRate(rate);

        nextTime = System.currentTimeMillis() - deltaInMillis;
    }


    public float getTPS()
    {
        return (rate/tim.MILLIS)*1000;
    }

    public long getTPSAsLong()
    {
        return (long) getTPS();
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

    public Mode getMode()
    {
        return modeType;
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
        if(rate == 0)
        {
            throw new IllegalArgumentException("Rate is zero");
        }
        long delay = 0;
        long now = System.currentTimeMillis();
        switch (getMode())
        {

            case TIME_BASED:
                long next = nextTime + deltaInMillis;

                if(next > now)
                {
                    delay = next - now;
                }

                nextTime = now + delay;
                break;
            case RATE_COUNTER:
//                if(rateCounter == 0)
//                {
//                    startTime = now;
//                }
//                rateCounter++;
//
//                if (rateCounter <= rate &&  now - startTime < duration)
//                {
//                    delay = 0;
//                }
//                else {
//                    delay = startTime + duration;
//                    rateCounter = 0 ;
//                }
                break;
        }



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
            duration = deltaInMillis*(long)rate;
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

