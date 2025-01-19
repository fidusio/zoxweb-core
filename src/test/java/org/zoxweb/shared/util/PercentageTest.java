package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.Percentage;

public class PercentageTest
{
    @Test
    public void intTest()
    {
        Percentage<Integer> intPercent = new Percentage<>(100, 500);
        for(int i=0; i < 600; i++)
        {
            System.out.println(SUS.toCanonicalID(',', i, intPercent.floatPercent(i), intPercent.intPercent(i), intPercent.longPercent(i)));
        }
    }
}
