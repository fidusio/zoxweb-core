package org.zoxweb.server.net.common;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.net.BaseChannelOutputStream;
import org.zoxweb.server.net.ProtocolHandler;
import org.zoxweb.server.net.ssl.SSLSessionConfig;
import org.zoxweb.server.net.ssl.SSLUtil;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SUS;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;


public class CommonChannelOutputStream
        extends BaseChannelOutputStream {


    private transient SSLSessionConfig sslConfig;
    private final AtomicBoolean sslMode = new AtomicBoolean(false);


    public CommonChannelOutputStream(ByteChannel byteChannel) throws IOException {
        super(null, byteChannel);
    }


    public CommonChannelOutputStream(ProtocolHandler protocolHandler, ByteChannel byteChannel) throws IOException {
        super(protocolHandler, byteChannel);
    }


    public synchronized CommonChannelOutputStream setSSLSessionConfig(SSLSessionConfig sslConfig) {
        SUS.checkIfNull("sslConfig", sslConfig);
        this.sslConfig = sslConfig;
        this.sslMode.set(true);
        return this;
    }

    public boolean isSSLMode() {
        return sslMode.get();
    }

    public SSLSessionConfig getSSLSessionConfig() {
        return sslConfig;
    }


    /**
     * Sends the contents of a {@link ByteBuffer} to the underlying channel,
     * choosing the plaintext or SSL/TLS path based on {@link #isSSLMode()}.
     * <p>
     * In SSL mode the payload is encrypted and chunked via
     * {@link SSLUtil#sslChunkedWrite}; in plaintext mode it is drained directly
     * through {@link #plainWrite}. The {@code flip} flag is forwarded in both
     * cases to describe the caller's buffer mode.
     * </p>
     *
     * @param byteBuffer payload to transmit
     * @param flip       {@code true} if {@code byteBuffer} is in write-mode (needs flipping);
     *                   {@code false} if already in read-mode (e.g. from {@link ByteBuffer#wrap})
     * @return number of bytes transmitted to the channel, or -1 on EOF
     * @throws IOException if an I/O or SSL error occurs
     */
    @Override
    public synchronized int write(ByteBuffer byteBuffer, boolean flip) throws IOException {
        return isSSLMode() ?  SSLUtil.sslChunkedWrite(sslConfig, dataChannel, byteBuffer, usageTracker, this, flip) : plainWrite(byteBuffer, flip);
    }


    /**
     * Drains a plaintext {@link ByteBuffer} to the underlying channel via
     * {@link ByteBufferUtil#smartWrite}.
     * <p>
     * {@code smartWrite} will flip {@code bb} iff {@code flip=true}, then drain
     * and compact it. If {@code flip=false} the buffer is assumed to be in
     * read-mode already and is drained as-is.
     * </p>
     * <p>
     * On I/O error the stream is closed before rethrowing.
     * </p>
     *
     * @param bb   payload
     * @param flip {@code true} if {@code bb} is in write-mode, {@code false} if already read-mode
     * @return bytes written to the channel
     * @throws IOException on channel error; the stream is closed before the exception propagates
     */
    private synchronized int plainWrite(ByteBuffer bb, boolean flip) throws IOException {
        try {
            int ret = ByteBufferUtil.smartWrite(null, dataChannel, bb, flip );
            if (usageTracker != null) usageTracker.updateUsage();
            return ret;
        } catch (IOException e) {
            SharedIOUtil.close(this);
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
//    private synchronized int sslWrite(ByteBuffer bb) throws IOException {
//
//        int written = -1;
//        if (sslConfig == null) {
//            throw new SSLException("SSL engine not configured");
//        }
//
//        if (sslConfig.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
//            SSLEngineResult result = sslConfig.smartWrap(bb, sslConfig.outSSLNetData); // at handshake stage, data in appOut won't be
//            if (log.isEnabled())
//                log.getLogger().info("AFTER-NEED_WRAP-PROCESSING: " + result);
//            switch (result.getStatus()) {
//                case BUFFER_UNDERFLOW:
//                case BUFFER_OVERFLOW:
//                    throw new IOException(result.getStatus() + " invalid state context buffer size " +
//                            SUS.toCanonicalID(',', sslConfig.outSSLNetData.capacity(), sslConfig.outSSLNetData.limit(), sslConfig.outSSLNetData.position()));
//                case OK:
//                    try {
//                        written = ByteBufferUtil.smartWrite(null, dataChannel, sslConfig.outSSLNetData, true);
//                        if (usageTracker != null) usageTracker.updateUsage();
//                    } catch (IOException e) {
//                        SharedIOUtil.close(this);
//                        throw e;
//                    }
//                    break;
//                case CLOSED:
//                    throw new IOException("Closed");
//            }
//        } else {
//            throw new SSLException("handshaking state can't send data yet");
//        }
//
//        return written;
//    }

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
                SharedIOUtil.close(sslConfig, usageTracker);
            else
                SharedIOUtil.close(dataChannel, usageTracker);
            //ByteBufferUtil.cache(outAppData);
        }
    }

}
