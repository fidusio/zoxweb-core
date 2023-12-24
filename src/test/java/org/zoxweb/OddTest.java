package org.zoxweb;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.Const;

import java.util.Arrays;

public class OddTest {

    @Test
    public void createBillionArray()
    {
        long ts = System.currentTimeMillis();
        int billions = 1_200_000_000;
        int[] array = new int[billions];
        for(int i=0; i < billions; i++)
            array[i] = billions - i;
        assert(array.length == billions);
        Arrays.sort(array);
        ts = System.currentTimeMillis() - ts;
        System.out.println("It took: " + ts + " millis " + Const.TimeInMillis.toString(ts));
    }
    @Test
    public void countToBillion()
    {


        int billions = 2_000_000_000;

        int v = 0;
        long ts = System.currentTimeMillis();
        for(v=0; v < billions; v++);
        ts = System.currentTimeMillis() - ts;
        System.out.println("For loop took: " + Const.TimeInMillis.toString(ts) + " to count to " + v);

        v = 0;
        ts = System.currentTimeMillis();
        while(v < billions)
            v++;
        ts = System.currentTimeMillis() - ts;
        assert(v == billions);
        System.out.println("While loop took: " + Const.TimeInMillis.toString(ts) + " to count to " + v);

    }
}
