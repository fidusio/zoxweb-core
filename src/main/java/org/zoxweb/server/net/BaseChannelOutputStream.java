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
package org.zoxweb.server.net;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.io.CloseableType;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.UsageTracker;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract base class for channel-based output streams using NIO {@link ByteChannel} and {@link ByteBuffer}.
 * <p>
 * Bridges the traditional {@link OutputStream} API to NIO channels. Stream-style
 * writes ({@code write(int)}, {@code write(byte[], int, int)},
 * {@code write(UByteArrayOutputStream, boolean)}) are translated into
 * {@link ByteBuffer}-based channel writes handed to the subclass-provided
 * {@link #write(ByteBuffer, boolean)}, which does the actual encrypted or
 * plaintext transmission.
 * </p>
 * <p>
 * <b>Buffer-mode convention.</b> The abstract {@link #write(ByteBuffer, boolean)}
 * takes a {@code flip} flag:
 * </p>
 * <ul>
 *     <li>{@code flip=true} — caller's buffer is in <b>write-mode</b>
 *         (position = end of data, limit = capacity); the implementation will
 *         flip before draining and compact after.</li>
 *     <li>{@code flip=false} — caller's buffer is already in <b>read-mode</b>
 *         (position = start of data, limit = end of data); no flip needed.</li>
 * </ul>
 * <p>
 * All built-in callers in this class use {@link ByteBuffer#wrap(byte[], int, int)}
 * or equivalent to produce read-mode buffers and pass {@code flip=false}.
 * </p>
 *
 * @see ByteChannel
 * @see ProtocolHandler
 */
public abstract class BaseChannelOutputStream extends OutputStream
        implements CloseableType {

    /** Per-class logger; disabled by default. */
    public static final LogWrapper log = new LogWrapper(BaseChannelOutputStream.class).setEnabled(false);

    /** The underlying NIO channel for writing data. */
    protected final ByteChannel dataChannel;
    /** Reusable single-byte buffer backing {@link #write(int)}. */
    protected final byte[] oneByteBuffer = new byte[1];
    /** Set to {@code true} the first time {@link #close()} is called (in subclasses). */
    protected final AtomicBoolean isClosed = new AtomicBoolean(false);
    /** Activity tracker notified on each successful write; may be {@code null}. */
    protected final UsageTracker usageTracker;
    /** Remote peer address captured at construction. */
    protected final InetSocketAddress clientAddress;
    protected volatile ByteBuffer outAppData;

    /**
     * Constructs a new output stream bound to the given NIO channel.
     *
     * @param ut             activity tracker, may be {@code null}
     * @param outByteChannel the NIO channel to write to; must be a connected {@link SocketChannel}
     * @throws IOException          if the remote address cannot be read from the channel
     * @throws NullPointerException if {@code outByteChannel} is {@code null}
     */
    protected BaseChannelOutputStream(ProtocolHandler ut, ByteChannel outByteChannel, boolean useAppDataBuffer) throws IOException {
        SUS.checkIfNulls("channel can't be null ", outByteChannel);
        this.clientAddress = (InetSocketAddress) ((SocketChannel) outByteChannel).getRemoteAddress();
        this.dataChannel = outByteChannel;
        this.usageTracker = ut;
        if (useAppDataBuffer)
            outAppData = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP, SharedIOUtil.K_4);
    }


    /**
     * Writes a single byte to the channel.
     * <p>
     * This method is synchronized to ensure thread-safe writes.
     * </p>
     *
     * @param b the byte to write (only the low 8 bits are used)
     * @throws IOException if an I/O error occurs
     */
    @Override
    public synchronized void write(int b) throws IOException {
        oneByteBuffer[0] = (byte) b;
        write(oneByteBuffer, 0, 1);
    }

    /**
     * Writes the contents of a {@link UByteArrayOutputStream} to the channel.
     * <p>
     * Wraps the output stream's internal byte array as a read-mode {@link ByteBuffer}
     * and hands it to {@link #write(ByteBuffer, boolean)} with {@code flip=false}.
     * Synchronizes on {@code byteArrayOutputStream} to serialize with concurrent
     * writers into the same stream.
     * </p>
     *
     * @param byteArrayOutputStream source buffer (must not be mutated during the call)
     * @param reset                 if {@code true}, {@link UByteArrayOutputStream#reset()} is called after the write
     * @throws IOException if an I/O error occurs during writing
     */
    public void write(UByteArrayOutputStream byteArrayOutputStream, boolean reset) throws IOException {
        SUS.checkIfNulls("byteArrayOutputStream can't be null ", byteArrayOutputStream);
        synchronized (byteArrayOutputStream) {
            write(byteArrayOutputStream.getInternalBuffer(), 0, byteArrayOutputStream.size());
            if (reset)
                byteArrayOutputStream.reset();
        }
    }

    /**
     * Writes {@code len} bytes from {@code b} starting at {@code off} to the channel.
     * <p>
     * Produces a read-mode {@link ByteBuffer} view over the slice via
     * {@link ByteBuffer#wrap(byte[], int, int)} and delegates to
     * {@link #write(ByteBuffer, boolean)} with {@code flip=false}. No staging
     * buffer is used; the subclass' chunking logic (TLS records etc.) takes
     * care of any size limits.
     * </p>
     *
     * @param b   source array
     * @param off start offset in {@code b}
     * @param len number of bytes to write
     * @throws IOException               if an I/O error occurs
     * @throws IndexOutOfBoundsException if {@code off}, {@code len}, or {@code off + len} is out of bounds
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (off < 0 || len < 0 || off > b.length - len)
            throw new IndexOutOfBoundsException();


        // added duel mode from performance tuning !?!?
        if (outAppData == null)
            write(ByteBuffer.wrap(b, off, len), false);
        else {
            int end = off + len;
            while (off < end) {
                int tempLen = Math.min(end - off, outAppData.capacity() - outAppData.position());
                outAppData.put(b, off, tempLen);
                write(outAppData, true);
                off += tempLen;
            }
        }
    }


    /**
     * Returns the remote client address associated with this channel.
     *
     * @return the remote socket address of the connected client
     */
    public InetSocketAddress getClientAddress() {
        return clientAddress;
    }

    /**
     * Sends the contents of a {@link ByteBuffer} to the underlying channel.
     * <p>
     * Subclasses implement this for plain or SSL/TLS transmission. The
     * {@code flip} flag conveys the caller's buffer mode:
     * </p>
     * <ul>
     *     <li>{@code flip=true} — {@code byteBuffer} is in <b>write-mode</b>
     *         (position = end of data, limit = capacity); implementation must
     *         flip before draining.</li>
     *     <li>{@code flip=false} — {@code byteBuffer} is in <b>read-mode</b>
     *         (position = start of data, limit = end of data); no flip needed.</li>
     * </ul>
     * <p>
     * Implementations typically end by compacting (restoring write-mode) so the
     * caller's buffer can be reused; callers passing {@code flip=false} over
     * throwaway {@code wrap(...)}-produced buffers should not rely on any
     * particular post-condition.
     * </p>
     *
     * @param byteBuffer plaintext to write
     * @param flip       see above — describes the caller's mode
     * @return number of bytes transmitted to the channel (ciphertext bytes in SSL mode), or -1 on EOF
     * @throws IOException on I/O error; the stream is typically closed before the exception propagates
     */
    public abstract int write(ByteBuffer byteBuffer, boolean flip) throws IOException;


    /**
     * Checks whether this output stream has been closed.
     *
     * @return true if this stream is closed or the underlying channel is not open
     */
    public boolean isClosed() {
        return isClosed.get() || !dataChannel.isOpen();
    }


}
