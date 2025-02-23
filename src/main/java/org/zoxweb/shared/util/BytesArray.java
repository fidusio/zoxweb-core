package org.zoxweb.shared.util;

import java.util.Arrays;

public class BytesArray
{
    public final byte[] array;
    public final int offset;
    public final int length;

    public BytesArray(byte[] array, int offset, int length, boolean checkBoundary)
    {
        SUS.checkIfNulls("Byte array null", array);
        if (checkBoundary && (offset < 0 || length < 0 || (offset + length > array.length)))
            throw new IndexOutOfBoundsException("Invalid offset and length " + offset + " ," + length + " ," + (offset + length) + " ," + array.length);
        this.array = array;
        this.offset = offset;
        this.length = length;
    }
    public BytesArray(byte[] array, int offset, int length)
    {
       this(array, offset, length, true);
    }
    public BytesArray(byte[] array)
    {
        this(array, 0, array.length, true);
    }


    public int byteAt(int index)
    {
        int byteIndex = offset + index;
        if(byteIndex > length)
            throw new ArrayIndexOutOfBoundsException(index + " out of bound length: " + length);
        return array[byteIndex];
    }


    public String toString()
    {
        return SUS.toCanonicalID(',', array.length, offset, length);
    }

    public String asString()
    {
        return new String(array, offset, length);
    }

    public byte[] asBytes()
    {
        return Arrays.copyOfRange(array, offset, offset + length);
    }
}
