package org.zoxweb.shared.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class BytesArray
{
    public static final BytesArray EMPTY = new BytesArray(null, Const.EMPTY_BYTE_ARRAY);


    private final byte[] array;
    public final int offset;
    public final int length;
    private final AtomicBoolean valid;

    public BytesArray(AtomicBoolean valid, byte[] array, int offset, int length, boolean checkBoundary)
    {
        SUS.checkIfNull("Byte array null", array);
        if (checkBoundary && (offset < 0 || length < 0 || (offset + length > array.length)))
            throw new IndexOutOfBoundsException("Invalid offset and length " + offset + " ," + length + " ," + (offset + length) + " ," + array.length);
        this.array = array;
        this.offset = offset;
        this.length = length;
        this.valid = valid != null ? valid : new AtomicBoolean(true);
    }
    public BytesArray(AtomicBoolean valid, byte[] array, int offset, int length)
    {
       this(valid, array, offset, length, true);
    }
    public BytesArray(AtomicBoolean valid, byte[] array)
    {
        this(valid, array, 0, array.length, true);
    }


    public int byteAt(int index)
    {
        checkValidity();
        int byteIndex = offset + index;
        if(byteIndex > array.length)
            throw new ArrayIndexOutOfBoundsException(index + " out of bound length: " + length);
        return array[byteIndex];
    }


    public String asString()
    {
        checkValidity();
        return new String(array, offset, length);
    }

    public byte[] asBytes()
    {
        checkValidity();
        return Arrays.copyOfRange(array, offset, offset + length);
    }

    public BytesArray copy()
    {
        checkValidity();
        BytesArray ret = new BytesArray(null ,asBytes());
        checkValidity();// we must perform double validation
        return ret;
    }

    public boolean isValid()
    {
        return valid.get();
    }

    @Override
    public String toString() {
        return "BytesArray{" +
                "offset=" + offset +
                ", length=" + length +
                ", valid=" + isValid() +
                '}';
    }

    public void writeTo(OutputStream os, boolean flush)
            throws IOException
    {
        if(isValid())
        {
            os.write(array, offset, length);
            if (flush)
                os.flush();


        }
        else
            throw new IOException("Byte buffer invalid");
    }

    private void checkValidity()
    {
        if(!isValid())
            throw new ProtocolException("Invalid BytesArray");
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || getClass() != o.getClass()) return false;
        BytesArray that = (BytesArray) o;
        return offset == that.offset && length == that.length && (valid.get() ==  that.valid.get()) && Objects.deepEquals(array, that.array);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(Arrays.hashCode(array), offset, length, valid);
    }
}
