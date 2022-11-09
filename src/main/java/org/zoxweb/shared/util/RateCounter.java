package org.zoxweb.shared.util;



public class RateCounter
    implements GetName
{
    private final NamedDescription namedDescription;
    private long counter = 0;
    private long deltaCounter = 0;


    public RateCounter(NamedDescription namedDescription)
    {
        this.namedDescription = namedDescription != null ? namedDescription : new NamedDescription("");
    }

    public NamedDescription getNameDescription()
    {
        return namedDescription;
    }


    public RateCounter inc(long delta)
    {
        return inc(delta, 1);
    }

    public synchronized RateCounter inc(long delta, long count)
    {
       deltaCounter += delta;
       counter += count;
       return this;
    }

    public long getDeltaCounter()
    {
        return deltaCounter;
    }
    public long getCounter()
    {
        return counter;
    }

    public float rate()
    {
        return rate(1);
    }

    public float rate(float rateMultiplier)
    {
        float ret = 0;
        if(counter != 0) {
            ret = rateMultiplier * ((float) deltaCounter / (float) counter);
        }
        return ret;
    }

    public float rate(long rateMultiplier)
    {
      return rate((float)rateMultiplier);
    }


    public float rate(int rateMultiplier)
    {
        return rate((float)rateMultiplier);
    }


    public String getName()
    {
        return namedDescription.getName();
    }

    @Override
    public String toString() {
        return "RateCounter{" +
                "namedDescription=" + namedDescription +
                ", counter=" + counter +
                ", deltaCounter=" + deltaCounter +
                '}';
    }
}
