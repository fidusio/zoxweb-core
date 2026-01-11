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
import org.zoxweb.shared.util.CloseableType;
import org.zoxweb.shared.util.SUS;

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
 * This class provides the foundation for writing data to NIO channels in a stream-oriented manner,
 * bridging the gap between traditional {@link OutputStream} API and NIO channel-based I/O.
 * It handles buffering, synchronization, and provides common functionality for both
 * plain and SSL/TLS encrypted channel communications.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 *     <li>Extends {@link OutputStream} for compatibility with stream-based APIs</li>
 *     <li>Uses direct {@link ByteBuffer} for efficient I/O operations</li>
 *     <li>Thread-safe write operations via synchronization</li>
 *     <li>Integrates with {@link ProtocolHandler} for connection management</li>
 *     <li>Implements {@link CloseableType} for proper resource management</li>
 * </ul>
 *
 * @see ChannelOutputStream
 * @see org.zoxweb.server.net.ssl.SSLChannelOutputStream
 * @see ByteChannel
 * @see ProtocolHandler
 */
public abstract class BaseChannelOutputStream extends OutputStream
        implements CloseableType {

    /** Logger for debugging channel output operations */
    public static final LogWrapper log = new LogWrapper(BaseChannelOutputStream.class).setEnabled(false);

    /** The underlying NIO channel for writing data */
    protected final ByteChannel outChannel;
    /** Buffer for application data before writing to the channel */
    protected final ByteBuffer outAppData;
    /** Reusable single-byte buffer for write(int) operations */
    protected final byte[] oneByteBuffer = new byte[1];
    /** Flag indicating whether this stream has been closed */
    protected final AtomicBoolean isClosed = new AtomicBoolean(false);
    /** Protocol handler for connection lifecycle management */
    protected final ProtocolHandler protocolHandler;
    /** Remote client address associated with this channel */
    protected final InetSocketAddress clientAddress;

    /**
     * Constructs a new BaseChannelOutputStream with the specified parameters.
     *
     * @param ph the protocol handler for connection management, may be null
     * @param outByteChannel the NIO channel to write to, must not be null
     * @param outAppBufferSize the size of the output buffer in bytes, must be greater than 0
     * @throws IOException if unable to get the remote address from the channel
     * @throws NullPointerException if outByteChannel is null
     * @throws IllegalArgumentException if outAppBufferSize is not greater than 0
     */
    protected BaseChannelOutputStream(ProtocolHandler ph, ByteChannel outByteChannel, int outAppBufferSize) throws IOException {
        SUS.checkIfNulls("channel can't be null ", outByteChannel);
        this.clientAddress = (InetSocketAddress) ((SocketChannel) outByteChannel).getRemoteAddress();
        this.outChannel = outByteChannel;
        if (outAppBufferSize > 0) {
            outAppData = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.DIRECT, outAppBufferSize);
            this.protocolHandler = ph;
        } else
            throw new IllegalArgumentException("Invalid buffer size");
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
     * This method synchronizes on the provided stream to ensure thread-safe access
     * to its internal buffer.
     * </p>
     *
     * @param byteArrayOutputStream the source buffer to write from
     * @param reset if true, resets the source buffer after writing
     * @throws IOException if an I/O error occurs
     */
    public void write(UByteArrayOutputStream byteArrayOutputStream, boolean reset) throws IOException {
        synchronized (byteArrayOutputStream) {
            write(byteArrayOutputStream.getInternalBuffer(), 0, byteArrayOutputStream.size());
            if (reset)
                byteArrayOutputStream.reset();
        }
    }

    /**
     * Writes a portion of a byte array to the channel.
     * <p>
     * This method buffers the data and writes it to the channel in chunks
     * that fit the internal buffer size. It is synchronized to ensure thread-safe writes.
     * </p>
     *
     * @param b the byte array containing the data to write
     * @param off the start offset in the array
     * @param len the number of bytes to write
     * @throws IOException if an I/O error occurs
     * @throws IndexOutOfBoundsException if off or len are invalid
     */
    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        if (off > b.length || len > (b.length - off) || off < 0 || len < 0)
            throw new IndexOutOfBoundsException();
        // len == 0 condition implicitly handled by loop bounds
        while (off < len) {
            int tempLen = len - off;
            if (tempLen > (outAppData.capacity() - outAppData.position()))
                tempLen = outAppData.capacity() - outAppData.position();


            outAppData.put(b, off, tempLen);
            write(outAppData);
            off += tempLen;
        }
    }


    /**
     * Returns the remote client address associated with this channel.
     *
     * @return the remote socket address of the connected client
     */
    public InetSocketAddress getClientAddress(){
        return clientAddress;
    }

    /**
     * Writes the contents of a {@link ByteBuffer} to the underlying channel.
     * <p>
     * Subclasses must implement this method to handle the actual writing mechanism,
     * which may involve encryption (SSL/TLS) or direct channel writes.
     * The buffer should be in read mode (flipped) or the implementation should handle flipping.
     * </p>
     *
     * @param byteBuffer the buffer containing data to write
     * @return the number of bytes written to the channel
     * @throws IOException if an I/O error occurs during writing
     */
    public abstract int write(ByteBuffer byteBuffer) throws IOException;

    /**
     * Checks whether this output stream has been closed.
     *
     * @return true if this stream is closed or the underlying channel is not open
     */
    public boolean isClosed() {
        return isClosed.get() || !outChannel.isOpen();
    }
}
