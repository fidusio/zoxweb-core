package org.zoxweb;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.RateCounter;

import java.util.Arrays;

public class OddTest {

    static RateCounter rc = new RateCounter("tes");


    @Test
    public void createBillionArray()
    {

        int billions = 1_200_000_000;
        int[] array = new int[billions];
        rc.reset();
        rc.start();
        for(int i=0; i < billions; i++)
            array[i] = billions - i;
        assert(array.length == billions);
        Arrays.sort(array);
        rc.stop();
        System.out.println("It took: " + rc.getDeltas() + " millis " + Const.TimeInMillis.toString(rc.getDeltas()));
    }
    @Test
    public void countToBillion()
    {


        int billions = 2_000_000_000;

        int v = 0;
        rc.reset().start();
        for(v=0; v < billions; v++);
        rc.stop();
        System.out.println("For loop took: " + Const.TimeInMillis.toString(rc.getDeltas()) + " to count to " + v);

        v = 0;
        rc.reset().start();
        while(v < billions)
            v++;
        rc.stop();
        assert(v == billions);
        System.out.println("While loop took: " + Const.TimeInMillis.toString(rc.getDeltas()) + " to count to " + v);

    }

    @Test
    public void numberEquality()
    {
        for(int i = 0; i < 256; i++)
        {
            int a = i;
            int b = i;
            long lA = i;
            long lB = i;
            Integer aI = new Integer(i);
            Integer bI = new Integer(i);
            assert a==b;
            assert lA==lB;
            assert lA==a;
            assert aI.equals(bI);
        }
    }
}
