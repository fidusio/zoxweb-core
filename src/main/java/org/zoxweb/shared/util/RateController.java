package org.zoxweb.shared.util;


import java.util.concurrent.TimeUnit;

public class RateController
    implements GetName, WaitTime<RateController>
{

    public enum RCType
    {
        TIME,
        COUNTER
    }

    private float rate;
    private TimeUnit unit;
    private Const.TimeInMillis tim;
    private long deltaInMillis;
    private long nextTime;
    private long callCounts;

    private RCType type = RCType.TIME;
    private int counter = 0;
    private long counterEndTime = 0;
    private long counterStartTime = 0;

    private long duration;
    private final NamedDescription namedDescription;



    public RateController(String name, long rate, TimeUnit unit)
    {
        this(name, (float) rate, unit);
    }
    public RateController(String name, float rate, TimeUnit unit)
    {
        setRate(rate, unit);
        this.namedDescription = new NamedDescription(name);
        nextTime = System.currentTimeMillis() - deltaInMillis;
    }


    public RateController(String name, String rate)
    {
        setRate(rate);
        this.namedDescription = new NamedDescription(name);//namedDescription != null ? namedDescription : new NamedDescription("");
        nextTime = System.currentTimeMillis() - deltaInMillis;
    }

    public NamedDescription getNameDescription()
    {
        return namedDescription;
    }



    @Override
    public RateController getType()
    {
        return this;
    }

    public RCType getRCType()
    {
        return type;
    }

    public RateController setRCType(RCType type)
    {
        SharedUtil.checkIfNulls("Null type", type);
        this.type = type;
        return this;
    }

    public String getName()
    {
      return namedDescription.getName();
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

    public long getDuration()
    {
        return duration;
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

    public long getCallCounts()
    {
        return callCounts;
    }







    /**
     *
     * @return the next delay based on the set rate
     */
    public synchronized long nextWait()
    {
        if(rate == 0)
        {
            throw new IllegalArgumentException("Rate is zero");
        }
        long delay = 0;


        switch (type)
        {

            case TIME:
            {
                long now = System.currentTimeMillis();
                long next = nextTime + deltaInMillis;

                if(next > now)
                {
                    delay = next - now;
                }

                nextTime = now + delay;
            }
                break;
            case COUNTER:
            {
                long now = System.currentTimeMillis();

                if(counterEndTime < now )//|| now - counterStartTime > duration)
                {
                    counter = 0;
                    counterEndTime = now + duration;
                    counterStartTime = now;
                }

               if((now - counterStartTime)> duration || counter == rate)
               {
                   counter = 0;

                   counterStartTime = counterEndTime;
                   counterEndTime+=duration;
               }
               counter++;

               if(now - counterStartTime < duration && counterEndTime - now <= duration)
               {
                   delay = 0;
               }
               else {
                   delay = counterStartTime - now;
               }
            }
                break;
        }

        callCounts++;
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
            else
            {
                deltaInMillis = (long)floatDelta + 1;
            }
            duration = deltaInMillis*(long)rate;
        }
        counterStartTime = System.currentTimeMillis();
        counterEndTime = counterStartTime + duration;

        return this;
    }



    @Override
    public String toString() {
        return "RateController{" +
                "name=" + getName() +
                "rate=" + rate + "/" + tim.getTokens()[0] +
                ", unit=" + unit +
                ", deltaInMillis=" + deltaInMillis +
                ", nextTime=" + nextTime +
                ", transactions=" + callCounts +
                ", type=" + type +
                ", counter=" + counter +
                ", counterEndTime=" + counterEndTime +
                ", duration=" + duration +
                '}';
    }
}

