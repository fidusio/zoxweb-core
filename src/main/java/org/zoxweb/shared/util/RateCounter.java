package org.zoxweb.shared.util;



public class RateCounter
    implements GetName
{
    private final NamedDescription namedDescription;
    private long counter = 0;
    private long deltaCounter = 0;


    public RateCounter(String name)
    {
        this(new NamedDescription(name));
    }
    public RateCounter(NamedDescription namedDescription)
    {
        this.namedDescription = namedDescription != null ? namedDescription : new NamedDescription("");
    }

    public NamedDescription getNameDescription()
    {
        return namedDescription;
    }


    public RateCounter register(long delta)
    {
        return register(delta, 1);
    }

    public synchronized RateCounter register(long delta, long inc)
    {
       deltaCounter += delta;
       counter += inc;
       return this;
    }


    public synchronized RateCounter reset()
    {
        deltaCounter = 0;
        counter = 0;
        return this;
    }

    public long getDeltas()
    {
        return deltaCounter;
    }
    public long getCounts()
    {
        return counter;
    }

    public float average()
    {
        return average(1);
    }

    public float average(float rateMultiplier)
    {
        float ret = 0;
        if(counter != 0) {
            ret = rateMultiplier * ((float) deltaCounter / (float) counter);
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
        if(deltaCounter != 0) {
            ret = multiplier * ((float) counter / (float) deltaCounter);
        }

        return ret;
    }


    public float average(int rateMultiplier)
    {
        return average((float)rateMultiplier);
    }


    public String getName()
    {
        return namedDescription.getName();
    }

    @Override
    public String toString() {
        return "RateCounter{" +
                "name=" + getName() +
                ", counts=" + counter +
                ", deltas=" + deltaCounter +
                ", average=" + average() +
                '}';
    }
}
