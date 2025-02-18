package org.zoxweb.shared.protocol;

import org.zoxweb.shared.util.SUS;

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
}
