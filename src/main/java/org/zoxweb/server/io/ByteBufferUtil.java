/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
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
import org.zoxweb.shared.io.BytesArray;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SimpleQueue;
import org.zoxweb.shared.util.UniqueSimpleQueue;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * The NIO buffer utility and pooling hub: a singleton cache of reusable {@link ByteBuffer}s
 * (pooled by exact capacity), {@code byte[]}s (pooled by exact length), and
 * {@link UByteArrayOutputStream}s, plus the transfer helpers that move bytes between channels,
 * buffers, byte arrays, and accumulator streams.
 * <p>
 * The pooling is load-proven: under sustained NIO traffic, per-dispatch allocation thrashes the
 * GC, so buffers are recycled instead. The contract is <b>allocate → use → cache</b>: obtain
 * through an {@code allocate*} method, return through the matching {@link #cache} overload at
 * most once, and never touch a buffer again after caching it — the same instance is handed to
 * the next caller. Caching is best-effort: each per-size pool is capped at {@link #CACHE_LIMIT}
 * entries, and a buffer above the cap (or one that never came from the pool) is simply dropped
 * to the GC.
 * <p>
 * The channel write helpers are one-shot complete by design: {@code write}/{@code smartWrite}
 * loop until the buffer is fully drained before returning — there is no partial-write queue.
 */
public class ByteBufferUtil {
    /** The two {@link ByteBuffer} allocation flavors: off-heap {@code DIRECT} or on-heap {@code HEAP}. */
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


    /**
     * The maximum number of buffer cached per byte buffer capacity
     */
    public static final int CACHE_LIMIT = 512;


    /** A shared zero-capacity buffer for APIs that require a non-null placeholder. Never cache it. */
    public static final ByteBuffer EMPTY = allocateByteBuffer(0);

    private ByteBufferUtil() {


    }


    /**
     * UTF-8 encodes a char array to bytes without going through a {@link String} — no interned
     * copy of the content is left behind (the password/secret-handling path).
     *
     * @param chars the characters to encode
     * @return the UTF-8 bytes
     */
    public static byte[] toBytes(char[] chars) {
        Charset utf8 = StandardCharsets.UTF_8;
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = utf8.encode(charBuffer);
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }

    /** Recaches a UBAOS after resetting it; only small ones (≤ 4K internal buffer) are kept. */
    private void cache0(UByteArrayOutputStream ubaos) {
        if (cachedUBAOS.size() < CACHE_LIMIT && ubaos != null && ubaos.size() <= SharedIOUtil.K_4) {
            synchronized (this) {
                ubaos.reset();
                cachedUBAOS.queue(ubaos);
                availableCapacity += ubaos.getInternalBuffer().length;
            }
        }
    }


    /** Recaches a byte array into its exact-length pool bucket, zeroed so no data leaks to the next user. */
    private void cache0(byte[] ba) {
        if (ba != null && ba.length != 0)
            synchronized (this) {
                UniqueSimpleQueue<byte[]> usq = cachedByteArrays.get(ba.length);
                if (usq == null) {
                    usq = new UniqueSimpleQueue<byte[]>();
                    cachedByteArrays.put(ba.length, usq);
                }

                if (usq.size() < CACHE_LIMIT) {
                    Arrays.fill(ba, (byte) 0);
                    usq.queue(ba);
                }
            }
    }

    /** Recaches a ByteBuffer into its exact-capacity pool bucket, cleared; duplicates are ignored. */
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


    /** A pooled (or new) byte array of exactly {@code length} bytes; content is zeroed by the recache path. */
    private byte[] toByteArray0(int length) {
        if (length < 0)
            throw new IllegalArgumentException("byte array size < 0 " + length);
        if (length == 0)
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

    /**
     * Zero-copy read-mode view over a {@link BytesArray}'s backing array. The view is only as
     * stable as the BytesArray's validity window — validity is checked here, not on later reads.
     *
     * @param ba the byte-array slice to wrap
     * @return a ByteBuffer sharing the slice's backing array
     */
    public static ByteBuffer wrap(BytesArray ba) {

        ba.checkValidity();
        return ByteBuffer.wrap(ba.getArray(), ba.offset, ba.length);

    }


    /**
     * Zero-copy read-mode view over a UBAOS's current content. Any mutation of the stream
     * (write, shiftLeft, reset) invalidates the view — consume it before touching the stream.
     *
     * @param ubaos the accumulator to wrap
     * @return a ByteBuffer sharing the accumulator's internal buffer, [0, size())
     */
    public static ByteBuffer wrap(UByteArrayOutputStream ubaos) {
        return ByteBuffer.wrap(ubaos.getInternalBuffer(), 0, ubaos.size());
    }

    /**
     * A pooled (or new) ByteBuffer of capacity {@code length - offset}; with {@code copy} the
     * source slice is copied in and the buffer returned flipped (read mode), otherwise it is
     * empty in write mode.
     */
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


    /**
     * Writes a UBAOS's entire content to the channel, fully — direct from the internal buffer,
     * no content copy beyond the pooled transfer chunk.
     *
     * @param bc    the destination channel
     * @param ubaos the accumulator to drain (its content is left untouched)
     * @throws IOException on a channel write error
     */
    public static void write(ByteChannel bc, UByteArrayOutputStream ubaos) throws IOException {
        write(bc, ubaos.getInternalBuffer(), 0, ubaos.size());
    }

    /**
     * Writes {@code array[off, off+len)} to the channel completely, staged through a pooled 4K
     * ByteBuffer (recached in a finally). A {@code len} reaching past the array end is clamped
     * to the array's length rather than failing.
     *
     * @param bc    the destination channel
     * @param array the source bytes
     * @param off   start offset within the array
     * @param len   number of bytes to write (clamped to the array end)
     * @throws IOException on a channel write error
     */
    public static void write(ByteChannel bc, byte[] array, int off, int len) throws IOException {
        SUS.checkIfNulls("null byte channel", bc);

        if (off < 0) {
            throw new IllegalArgumentException("invalid offset " + off);
        }

        ByteBuffer bb = allocateByteBuffer(BufferType.HEAP, SharedIOUtil.K_4);

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


    /**
     * A pooled (or new) byte array of exactly {@code length} bytes. Return it with
     * {@link #cache(byte[]...)} when done; the recache path zeroes it.
     *
     * @param length the exact array length required
     * @return a zero-filled array of that length
     */
    public static byte[] allocateByteArray(int length) {
        return SINGLETON.toByteArray0(length);
    }

    /**
     * Drains {@code bb} into a pooled byte array sized to the buffer's limit. The buffer must be
     * at position 0 after the optional flip (a fresh write-mode buffer with {@code flip}, or a
     * freshly flipped one without) — a partially consumed buffer underflows.
     *
     * @param bb   the source buffer, fully consumed by the call
     * @param flip true to flip {@code bb} from write mode first
     * @return a pooled array holding exactly the buffer's content
     */
    public static byte[] allocateByteArray(ByteBuffer bb, boolean flip) {
        if (flip) bb.flip();

        byte[] ret = SINGLETON.toByteArray0(bb.limit());
        bb.get(ret);
        return ret;
    }

    /**
     * A pooled (or new) empty heap ByteBuffer of exactly {@code capacity}, in write mode.
     * Return it with {@link #cache(ByteBuffer...)} when done.
     *
     * @param capacity the exact capacity required
     * @return an empty write-mode buffer
     */
    public static ByteBuffer allocateByteBuffer(int capacity) {
        return SINGLETON.toByteBuffer0(BufferType.HEAP, null, 0, capacity, false);
    }

    /**
     * A pooled (or new) empty ByteBuffer of the default 4K capacity, in write mode.
     *
     * @param bType direct or heap
     * @return an empty write-mode buffer
     */
    public static ByteBuffer allocateByteBuffer(BufferType bType) {
        return SINGLETON.toByteBuffer0(bType, null, 0, SharedIOUtil.K_4, false);
    }

    /**
     * A pooled (or new) empty ByteBuffer of exactly {@code capacity}, in write mode.
     *
     * @param bType    direct or heap
     * @param capacity the exact capacity required
     * @return an empty write-mode buffer
     */
    public static ByteBuffer allocateByteBuffer(BufferType bType, int capacity) {
        return SINGLETON.toByteBuffer0(bType, null, 0, capacity, false);
    }

    /**
     * A pooled (or new) ByteBuffer of capacity {@code length - offset}; with {@code copy} the
     * source slice is copied in and the buffer returned flipped (read mode), without it the
     * buffer comes back empty in write mode and the slice only sizes it.
     *
     * @param bType  direct or heap
     * @param buffer source array (used only when {@code copy})
     * @param offset slice start
     * @param length slice end (exclusive)
     * @param copy   true to copy the slice in and flip
     * @return the buffer, read mode when {@code copy}, else empty write mode
     */
    public static ByteBuffer allocateByteBuffer(BufferType bType, byte[] buffer, int offset, int length, boolean copy) {
        return SINGLETON.toByteBuffer0(bType, buffer, offset, length, copy);
    }

    /** A pooled (or new) UBAOS; only small requests (≤ 1K) are served from the pool. */
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

    /**
     * A pooled (or new) empty {@link UByteArrayOutputStream}. Return it with
     * {@link #cache(UByteArrayOutputStream...)} when done — e.g. a session accumulator recached
     * once at teardown.
     *
     * @param capacity initial capacity hint (a pooled instance may be smaller and will grow)
     * @return an empty accumulator
     */
    public static UByteArrayOutputStream allocateUBAOS(int capacity) {
        return SINGLETON.toUBAOS0(capacity);
    }

    /** {@link #write(ByteChannel, ByteBuffer, boolean)} with {@code flip = true}. */
    public static int write(ByteChannel bc, ByteBuffer bb) throws IOException {
        return write(bc, bb, true);
    }


    /**
     * Writes the buffer to the channel <b>completely</b> — loops until nothing remains (one-shot
     * complete write, by design; there is no partial-write queue). The buffer is left fully
     * consumed but not compacted — see {@link #smartWrite} for the compacting, locked variant.
     *
     * @param bc   the destination channel
     * @param bb   the data to write
     * @param flip true if {@code bb} is in write mode and must be flipped first
     * @return total bytes written, or -1 if the channel reported end-of-stream
     * @throws IOException on a channel write error
     */
    public static int write(ByteChannel bc, ByteBuffer bb, boolean flip) throws IOException {
        if (flip) ((Buffer) bb).flip();

        int totalWritten = 0;
        while (bb.hasRemaining()) {
            int written = bc.write(bb);
            if (written == -1)
                return -1;

            totalWritten += written;
        }
        return totalWritten;
    }


//    public static int smartWrite(Lock lock, ByteChannel bc, ByteBuffer bb) throws IOException {
//        return smartWrite(lock, bc, bb, true);
//    }

//    public static int send(Lock lock, DatagramChannel channel, DataPacket<?> dataPacket, boolean flip) throws IOException {
//        return send(lock, channel, dataPacket.getIOBuffers().getInBuffer(), dataPacket.getAddress(), flip);
//    }

    /**
     * Sends the buffer as one datagram under the (optional) lock. Unlike the stream writers this
     * is single-shot — UDP either sends the whole datagram or nothing.
     *
     * @param lock               serializes senders sharing the channel; null for none
     * @param channel            the datagram channel
     * @param bb                 the datagram payload
     * @param destinationAddress the target address (ignored by a connected channel)
     * @param flip               true if {@code bb} is in write mode and must be flipped first
     * @return bytes sent, 0 if the send could not complete
     * @throws IOException on a channel error
     */
    public static int send(Lock lock, DatagramChannel channel, ByteBuffer bb, SocketAddress destinationAddress, boolean flip) throws IOException {
        int sent = 0;
        if (flip)
            bb.flip();

        ServerUtil.lock(lock);
        try {
            sent = channel.send(bb, destinationAddress);
        } finally {
            ServerUtil.unlock(lock);
        }

        return sent;
    }

    /**
     * The session write primitive: under the (optional) lock, writes the buffer to the channel
     * completely — looping until drained (one-shot complete write, by design) — then
     * {@code compact()}s it so a reused session buffer is ready for its next fill.
     *
     * @param lock serializes writers sharing the channel; null for none
     * @param bc   the destination channel
     * @param bb   the data to write, compacted after the drain
     * @param flip true if {@code bb} is in write mode and must be flipped first
     * @return total bytes written, or -1 if the channel reported end-of-stream (buffer not compacted)
     * @throws IOException on a channel write error
     */
    public static int smartWrite(Lock lock, ByteChannel bc, ByteBuffer bb, boolean flip) throws IOException {
        int totalWritten = 0;
        ServerUtil.lock(lock);

        try {
            if (flip)
                bb.flip();

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

    /**
     * Debug helper: drains a write-mode buffer's content into a string of its size/length info.
     * The buffer is flipped and compacted (content consumed).
     *
     * @param bb the buffer to render
     * @return the accumulator's {@code toString} of the drained content
     * @throws IOException never in practice (in-memory copy)
     */
    public static String toString(ByteBuffer bb) throws IOException {
        UByteArrayOutputStream ubaos = new UByteArrayOutputStream();
        write(bb, ubaos, true);

        return ubaos.toString();
    }


    /**
     * Bulk-copies every remaining byte of {@code bbSrc} into {@code ubaosDst} — the
     * NIO-buffer-to-accumulator transfer. A heap buffer is copied in one bulk write straight
     * from its backing array; a direct buffer is drained through a pooled 4K chunk. Either way
     * the data is copied exactly once and the source is left fully consumed.
     * <p>
     * {@code flip} declares the source's mode: {@code true} for a write-mode buffer — flipped
     * before the copy and compacted after, so a reused read buffer is ready for its next fill
     * (the per-dispatch NIO idiom); {@code false} for a buffer already in read mode — its
     * remaining bytes are consumed and no mode change is made.
     *
     * @param bbSrc    the source buffer
     * @param ubaosDst the destination accumulator
     * @param flip     true if {@code bbSrc} is in write mode (flip before, compact after)
     * @throws IOException declared for caller compatibility; the copy itself performs no I/O
     */
    public static void write(ByteBuffer bbSrc, UByteArrayOutputStream ubaosDst, boolean flip) throws IOException {
        if (flip)
            ((Buffer) bbSrc).flip();

        int remaining = bbSrc.remaining();
        if (remaining > 0) {
            if (bbSrc.hasArray()) {
                ubaosDst.write(bbSrc.array(), bbSrc.arrayOffset() + bbSrc.position(), remaining);
                ((Buffer) bbSrc).position(bbSrc.limit());
            } else {
                // fixed-size pooled chunk — a remaining-sized one would seed odd-size pool buckets
                byte[] chunk = allocateByteArray(SharedIOUtil.K_4);
                try {
                    while (bbSrc.hasRemaining()) {
                        int length = Math.min(bbSrc.remaining(), chunk.length);
                        bbSrc.get(chunk, 0, length);
                        ubaosDst.write(chunk, 0, length);
                    }
                } finally {
                    cache(chunk);
                }
            }
        }
        if (flip)
            bbSrc.compact();
    }


    /**
     * Copies a UBAOS's entire content into the destination buffer via relative puts. The caller
     * guarantees {@code bbrDst} has at least {@code baosSrc.size()} bytes remaining — overflow
     * throws {@link java.nio.BufferOverflowException}.
     *
     * @param baosSrc the source accumulator (left untouched)
     * @param bbrDst  the destination buffer, in write mode
     * @throws IOException declared for caller compatibility; the copy itself performs no I/O
     */
    public static void write(UByteArrayOutputStream baosSrc, ByteBuffer bbrDst) throws IOException {
        bbrDst.put(baosSrc.getInternalBuffer(), 0, baosSrc.size());
    }

    /**
     * Returns ByteBuffers to the pool (cleared, requeued by capacity, capped). Null-safe; never
     * touch a buffer again after caching it.
     *
     * @param buffers the buffers to recache
     */
    public static void cache(ByteBuffer... buffers) {
        if (buffers != null) {
            for (ByteBuffer bb : buffers)
                SINGLETON.cache0(bb);
        }
    }


    /**
     * Recaches both buffers of the pair, exactly once per pair: gated on the pair's
     * one-shot {@link IOBuffers#canCache()} token, which {@link IOBuffers#close()} also
     * consumes — repeated calls, or any cache/close combination, recache only once.
     * Null-safe, and null buffers within the pair are skipped.
     */
    public static void cache(IOBuffers ioBuffers) {
        if (ioBuffers != null && ioBuffers.canCache()) {
            SINGLETON.cache0(ioBuffers.getInBuffer());
            SINGLETON.cache0(ioBuffers.getOutBuffer());
        }
    }

    /**
     * Returns byte arrays to their exact-length pool buckets, zeroed so no content leaks to the
     * next user. Null-safe; never touch an array again after caching it.
     *
     * @param buffers the arrays to recache
     */
    public static void cache(byte[]... buffers) {
        if (buffers != null) {
            for (byte[] ba : buffers)
                SINGLETON.cache0(ba);
        }
    }

    /**
     * Returns UBAOS accumulators to the pool (reset; only small ones are kept). Null-safe; never
     * touch a stream again after caching it — the teardown-recache counterpart of
     * {@link #allocateUBAOS(int)}.
     *
     * @param buffers the accumulators to recache
     */
    public static void cache(UByteArrayOutputStream... buffers) {
        if (buffers != null) {
            for (UByteArrayOutputStream bb : buffers)
                SINGLETON.cache0(bb);
        }
    }


    /**
     * Drains the buffer's remaining bytes into a <b>new</b> (unpooled) exactly-sized array —
     * unlike {@link #allocateByteArray(ByteBuffer, boolean)}, safe on a partially consumed
     * buffer, and the result never needs caching.
     *
     * @param buffer the source, fully consumed by the call; null tolerated
     * @param flip   true if the buffer is in write mode and must be flipped first
     * @return the remaining bytes, or null for a null buffer
     */
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

    /**
     * Copies a {@link BytesArray}'s content into a pooled heap ByteBuffer, returned flipped
     * (read mode) — the copying counterpart of the zero-copy {@link #wrap(BytesArray)}.
     *
     * @param ba the byte-array slice; null tolerated
     * @return a read-mode buffer holding a copy of the slice, or null for a null input
     * @throws IllegalArgumentException if the slice is no longer valid
     */
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

    /**
     * @return the number of {@link UByteArrayOutputStream}s in the cache
     */
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

