package org.zoxweb.server.net.common;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.net.BaseChannelOutputStream;
import org.zoxweb.server.net.ProtocolHandler;
import org.zoxweb.server.net.ssl.SSLSessionConfig;
import org.zoxweb.shared.util.SharedUtil;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;

public class CommonChannelOutputStream
        extends BaseChannelOutputStream {


    private transient SSLSessionConfig sslConfig;
    private final AtomicBoolean sslMode = new AtomicBoolean(false);

    public CommonChannelOutputStream(ProtocolHandler protocolHandler, ByteChannel byteChannel, int outAppBufferSize) throws IOException {
        super(protocolHandler, byteChannel, outAppBufferSize);
    }


    public synchronized CommonChannelOutputStream setSSLSessionConfig(SSLSessionConfig sslConfig) {
        this.sslConfig = sslConfig;
        return this;
    }

    public synchronized CommonChannelOutputStream setSSLMode(boolean sslMode) {
        if (sslMode && sslConfig == null) {
            throw new IllegalArgumentException("SSLSessionConfig cannot be null, set it first");
        }
        this.sslMode.set(sslMode);
        return this;
    }

    public boolean isSSLMode() {
        return sslMode.get();
    }

    public SSLSessionConfig getSSLSessionConfig() {
        return sslConfig;
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
    @Override
    public synchronized int write(ByteBuffer byteBuffer) throws IOException {
        return sslMode.get() ? sslWrite(byteBuffer) : plainWrite(byteBuffer);
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
    private synchronized int plainWrite(ByteBuffer bb) throws IOException {
        try {
            int ret = ByteBufferUtil.smartWrite(null, dataChannel, bb);
            if (protocolHandler != null) protocolHandler.updateUsage();
            return ret;
        } catch (IOException e) {
            IOUtil.close(this);
            throw e;
        }
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
    private synchronized int sslWrite(ByteBuffer bb) throws IOException {
        int written = -1;
        if (sslConfig == null) {
            throw new SSLException("SSL engine not configured");
        }

        if (sslConfig.getHandshakeStatus() == NOT_HANDSHAKING) {
            SSLEngineResult result = sslConfig.smartWrap(bb, sslConfig.outSSLNetData); // at handshake stage, data in appOut won't be
            if (log.isEnabled())
                log.getLogger().info("AFTER-NEED_WRAP-PROCESSING: " + result);
            switch (result.getStatus()) {
                case BUFFER_UNDERFLOW:
                case BUFFER_OVERFLOW:
                    throw new IOException(result.getStatus() + " invalid state context buffer size " +
                            SharedUtil.toCanonicalID(',', sslConfig.outSSLNetData.capacity(), sslConfig.outSSLNetData.limit(), sslConfig.outSSLNetData.position()));
                case OK:
                    try {
                        written = ByteBufferUtil.smartWrite(null, dataChannel, sslConfig.outSSLNetData);
                        if (protocolHandler != null) protocolHandler.updateUsage();
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
            if (sslConfig != null)
                IOUtil.close(sslConfig, protocolHandler);
            else
                IOUtil.close(dataChannel, protocolHandler);
            ByteBufferUtil.cache(outAppData);
        }
    }


//    /**
//     * Closes this output stream and releases associated resources.
//     * <p>
//     * This method closes the underlying channel, notifies the protocol handler,
//     * and returns the output buffer to the cache for reuse. Multiple calls to
//     * this method have no effect after the first call.
//     * </p>
//     *
//     * @throws IOException if an I/O error occurs during closing
//     */
//    public void close() throws IOException {
//
//        if (!isClosed.getAndSet(true)) {
//            if (log.isEnabled()) log.getLogger().info("Calling close");
//            IOUtil.close(dataChannel, protocolHandler);
//            ByteBufferUtil.cache(outAppData);
//        }
//    }

}
