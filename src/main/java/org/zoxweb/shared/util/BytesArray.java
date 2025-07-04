package org.zoxweb.shared.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BytesArray
    implements IsValid {


    public static final BytesArray EMPTY = new BytesArray(null, Const.EMPTY_BYTE_ARRAY);
    private final byte[] array;
    public final int offset;
    public final int length;

    // volatile on purpose DO NOT CONVERT to final
    private volatile AtomicBoolean valid;
    private volatile Integer hashCode = null;

    public BytesArray(AtomicBoolean valid, byte[] array, int offset, int length, boolean checkBoundary) {
        SUS.checkIfNull("Byte array null", array);
        if (checkBoundary && (offset < 0 || length < 0 || (offset + length > array.length)))
            throw new IndexOutOfBoundsException("Invalid offset and length " + offset + " ," + length + " ," + (offset + length) + " ," + array.length);
        this.array = array;
        this.offset = offset;
        this.length = length;
        this.valid = valid; // valid != null ? valid : new AtomicBoolean(true);
    }

    public BytesArray(AtomicBoolean valid, byte[] array, int offset, int length) {
        this(valid, array, offset, length, true);
    }

    public BytesArray(AtomicBoolean valid, byte[] array) {
        this(valid, array, 0, array.length, true);
    }
    public BytesArray(byte[] array) {
        this(null, array, 0, array.length, true);
    }


    public int byteAt(int index) {
        checkValidity();
        int byteIndex = offset + index;
        if (byteIndex > array.length)
            throw new ArrayIndexOutOfBoundsException(index + " out of bound length: " + length);
        return array[byteIndex];
    }


    public String asString() {
        checkValidity();
        return new String(array, offset, length);
    }

    /**
     * @return a copy of the byte array if you want the direct access {@link #getArray()}
     */
    public byte[] asBytes() {
        checkValidity();
        return Arrays.copyOfRange(array, offset, offset + length);
    }

    public BytesArray copy() {
        checkValidity();
        BytesArray ret = new BytesArray(null, asBytes());
        checkValidity();// we must perform double validation
        return ret;
    }

    public boolean isValid() {
        return valid == null || valid.get();
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
            throws IOException {
        if (isValid()) {
            os.write(array, offset, length);
            if (flush)
                os.flush();


        } else
            throw new IOException("Byte buffer invalid");
    }

    public InputStream toInputStream()
            throws IOException {
        if (isValid())
            return new ByteArrayInputStream(array, offset, length);
        else
            throw new IOException("Byte buffer invalid");
    }

    public void checkValidity() {
        if (!isValid())
            throw new ProtocolException("Invalid BytesArray");
    }

    /**
     * @return the internal byte array use with extreme caution
     */
    public byte[] getArray()
    {
        checkValidity();
        return array;
    }
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;

        if (o instanceof byte[]) {
            byte[] input = (byte[]) o;
            return SharedUtil.equals(this, array, offset, offset + length, input, 0, input.length);
        }

        if (getClass() != o.getClass()) return false;

        BytesArray that = (BytesArray) o;
        return (length == that.length) && SharedUtil.equals(() -> isValid() && that.isValid(), array, offset, offset + length, that.array, that.offset, that.offset + that.length);
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            synchronized (this) {
                if (hashCode == null) {
                    checkValidity();
                    hashCode = SharedUtil.hashCode(array, offset, length);
                }
            }
        }
        return hashCode;
    }


    /**
     * Return the first index of matching bytes in contained within the stream
     *
     * @param match for byte array matching
     * @return index of the match, -1 no match found
     */
    public int indexOf(byte[] match) {
        checkValidity();
        return SharedUtil.indexOf(array, offset, offset + length, match, 0, match.length);
    }


    /**
     * @param startAt     index inclusive
     * @param match       byte array to match
     * @param matchOffset offset relative to the internal bye array
     * @param matchLength of the match
     * @return index -1 not found or index of the first match
     */
    public int indexOf(int startAt, byte[] match, int matchOffset, int matchLength) {
        checkValidity();
        if (startAt < 0)
            throw new IllegalArgumentException("Negative start index " + startAt);

        int ret = SharedUtil.indexOf(array, offset + startAt, offset + length, match, matchOffset, matchLength);
        return ret != -1 ? ret - offset : -1;
    }

    public int indexOf(int startAt, byte[] match) {
        checkValidity();
        if (startAt < 0)
            throw new IllegalArgumentException("Negative start index " + startAt);
        int ret = SharedUtil.indexOf(array, offset + startAt, offset + length, match, 0, match.length);
        return ret != -1 ? ret - offset : -1;
    }

    public int indexOf(int startAt, String str) {
        checkValidity();
        if (startAt < 0)
            throw new IllegalArgumentException("Negative start index " + startAt);
        byte[] match = SharedStringUtil.getBytes(str);
        int ret = SharedUtil.indexOf(array, offset + startAt, offset + length, match, 0, match.length);
        return ret != -1 ? ret - offset : -1;
    }


    public int indexOf(String str) {
        checkValidity();
        int ret = SharedUtil.indexOf(array, offset, offset + length, str, 0, str.length(), false);
        return ret != -1 ? ret - offset : -1;
    }

    public int indexOfIgnoreCase(String str) {
        checkValidity();
        int ret = SharedUtil.indexOf(array, offset, offset + length, str, 0, str.length(), true);
        return ret != -1 ? ret - offset : -1;
    }

    public String toString(int startIndex, int strLength) {
        checkValidity();
        if (startIndex < 0 || strLength > (length - startIndex))
            throw new IllegalArgumentException("Invalid index " + startIndex);
        return new String(array, offset + startIndex, strLength);
    }

    public String toString(int startIndex) {
        checkValidity();
        if (startIndex < 0 || startIndex > length)
            throw new IllegalArgumentException("Negative start index " + startIndex);
        return new String(array, offset + startIndex, length - startIndex);
    }


}
