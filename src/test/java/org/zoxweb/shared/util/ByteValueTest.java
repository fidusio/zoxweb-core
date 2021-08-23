package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.BytesValue;


import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by mnael on 5/9/2017.
 */


public class ByteValueTest
{
    @Test
    public void genericTest()
    {
        byte result[] = BytesValue.SHORT.toBytes((short) 25999);

        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putShort((short) 25999);
        System.out.println(result.length + " " + Arrays.toString(result) + " " + BytesValue.SHORT.toValue(result) + " " + Arrays.toString(bb.array()));
        result = BytesValue.INT.toBytes(4000000);
        bb.clear();
        bb.putInt(4000000);
        System.out.println(result.length + " " + Arrays.toString(result) + " " + BytesValue.INT.toValue(result) + " " + Arrays.toString(bb.array()));
        
        
        
        result = BytesValue.LONG.toBytes(null, 0, Long.decode("10000000000"), Long.decode("100000000001"));
        System.out.println(result.length + " " + Arrays.toString(result) + " " + BytesValue.LONG.toValue(result));

        byte buffer[] = BytesValue.SHORT.toBytes((short) 25999);
        short s = 0;
        s |=  buffer[0]& 0xFF;
        s = (short)( s<<8  | buffer[1] &0xFF);
        System.out.println(s);
    }

    @Test
    public void testShort() {
        byte[] buffer = BytesValue.SHORT.toBytes(Short.MAX_VALUE);
        assert(Short.MAX_VALUE == BytesValue.SHORT.toValue(buffer));
        System.out.print("Short MAX: " + BytesValue.SHORT.toValue(buffer));
        buffer = BytesValue.SHORT.toBytes(Short.MIN_VALUE);
        assert(Short.MIN_VALUE == BytesValue.SHORT.toValue(buffer));
        System.out.print(" Short MIN: " + BytesValue.SHORT.toValue(buffer));
        System.out.println();
    }

    @Test
    public void testInteger() {
        byte[] buffer = BytesValue.INT.toBytes(Integer.MAX_VALUE);
        assert(Integer.MAX_VALUE == BytesValue.INT.toValue(buffer));
        System.out.print("Int MAX: " + BytesValue.INT.toValue(buffer));
        buffer = BytesValue.INT.toBytes(Integer.MIN_VALUE);
        assert(Integer.MIN_VALUE == BytesValue.INT.toValue(buffer));
        System.out.print(" Int MIN: " + BytesValue.INT.toValue(buffer));
        System.out.println();
    }

    @Test
    public void testLong() {
        byte[] buffer = BytesValue.LONG.toBytes(Long.MAX_VALUE);
        assert(Long.MAX_VALUE == BytesValue.LONG.toValue(buffer));
        System.out.print("Long MAX: " + BytesValue.LONG.toValue(buffer));
        buffer = BytesValue.LONG.toBytes(Long.MIN_VALUE);
        assert(Long.MIN_VALUE == BytesValue.LONG.toValue(buffer));
        System.out.print(" Long MIN: " + BytesValue.LONG.toValue(buffer));
        System.out.println();

        buffer =  BytesValue.LONG.toBytes(10000000000L);
        System.out.println("Long from bytes : " + BytesValue.LONG.toValue(buffer));
        buffer =  BytesValue.LONG.toBytes(-10000000000L);
        System.out.println("Long from bytes : " + BytesValue.LONG.toValue(buffer));
    }
}
