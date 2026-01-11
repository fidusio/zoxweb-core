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
package org.zoxweb.server.net.ssl;


import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.net.BaseChannelOutputStream;
import org.zoxweb.server.net.ProtocolHandler;
import org.zoxweb.shared.util.SharedUtil;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;

/**
 * An SSL/TLS-encrypted channel output stream for secure data transmission.
 * <p>
 * This class extends {@link BaseChannelOutputStream} to provide SSL/TLS encryption
 * for outgoing data using the Java {@link javax.net.ssl.SSLEngine}. All data written
 * to this stream is encrypted before being sent over the underlying channel.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 *     <li>Automatic encryption of all outgoing data via SSLEngine</li>
 *     <li>Handles SSL handshake state verification before writing</li>
 *     <li>Supports all SSL/TLS protocols configured in the SSLEngine</li>
 *     <li>Thread-safe write operations</li>
 * </ul>
 * <p>
 * <b>Note:</b> Data can only be written after the SSL handshake is complete.
 * Attempting to write during handshaking will result in an {@link SSLException}.
 * </p>
 *
 * @see BaseChannelOutputStream
 * @see org.zoxweb.server.net.ChannelOutputStream
 * @see SSLSessionConfig
 * @see javax.net.ssl.SSLEngine
 */
public class SSLChannelOutputStream extends BaseChannelOutputStream {

    /** Configuration containing the SSLEngine and related SSL session state */
    private final SSLSessionConfig config;

    /**
     * Constructs a new SSLChannelOutputStream with the specified parameters.
     *
     * @param protocolHandler the protocol handler for connection management, may be null
     * @param config the SSL session configuration containing the SSLEngine and channel
     * @param outAppBufferSize the size of the application output buffer in bytes
     * @throws IOException if unable to get the remote address from the channel
     * @throws NullPointerException if config or its channel is null
     */
    public SSLChannelOutputStream(ProtocolHandler protocolHandler, SSLSessionConfig config, int outAppBufferSize) throws IOException {
        super(protocolHandler, config.sslChannel, outAppBufferSize);
        this.config = config;
    }


    /**
     * Encrypts and writes the contents of a {@link ByteBuffer} to the underlying SSL channel.
     * <p>
     * This method wraps (encrypts) the plaintext data using the configured {@link javax.net.ssl.SSLEngine}
     * and then writes the encrypted data to the channel. The method verifies that the SSL
     * handshake is complete before attempting to write.
     * </p>
     *
     * @param bb the buffer containing plaintext data to encrypt and write
     * @return the number of encrypted bytes written to the channel
     * @throws IOException if an I/O error occurs during encryption or writing
     * @throws SSLException if the SSL handshake is not complete and data cannot be sent
     */
    public synchronized int write(ByteBuffer bb) throws IOException {
        int written = -1;
        if (config.getHandshakeStatus() == NOT_HANDSHAKING) {
            SSLEngineResult result = config.smartWrap(bb, config.outSSLNetData); // at handshake stage, data in appOut won't be
            if (log.isEnabled())
                log.getLogger().info("AFTER-NEED_WRAP-PROCESSING: " + result);
            switch (result.getStatus()) {
                case BUFFER_UNDERFLOW:
                case BUFFER_OVERFLOW:
                    throw new IOException(result.getStatus() + " invalid state context buffer size " +
                            SharedUtil.toCanonicalID(',', config.outSSLNetData.capacity(), config.outSSLNetData.limit(), config.outSSLNetData.position()));
                case OK:
                    try {
                        written = ByteBufferUtil.smartWrite(null, outChannel, config.outSSLNetData);
                        if(protocolHandler != null) protocolHandler.updateUsage();
                    } catch (IOException e) {
                        IOUtil.close(this);
                        throw e;
                    }
                    break;
                case CLOSED:
                    throw new IOException("Closed");
            }
        } else {
            throw new SSLException("handshaking state can't send data yet");
        }

        return written;
    }

    /**
     * Closes this SSL output stream and releases associated resources.
     * <p>
     * This method closes the SSL session configuration (which handles SSL shutdown),
     * notifies the protocol handler, and returns the output buffer to the cache for reuse.
     * Multiple calls to this method have no effect after the first call.
     * </p>
     *
     * @throws IOException if an I/O error occurs during closing
     */
    public void close() throws IOException {
        if (!isClosed.getAndSet(true)) {
            if (log.isEnabled()) log.getLogger().info("Calling close");
            IOUtil.close(config, protocolHandler);
            ByteBufferUtil.cache(outAppData);
        }
    }


}
