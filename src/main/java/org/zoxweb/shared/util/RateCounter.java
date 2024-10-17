package org.zoxweb.shared.util;


import org.zoxweb.shared.data.AddressDAO;
import org.zoxweb.shared.data.SetNameDescriptionDAO;

public class RateCounter
    extends SetNameDescriptionDAO
{
    public enum Param
            implements GetNVConfig
    {
        COUNTS(NVConfigManager.createNVConfig("counts", "Counted values", "Counts", true, false, long.class)),
        DELTAS(NVConfigManager.createNVConfig("deltas", "Accumulated deltas", "Deltas", true, false, long.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc)
        {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig()
        {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_RATE_COUNTER = new NVConfigEntityLocal(
            "rate_counter",
            null ,
            "RateCounter",
            true,
            false,
            false,
            false,
            AddressDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );


    private transient long timestamp = 0;
    private transient long lastDelta = 0;

    public RateCounter()
    {
        super(NVC_RATE_COUNTER);
    }
    public RateCounter(String name)
    {
        this(name, null);
    }

    public RateCounter(Enum<?> name)
    {
        this(SUS.enumName(name), null);
    }
    public RateCounter(String name, String description)
    {
        this();
        setName(name);
        setDescription(description);
    }


    /**
     * Register a delta time stamp based on a previously sampled System.currentTimeMillis()
     * and return the current timestamp System.currentTimeMillis()
     * @param timeStamp sometime in past timeStamp = System.currentTimeMillis()
     * @return current time stamp in millis
     */
    public long registerTimeStamp(long timeStamp)
    {
        return registerTimeStamp(true, timeStamp, 1);
    }

    /**
     *
     * @param millis if true the timeStamp is in millisecond otherwise it is in nanosecond
     * @param timeStamp sometime in past timeStamp = System.currentTimeMillis() or System.nanoTime()
     * @param inc count
     * @return current time stamp in millis or nanos depend on the millis value
     */
    public long registerTimeStamp(boolean millis, long timeStamp, long inc)
    {
        long ts = millis ? System.currentTimeMillis() : System.nanoTime();
        register(ts - timeStamp, inc);
        return ts;
    }

    public RateCounter register(long delta)
    {
        return register(delta, 1);
    }

    public RateCounter inc()
    {
        register(0, 1);
        return this;
    }

    public RateCounter inc(long inc)
    {
        register(0, inc);
        return this;
    }

    public synchronized RateCounter register(long delta, long inc)
    {
       setValue(Param.DELTAS, (getDeltas() + delta));
       setValue(Param.COUNTS,  (getCounts() + inc));
       lastDelta = delta;
       return this;
    }

    public synchronized long start()
    {
        timestamp = System.currentTimeMillis();
        return timestamp;
    }

    public  long stop()
    {
        return stop(1);
    }

    public synchronized long stop(long inc)
    {
        if (timestamp == 0)
            throw new IllegalStateException("Never started must calls start() first");
        long currentTS = System.currentTimeMillis();

        register(currentTS - timestamp, inc);
        timestamp = currentTS;
        return timestamp;
    }

    public synchronized long lastTimeStampInMillis()
    {
        return timestamp;
    }


    public synchronized long lastDeltaInMillis()
    {
        return lastDelta;
    }


    public synchronized RateCounter reset()
    {
        setValue(Param.DELTAS, (long)0);
        setValue(Param.COUNTS, (long)0);
        lastDelta = 0;
        return this;
    }

    public long getDeltas()
    {
        return lookupValue(Param.DELTAS);
    }
    public long getCounts()
    {
        return lookupValue(Param.COUNTS);
    }

    public float average()
    {
        return average(1);
    }

    public float average(float rateMultiplier)
    {
        float ret = 0;
        if(getCounts() != 0) {
            ret = rateMultiplier * ((float) getDeltas() / (float) getCounts());
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
        if(getDeltas() != 0) {
            ret = multiplier * ((float) getCounts() / (float) getDeltas());
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
                ", counts=" + getCounts() +
                ", deltas=" + getDeltas() +
                ", average=" + average() +
                '}';
    }
}
