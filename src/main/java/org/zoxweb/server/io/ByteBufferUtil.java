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

import org.zoxweb.server.util.ServerUtil;
import org.zoxweb.shared.util.*;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.*;
import java.util.concurrent.locks.Lock;

public class ByteBufferUtil {
    public enum BufferType {
        DIRECT,
        HEAP
    }

    private static final ByteBufferUtil SINGLETON = new ByteBufferUtil();

    final private Map<Integer, SimpleQueue<ByteBuffer>> cachedBuffers = new HashMap<Integer, SimpleQueue<ByteBuffer>>();
    final private Map<Integer, UniqueSimpleQueue<byte[]>> cachedByteArrays = new HashMap<>();
    final private SimpleQueue<UByteArrayOutputStream> cachedUBAOS = new SimpleQueue<UByteArrayOutputStream>();
    volatile private int count;
    volatile private int availableCapacity;

    public static final int DEFAULT_BUFFER_SIZE = 4096;
    /**
     * The maximum number of buffer cached per byte buffer capacity
     */
    public static final int CACHE_LIMIT = 512;


    public static final ByteBuffer EMPTY = allocateByteBuffer(0);

    private ByteBufferUtil() {


    }

    private void cache0(UByteArrayOutputStream ubaos) {
        if (cachedUBAOS.size() < CACHE_LIMIT && ubaos != null && ubaos.size() <= Const.SizeInBytes.K.SIZE) {
            synchronized (this) {
                ubaos.reset();
                cachedUBAOS.queue(ubaos);
                availableCapacity += ubaos.getInternalBuffer().length;
            }
        }
    }


    private void cache0(byte[] ba) {
        if (ba != null && ba.length != 0)
            synchronized (this) {
                UniqueSimpleQueue<byte[]> usq = cachedByteArrays.get(ba.length);
                if (usq == null) {
                    usq = new UniqueSimpleQueue<byte[]>();
                    cachedByteArrays.put(ba.length, usq);
                }

                if (usq.size() < CACHE_LIMIT)
                    usq.queue(ba);
            }
    }

    private void cache0(ByteBuffer bb) {
        synchronized (this) {
            if (bb != null) {
                SimpleQueue<ByteBuffer> sq = cachedBuffers.get(bb.capacity());

                if (sq == null) {
                    sq = new SimpleQueue<ByteBuffer>(false);
                    cachedBuffers.put(bb.capacity(), sq);
                }

                if (sq.size() < CACHE_LIMIT) {
                    ((Buffer) bb).clear();
                    if (!sq.contains(bb)) {
                        sq.queue(bb);
                        availableCapacity += bb.capacity();
                        count++;
                    }
                }
            }
        }
    }


    private byte[] toByteArray0(int length) {
        if (length < 0)
            throw new IllegalArgumentException("byte array size < 0 " + length);
        if(length == 0)
            return Const.EMPTY_BYTE_ARRAY;

        UniqueSimpleQueue<byte[]> usq = null;
        byte[] ba = null;
        synchronized (this) {
            usq = cachedByteArrays.get(length);
            if (usq != null) {
                ba = usq.dequeue();
            }
        }
        if (ba == null)
            ba = new byte[length];

        return ba;
    }

    private ByteBuffer toByteBuffer0(BufferType bType, byte[] buffer, int offset, int length, boolean copy) {
        ByteBuffer bb = null;
        SimpleQueue<ByteBuffer> sq = null;

        synchronized (this) {
            sq = cachedBuffers.get(length - offset);

            if (sq != null) {
                bb = sq.dequeue();
                if (bb != null) {
                    availableCapacity -= bb.capacity();
                    count--;
                }
            }
        }

        if (bb == null) {
            switch (bType) {
                case DIRECT:
                    bb = ByteBuffer.allocateDirect(length - offset);
                    break;
                case HEAP:
                    bb = ByteBuffer.allocate(length - offset);
                    break;
            }
            //log.info("["+ (counter++) + "]must create new buffer:" + bb.capacity() + " " + bb.getClass().getName());
        }

        if (copy) {
            bb.put(buffer, offset, length);
            ((Buffer) bb).flip();
        }


        return bb;

    }


    public static void write(ByteChannel bc, UByteArrayOutputStream ubaos) throws IOException {
        write(bc, ubaos.getInternalBuffer(), 0, ubaos.size());
    }

    public static void write(ByteChannel bc, byte[] array, int off, int len) throws IOException {
        SUS.checkIfNulls("null byte channel", bc);

        if (off < 0) {
            throw new IllegalArgumentException("invalid offset " + off);
        }

        ByteBuffer bb = allocateByteBuffer(BufferType.HEAP, DEFAULT_BUFFER_SIZE);

        try {
            int end = off + len;

            if (end > array.length) {
                end = array.length;
            }

            for (int offset = off; offset < end; ) {
                int length = offset + bb.capacity() > end ? end - offset : bb.capacity();

                ((Buffer) bb).clear();
                bb.put(array, offset, length);
                offset += length;
                write(bc, bb);
            }
        } finally {
            cache(bb);
        }
    }


    public static byte[] allocateByteArray(int length) {
        return SINGLETON.toByteArray0(length);
    }

    public static ByteBuffer allocateByteBuffer(int capacity) {
        return SINGLETON.toByteBuffer0(BufferType.HEAP, null, 0, capacity, false);
    }

    public static ByteBuffer allocateByteBuffer(BufferType bType) {
        return SINGLETON.toByteBuffer0(bType, null, 0, DEFAULT_BUFFER_SIZE, false);
    }

    public static ByteBuffer allocateByteBuffer(BufferType bType, int capacity) {
        return SINGLETON.toByteBuffer0(bType, null, 0, capacity, false);
    }

    public static ByteBuffer allocateByteBuffer(BufferType bType, byte[] buffer, int offset, int length, boolean copy) {
        return SINGLETON.toByteBuffer0(bType, buffer, offset, length, copy);
    }

    private UByteArrayOutputStream toUBAOS0(int capacity) {
        if (capacity <= 1024) {
            synchronized (this) {
                UByteArrayOutputStream ret = cachedUBAOS.dequeue();

                if (ret != null) {
                    availableCapacity -= ret.getInternalBuffer().length;
                    return ret;
                }
            }
        }

        return new UByteArrayOutputStream(capacity);
    }

    public static UByteArrayOutputStream allocateUBAOS(int capacity) {
        return SINGLETON.toUBAOS0(capacity);
    }

    public static int write(ByteChannel bc, ByteBuffer bb) throws IOException {
        ((Buffer) bb).flip();
        int totalWritten = 0;
        while (bb.hasRemaining()) {
            int written = bc.write(bb);
            if (written == -1)
                return -1;

            totalWritten += written;
        }
        return totalWritten;
    }


    public static int smartWrite(Lock lock, ByteChannel bc, ByteBuffer bb) throws IOException {
        return smartWrite(lock, bc, bb, true);
    }

    public static int smartWrite(Lock lock, ByteChannel bc, ByteBuffer bb, boolean flip) throws IOException {
        int totalWritten = 0;
        ServerUtil.lock(lock);

        try {
            if (flip)
                ((Buffer) bb).flip();

            while (bb.hasRemaining()) {
                int written = bc.write(bb);
                if (written == -1)
                    return -1;
                totalWritten += written;
            }

            bb.compact();
        } finally {
            ServerUtil.unlock(lock);
        }
        return totalWritten;
    }

    public static String toString(ByteBuffer bb) throws IOException {
        UByteArrayOutputStream ubaos = new UByteArrayOutputStream();
        write(bb, ubaos, true);

        return ubaos.toString();
    }


    public static void write(ByteBuffer bbSrc, UByteArrayOutputStream ubaosDst, boolean flip) throws IOException {
        if (flip)
            ((Buffer) bbSrc).flip();

        for (int i = 0; i < bbSrc.limit(); i++) {
            ubaosDst.write(bbSrc.get());
        }
        if (flip)
            bbSrc.compact();
    }


    public static void write(UByteArrayOutputStream baosSrc, ByteBuffer bbrDst) throws IOException {
        for (int i = 0; i < baosSrc.size(); i++) {
            bbrDst.put(baosSrc.byteAt(i));
        }
    }

    public static void cache(ByteBuffer... buffers) {
        if (buffers != null) {
            for (ByteBuffer bb : buffers)
                SINGLETON.cache0(bb);
        }
    }

    public static void cache(byte[]... buffers) {
        if (buffers != null) {
            for (byte[] ba : buffers)
                SINGLETON.cache0(ba);
        }
    }

    public static void cache(UByteArrayOutputStream... buffers) {
        if (buffers != null) {
            for (UByteArrayOutputStream bb : buffers)
                SINGLETON.cache0(bb);
        }
    }


    public static byte[] toBytes(ByteBuffer buffer, boolean flip) {
        if (buffer != null) {
            if (flip)
                buffer.flip();
            // Create a new array sized to the remaining elements in the ByteBuffer
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return bytes;
        }

        return null;
    }

    public static ByteBuffer toByteBuffer(BytesArray ba) {
        if (ba != null) {

            if (!ba.isValid())
                throw new IllegalArgumentException("byte array not valid");

            byte[] bytes = ba.asBytes();

            return allocateByteBuffer(BufferType.HEAP, bytes, 0, bytes.length, true);
        }

        return null;
    }

    /**
     * @return the number of byte buffers in the cache
     */
    public static int cacheCount() {
        return SINGLETON.count;
    }

    public static int baosCount() {
        return SINGLETON.cachedUBAOS.size();
    }

    /**
     * @return total bytes of the available byte buffers
     */
    public static int cacheCapacity() {
        return SINGLETON.availableCapacity;
    }


}

