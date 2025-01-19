package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.data.Range;

public class PercentConverterTest
{
    @Test
    public void intTest()
    {
        PercentConverter pv = new PercentConverter(10, 20);
        Range<Integer> range = pv.iRange();
        for(int i = range.getStart() - 10; i <= range.getEnd() + 10; i++)
        {
            System.out.println(range + " Value = " + i + " percents : " + SUS.toCanonicalID(',',  pv.fPercent(i), pv.iPercent(i), pv.lPercent(i)));
        }
    }
}
