package org.zoxweb.server.net.common;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.net.BaseChannelOutputStream;
import org.zoxweb.server.net.ProtocolHandler;
import org.zoxweb.server.net.ssl.SSLConfigInt;
import org.zoxweb.server.net.ssl.SSLUtil;
import org.zoxweb.shared.io.SharedIOUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;


public class CommonChannelOutputStream
        extends BaseChannelOutputStream {


    private volatile SSLConfigInt sslConfig;
    private final AtomicBoolean sslMode = new AtomicBoolean(false);


    public CommonChannelOutputStream(ByteChannel byteChannel) throws IOException {
        super(null, byteChannel);
    }


    public CommonChannelOutputStream(ProtocolHandler protocolHandler, ByteChannel byteChannel) throws IOException {
        super(protocolHandler, byteChannel);
    }


    public synchronized CommonChannelOutputStream setSSLConfigInt(SSLConfigInt sslConfig) {
        this.sslConfig = sslConfig;
        this.sslMode.set(sslConfig != null);
        return this;
    }

    public boolean isSSLMode() {
        return sslMode.get();
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
            int ret = ByteBufferUtil.smartWrite(null, dataChannel, bb, flip);
            if (usageTracker != null) usageTracker.updateUsage();
            return ret;
        } catch (IOException e) {
            SharedIOUtil.close(this);
            throw e;
        }
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
                SharedIOUtil.close(sslConfig, usageTracker);
            else
                SharedIOUtil.close(dataChannel, usageTracker);
        }
    }

}
