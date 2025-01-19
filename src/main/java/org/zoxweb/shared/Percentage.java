package org.zoxweb.shared;

public class Percentage<V extends Number>
{
    private final V min;
    private final V max;


    public Percentage(V min, V max)
    {
        if(min.floatValue() >= max.floatValue())
            throw new IllegalArgumentException( min +" >= " + max);
        this.min = min;
        this.max = max;
    }


    public float floatPercent(float value)
    {
        return ((value-min.floatValue())*100)/(max.floatValue()-min.floatValue());
    }
    public int intPercent(int value)
    {
        return (int)floatPercent((float)value);
    }

    public long longPercent(long value)
    {
        return (long)floatPercent((float)value);
    }

}
