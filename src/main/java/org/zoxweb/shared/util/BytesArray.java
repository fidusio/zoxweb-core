package org.zoxweb.shared.util;

public class BytesArray
{
    public final byte[] array;
    public final int offset;
    public final int length;
    public BytesArray(byte[] array, int offset, int length)
    {
        SUS.checkIfNulls("Byte array null", array);
        if (offset < 0 || length < 0 ||(offset + length > array.length))
            throw new IndexOutOfBoundsException("Invalid offset and length " + offset + " ," + length + " ," + (offset + length) + " ," + array.length);
        this.array = array;
        this.offset = offset;
        this.length = length;
    }
    public BytesArray(byte[] array)
    {
        this(array, 0, array.length);
    }
}
