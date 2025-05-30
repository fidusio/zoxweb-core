/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.server.io;

import org.zoxweb.shared.util.BytesArray;
import org.zoxweb.shared.util.DataBufferController;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The UByteArrayOutputStream class.
 */
public class UByteArrayOutputStream
        extends ByteArrayOutputStream
        implements Externalizable, DataBufferController {

    private AtomicBoolean valid = new AtomicBoolean(true);

    /**
     * Create a byte array output stream.
     */
    public UByteArrayOutputStream() {
        super();
    }

    /**
     * @param size of initial buffer
     */
    public UByteArrayOutputStream(int size) {
        super(size);
    }


    public UByteArrayOutputStream(String str) {
        this(SharedStringUtil.getBytes(str));
    }

    /**
     * @param buffer to be written
     */
    public UByteArrayOutputStream(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }

    /**
     * @param buffer to be written
     * @param offset of the buffer
     * @param len    of the data to write
     */
    public UByteArrayOutputStream(byte[] buffer, int offset, int len) {
        super(buffer.length);
        write(buffer, offset, len);
    }


    /**
     *
     */
    public synchronized void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(count);
        byte[] newBuffer = new byte[count];
        System.arraycopy(buf, 0, newBuffer, 0, count);
        ((ObjectOutputStream) out).writeUnshared(newBuffer);

        out.flush();
    }

    public synchronized void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException {
        count = in.readInt();
        buf = (byte[]) ((ObjectInputStream) in).readUnshared();

    }


    /**
     * This method will transfer the data to ByteArrayInputStream and reset the UByteArrayOutputStream
     *
     * @return ByteArrayInputStream, the data in the input stream after the call are immutable, the implementing class create a new buffer and size() = 0
     */
    public synchronized ByteArrayInputStream toByteArrayInputStream() {
        byte[] tempBuf = buf;
        int tempCount = size();
        buf = new byte[32];
        count = 0;
        // the old valid don't modify it is since old buf is read only now
        valid = new AtomicBoolean(true);

        return new UByteArrayInputStream(tempBuf, 0, tempCount);
    }

    /**
     * @param index of the char we need to look at
     * @return the char at index.
     */
    public char charAt(int index) {
        return (char) buf[index];
    }

    /**
     * @param index of byte looking for
     * @return the byte at index.
     */
    public byte byteAt(int index) {
        return buf[index];
    }

    /**
     * Return the first index of matching bytes in contained within the stream
     *
     * @param match for byte array matching
     * @return index of the match, -1 no match found
     */
    public int indexOf(byte[] match) {
        return SharedUtil.indexOf(getInternalBuffer(), 0, size(), match, 0, match.length);
    }

    /**
     * @param startAt     index inclusive
     * @param match       byte array to match
     * @param matchOffset offset relative to the internal bye array
     * @param matchLength of the match
     * @return index -1 not found or index of the first match
     */
    public int indexOf(int startAt, byte[] match, int matchOffset, int matchLength) {
        return SharedUtil.indexOf(getInternalBuffer(), startAt, size(), match, matchOffset, matchLength);
    }

    public int indexOf(int startAt, byte[] match) {
        return SharedUtil.indexOf(getInternalBuffer(), startAt, size(), match, 0, match.length);
    }

    public int indexOf(int startAt, String str) {
        byte[] match = SharedStringUtil.getBytes(str);
        return SharedUtil.indexOf(getInternalBuffer(), startAt, size(), match, 0, match.length);
    }

    public int indexOf(int fromIndex, int toIndex, byte[] toMatch) {
        return SharedUtil.indexOf(size(), getInternalBuffer(), fromIndex, toIndex, toMatch, 0, toMatch.length);
    }


    public int indexOf(String str) {
        return SharedUtil.indexOf(getInternalBuffer(), 0, size(), str, 0, str.length(), false);
    }

    public int indexOfIgnoreCase(String str) {
        return SharedUtil.indexOf(getInternalBuffer(), 0, size(), str, 0, str.length(), true);
    }

    /**
     * @param index of the byte
     * @return the integer at index.
     */
    public int intAt(int index) {
        return buf[index];
    }

    public static UByteArrayOutputStream diff(UByteArrayOutputStream baos1, UByteArrayOutputStream baos2) {
        UByteArrayOutputStream ret = new UByteArrayOutputStream();
        // check condition
        {
            byte[] buff1 = baos1.getInternalBuffer();
            byte[] buff2 = baos2.getInternalBuffer();
            for (int i = 0; i < baos2.size(); i++) {
                byte diff = buff2[i];

                if (i < baos1.size()) {
                    diff = (byte) (diff - buff1[i]);
                }
                ret.write(diff);
            }

        }

        return ret;
    }

    /**
     * @param baos1 to compare
     * @param baos2 to compare
     * @return true if equals
     */
    public static boolean areEqual(UByteArrayOutputStream baos1, UByteArrayOutputStream baos2) {
        if (baos1 == baos2) {
            return true;
        }

        if (baos1 != null && baos2 != null && baos1.size() == baos2.size()) {
            int len = baos1.size();

            for (int i = 0; i < len; i++) {
                if (baos1.buf[i] != baos2.buf[i]) {
                    return false;
                }
            }

            return true;

        }

        return false;
    }


    public synchronized byte[] copyBytes(int from) {
        return copyBytes(from, size());
    }

    public synchronized byte[] copyBytes(int from, int to) {
        return Arrays.copyOfRange(getInternalBuffer(), from, to);
    }

    /**
     * Write a string to the data buffer
     *
     * @param str converted to bytes
     */
    public void write(String str) {
        write(SharedStringUtil.getBytes(str));
    }

    @Override
    public void write(byte[] buf) {
        write(buf, 0, buf.length);
    }


    /**
     * Insert a string at the specified location
     *
     * @param index where to insert str
     * @param str   to be inserted
     * @throws IndexOutOfBoundsException in case of error
     */
    public synchronized void insertAt(int index, String str)
            throws IndexOutOfBoundsException {
        insertAt(index, str.getBytes());
    }

    /**
     * Insert a byte at the specified index location
     *
     * @param index location
     * @param b     byte value
     * @throws IndexOutOfBoundsException in case of error
     */
    public synchronized void writeAt(int index, byte b)
            throws IndexOutOfBoundsException {
        if (index < 0)
            throw new IndexOutOfBoundsException("Index " + index);

        if (index >= buf.length) {
            int newcount = index;
            buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount + 1));

            //buf[index] = b;
            count = newcount;
        }

        buf[index] = b;

    }

    public UByteArrayOutputStream clone() {
        return new UByteArrayOutputStream(getInternalBuffer(), 0, size());
    }

    /**
     * Insert a byte array at the specified location
     *
     * @param index where to insert the buffer
     * @param array data buffer to be inserted
     * @throws IndexOutOfBoundsException in case of error
     */
    public void insertAt(int index, byte[] array)
            throws IndexOutOfBoundsException {
        insertAt(index, array, 0, array.length);
    }

    /**
     * @param index  where to insert the buffer
     * @param array  data buffer to be inserted
     * @param offset offset relative to the array
     * @param length of array data to be inserted
     * @throws IndexOutOfBoundsException in case of indexation error
     */
    public synchronized void insertAt(int index, byte[] array, int offset, int length)
            throws IndexOutOfBoundsException {
        if (index < 0 || offset < 0 || length < 0) {
            throw new IndexOutOfBoundsException("Invalid index " + index + " or offset " + offset + " or length " + length);
        }

        if (index > size() || offset > length) {
            throw new IndexOutOfBoundsException("Index " + index + "bigger than size " + size() + " or offset > length " + (length - offset));
        }


        if (array.length == 0)
            return;


        int toCopyCount = length - offset;
        // set the new size
        int newCount = count + toCopyCount;
        if (newCount == count)
            return;

        // increase the buffer size;
        if (newCount > buf.length) {
            byte[] newbuf = new byte[Math.max(buf.length << 1, newCount)];
            System.arraycopy(buf, 0, newbuf, 0, count);
            buf = newbuf;
        }

        int remainderLength = count - index;
        byte[] remainderBuf = null;
        // copy the remainder

        if (remainderLength > 0) {
            remainderBuf = new byte[count - index];
            System.arraycopy(buf, index, remainderBuf, 0, remainderBuf.length);
        }

        // copy the array
        System.arraycopy(array, offset, buf, index, toCopyCount);

        // copy remainder;
        if (remainderLength > 0) {
            System.arraycopy(remainderBuf, 0, buf, index + toCopyCount, remainderBuf.length);
        }

        count = newCount;
    }

    @Override
    public synchronized boolean equals(Object obj) {
        if (obj instanceof UByteArrayOutputStream) {
            synchronized (obj) {
                UByteArrayOutputStream boas = (UByteArrayOutputStream) obj;

                if (size() == boas.size()) {
                    for (int i = 0; i < size(); i++) {
                        if (byteAt(i) != boas.byteAt(i)) {
                            return false;
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Remove from the output stream at index the specified length
     *
     * @param index  where to start removing data
     * @param length to be removed
     * @throws IndexOutOfBoundsException if the index or length are out of bound
     */
    public synchronized void removeAt(int index, int length)
            throws IndexOutOfBoundsException {
        if (index < 0 || length < 0 || index >= count || (index + length) > count) {
            throw new IndexOutOfBoundsException("Size " + count + " index " + index + " length " + length);
        }

        if (length == 0) {
            return;
        }

        resetValid();
        int newCount = count - length;

        System.arraycopy(buf, index + length, buf, index, count - (index + length));

        count = newCount;
    }


    public void reset() {
        reset(false);
    }

    public synchronized void reset(boolean zero) {
        super.reset();
        resetValid();
        if (zero)
            Arrays.fill(buf, (byte) 0);
    }

    /**
     * Shift the data left, basically shrinking the buffer this method is useful and efficient for protocol parsing and processing
     *
     * @param from start index
     * @param to   end index with constraint to < from
     * @return the buffer size after the shift
     * @throws IndexOutOfBoundsException in case the from and to are out of bound
     */
    public synchronized int shiftLeft(int from, int to) {
        if (to < 0 || from > count || to > count || to > from) {
            throw new IndexOutOfBoundsException("Size " + count + " from " + from + " to " + to);
        }

        if (from == count && to == 0) {
            // basically resetting the buffer
            count = 0;
        } else if (from != to) {
            System.arraycopy(buf, from, buf, to, count - from);
            count = count - (from - to);
        }

        resetValid();
        return count;
    }


    private synchronized void resetValid() {
        valid.set(false);
        valid = new AtomicBoolean(true);
    }

    /**
     * Returns the internal buffer, this method must be used with extreme care.
     * If you don't know what you are doing DON'T USED IT.
     * @return internal buffer
     */
    public byte[] getInternalBuffer() {
        return buf;
    }

    /**
     * @param startIndex from the internal buffer
     * @return a string from the startIndex till size()
     */
    public String getString(int startIndex) {
        int length = size() - startIndex;
        return SharedStringUtil.toString(getInternalBuffer(), startIndex, length);
    }

    /**
     *
     * @param indexStart from the internal buffer
     * @param length of the String
     * @return a string from start index with the requested length
     */
    public String getString(int indexStart, int length) {
        return SharedStringUtil.toString(getInternalBuffer(), indexStart, length);
    }

    public synchronized String toString(boolean withContent) {
        StringBuilder sb = new StringBuilder("Size  " + size() + " Buffer Length " + buf.length);
        if (withContent) {
            sb.append("\n");
            sb.append(SharedStringUtil.toString(buf, 0, size()));
        }

        return sb.toString();
    }

//    /**
//     * Write entire inner content to output stream based ong block size
//     *
//     * @param os        to write the data to
//     * @param blockSize to sent per write
//     * @return data send
//     * @throws IOException is case error
//     */
//    public synchronized int writeTo(OutputStream os, int blockSize) throws IOException {
//        return writeTo(os, blockSize, size());
//    }
//
//    /**
//     * Write inner content to output steam based on block size and max bytes to write
//     *
//     * @param os        to write the data to
//     * @param blockSize to sent per write
//     * @param maxBytes  to be sent
//     * @return data send
//     * @throws IOException is case error
//     */
//    public synchronized int writeTo(OutputStream os, int blockSize, int maxBytes) throws IOException {
//        SUS.checkIfNulls("Null outputStream", os);
//        if (blockSize < 1 || size() == 0 || maxBytes < 1 || maxBytes > size())
//            throw new IllegalArgumentException("Invalid block size: " + blockSize + " size: " + size());
//        if (blockSize > maxBytes)
//            blockSize = maxBytes;
//
//        int offset = 0;
//        do {
//            os.write(getInternalBuffer(), offset, blockSize);
//            offset += blockSize;
//            if (offset + blockSize > size()) {
//                blockSize = size() - offset;
//            }
//        }
//        while (offset != maxBytes);
//        return maxBytes;
//    }

    /**
     * @param copy if true will return a copy of the data if false direct access to the internal buff in this mode use it with extreme care
     * @return BytesArray object of actual data
     */
    public synchronized BytesArray toBytesArray(boolean copy) {
        return toBytesArray(copy, 0, size());
    }

    /**
     * @param copy   if true will return a copy of the data if false direct access to the internal buff in this mode use it with extreme care
     * @param offset offset
     * @param length length of data
     * @return BytesArray object of actual data, if copy is true the BytesArray.offset = 0 and BytesArray.length = length
     */
    public synchronized BytesArray toBytesArray(boolean copy, int offset, int length) {
        if (copy) {
            return new BytesArray(null, copyBytes(offset, offset + length), 0, length);
        }
        return new BytesArray(valid, getInternalBuffer(), offset, length);
    }


    /**
     * @param baos
     */
    public static void printInfo(UByteArrayOutputStream baos) {
        System.out.println("*************************************");
        System.out.println("Size  " + baos.size());
        System.out.println("Buffer Length " + baos.buf.length);
        System.out.println(new String(baos.buf, 0, baos.size()));
    }


}
