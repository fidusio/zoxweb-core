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
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.shared.util.Const;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * A non-encrypted channel output stream for writing plaintext data to NIO channels.
 * <p>
 * This class provides a straightforward implementation of {@link BaseChannelOutputStream}
 * for scenarios where SSL/TLS encryption is not required. Data is written directly to
 * the underlying {@link ByteChannel} without any transformation.
 * </p>
 * <p>
 * Usage example:
 * <pre>{@code
 * SocketChannel channel = SocketChannel.open(new InetSocketAddress("localhost", 8080));
 * try (ChannelOutputStream out = new ChannelOutputStream(channel)) {
 *     out.write("Hello, World!".getBytes());
 *     out.flush();
 * }
 * }</pre>
 * </p>
 *
 * @see BaseChannelOutputStream
 * @see org.zoxweb.server.net.ssl.SSLChannelOutputStream
 * @see ByteChannel
 */
public class ChannelOutputStream
        extends BaseChannelOutputStream {

    /**
     * Constructs a new ChannelOutputStream with default buffer size (1024 bytes).
     *
     * @param byteChannel the NIO channel to write to
     * @throws IOException if unable to get the remote address from the channel
     */
    public ChannelOutputStream(ByteChannel byteChannel) throws IOException {
        this(null, byteChannel, Const.SizeInBytes.K.mult(1));
    }

    /**
     * Constructs a new ChannelOutputStream with the specified parameters.
     *
     * @param ph the protocol handler for connection management, may be null
     * @param byteChannel the NIO channel to write to
     * @param outAppBufferSize the size of the output buffer in bytes
     * @throws IOException if unable to get the remote address from the channel
     */
    public ChannelOutputStream(ProtocolHandler ph, ByteChannel byteChannel, int outAppBufferSize) throws IOException {
        super(ph, byteChannel, outAppBufferSize);
    }


    /**
     * Writes the contents of a {@link ByteBuffer} directly to the underlying channel.
     * <p>
     * This method performs a smart write operation that handles partial writes
     * and ensures all data in the buffer is sent. The buffer is automatically
     * flipped and compacted as needed.
     * </p>
     *
     * @param bb the buffer containing data to write
     * @return the number of bytes written to the channel
     * @throws IOException if an I/O error occurs; the stream will be closed on error
     */
    public synchronized int write(ByteBuffer bb) throws IOException {
        try {
            int ret = ByteBufferUtil.smartWrite(null, outChannel, bb);
            if (protocolHandler != null) protocolHandler.updateUsage();
            return ret;
        } catch (IOException e) {
            IOUtil.close(this);
            throw e;
        }
    }

    /**
     * Closes this output stream and releases associated resources.
     * <p>
     * This method closes the underlying channel, notifies the protocol handler,
     * and returns the output buffer to the cache for reuse. Multiple calls to
     * this method have no effect after the first call.
     * </p>
     *
     * @throws IOException if an I/O error occurs during closing
     */
    public void close() throws IOException {

        if (!isClosed.getAndSet(true)) {
            if (log.isEnabled()) log.getLogger().info("Calling close");
            IOUtil.close(outChannel, protocolHandler);
            ByteBufferUtil.cache(outAppData);
        }
    }

}
