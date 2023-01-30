package org.zoxweb.shared.util;



public class RateCounter
    extends NamedDescription
{

    private long counts = 0;
    private long deltas = 0;


    public RateCounter(String name)
    {
        this(name, null);
    }
    public RateCounter(String name, String description)
    {
        super(name, description);
    }



    public RateCounter register(long delta)
    {
        return register(delta, 1);
    }

    public synchronized RateCounter register(long delta, long inc)
    {
       deltas += delta;
       counts += inc;
       return this;
    }


    public synchronized RateCounter reset()
    {
        deltas = 0;
        counts = 0;
        return this;
    }

    public long getDeltas()
    {
        return deltas;
    }
    public long getCounts()
    {
        return counts;
    }

    public float average()
    {
        return average(1);
    }

    public float average(float rateMultiplier)
    {
        float ret = 0;
        if(counts != 0) {
            ret = rateMultiplier * ((float) deltas / (float) counts);
        }
        return ret;
    }

    public float average(long rateMultiplier)
    {
      return average((float)rateMultiplier);
    }

    public float rate()
    {
        return rate(1);
    }

    public float rate(int multiplier)
    {
        return rate((float)multiplier);
    }

    public float rate(long multiplier)
    {
        return rate((float)multiplier);
    }
    public float rate(float multiplier)
    {
        float ret = 0;
        if(deltas != 0) {
            ret = multiplier * ((float) counts / (float) deltas);
        }

        return ret;
    }


    public float average(int rateMultiplier)
    {
        return average((float)rateMultiplier);
    }



    @Override
    public String toString() {
        return "{" +
                "name=\"" + getName()+"\"" +
                (getDescription() != null ? ", description=\"" + getDescription() +"\"" : "") +
                ", counts=" + counts +
                ", deltas=" + deltas +
                ", average=" + average() +
                '}';
    }
}
