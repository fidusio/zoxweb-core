package org.zoxweb.shared.util;

import org.zoxweb.shared.data.Range;

public class PercentConverter
{
    private final float min;
    private final float max;


    public PercentConverter(int min, int max)
    {
        this((float)min, (float)max);
    }

    public PercentConverter(long min, long max)
    {
        this((float)min, (float)max);
    }

    public PercentConverter(float min, float max)
    {
        if(min >= max )
            throw new IllegalArgumentException( min +" >= " + max);
        this.min = min;
        this.max = max;
    }


    public float fPercent(float value)
    {
        return ((value-min)*100)/(max - min);
    }
    public int iPercent(int value)
    {
        return (int)fPercent((float)value);
    }

    public int iPercent(long value)
    {
        return (int)fPercent((float)value);
    }


    public long lPercent(long value)
    {
        return (long)fPercent((float)value);
    }
    public long lPercent(int value)
    {
        return (long)fPercent((float)value);
    }

    public double dPercent(double value) {

        return fPercent((float)value);
    }

    public Range<Float> fRange()
    {
        return new Range<>(min, max, Range.Inclusive.BOTH);
    }

    public Range<Integer> iRange()
    {
        return new Range<>((int) min, (int) max, Range.Inclusive.BOTH);
    }

    public Range<Long> lRange()
    {
        return new Range<>((long) min, (long) max, Range.Inclusive.BOTH);
    }

}
