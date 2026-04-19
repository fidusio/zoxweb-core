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
        super(null, byteChannel, true);
    }


    public CommonChannelOutputStream(ProtocolHandler protocolHandler, ByteChannel byteChannel) throws IOException {
        super(protocolHandler, byteChannel, true);
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
//        return isSSLMode() ?  sslChunkedWrite(byteBuffer, flip) : plainWrite(byteBuffer, flip);
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
            ByteBufferUtil.cache(outAppData);
        }
    }

    /**
     * Single-record SSL write: encrypt {@code bb} into {@code outSSLNetData} and
     * drain the resulting ciphertext to {@code dataChannel}.
     * <p>
     * <b>Buffer-mode contract.</b> The {@code flip} flag describes the caller's
     * buffer mode for {@code bb}:
     * </p>
     * <ul>
     *     <li>{@code flip=true} — {@code bb} is in write-mode
     *         (position = end of plaintext, limit = capacity);
     *         {@link SSLSessionConfig#smartWrap} will flip it before wrap.</li>
     *     <li>{@code flip=false} — {@code bb} is already in read-mode
     *         (position = start of plaintext, limit = end); no flip performed.</li>
     * </ul>
     * <p>
     * After wrap, {@link SSLSessionConfig#smartWrap} compacts {@code bb}.
     * The destination {@code outSSLNetData} is always in write-mode after wrap,
     * so the subsequent {@link ByteBufferUtil#smartWrite} is invoked with
     * {@code flip=true} unconditionally.
     * </p>
     * <p>
     * The session must be past handshake ({@code NOT_HANDSHAKING}); otherwise
     * an {@link SSLException} is thrown. {@code BUFFER_UNDERFLOW}/{@code OVERFLOW}
     * and {@code CLOSED} are translated to {@link IOException}.
     * </p>
     *
     * @param bb           plaintext source
     * @param flip         {@code true} if {@code bb} is in write-mode; {@code false} if read-mode
     * @return ciphertext bytes written to the channel, or -1 on channel EOF
     * @throws SSLException if the session is still handshaking
     * @throws IOException  on {@code BUFFER_*}, {@code CLOSED}, or channel error
     */
//    private  int _sslWrite(ByteBuffer bb, boolean flip) throws IOException {
//        int written = -1;
//        if (sslConfig.getHandshakeStatus() == NOT_HANDSHAKING) {
//
//
//
//            SSLEngineResult result = sslConfig.smartWrap(bb, sslConfig.outSSLNetData, flip); // at handshake stage, data in appOut won't be
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
//                        if(usageTracker != null) usageTracker.updateUsage();
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
     * Encrypt {@code src} and send it to {@code dataChannel}, chunking the
     * payload into TLS-record-sized pieces when it exceeds what a single
     * {@link javax.net.ssl.SSLEngine#wrap} call can consume.
     * <p>
     * <b>Dispatch.</b>
     * </p>
     * <ul>
     *     <li>Payloads with data size {@code < min(applicationBufferSize, K_8)}
     *         delegate to a single {@link #_sslWrite}.</li>
     *     <li>Larger payloads are sliced into {@link SharedIOUtil#K_8} (8&nbsp;KB)
     *         chunks; each chunk is encrypted and transmitted as one TLS record.
     *         The chunk size is deliberately below
     *         {@code SSLSession.getApplicationBufferSize()} so every {@code wrap()}
     *         fully consumes its chunk — no under-consumption and no
     *         {@code BUFFER_OVERFLOW} on the output side.</li>
     * </ul>
     * <p>
     * <b>Buffer-mode contract.</b> The {@code flip} flag describes the caller's
     * buffer mode for {@code src}:
     * </p>
     * <ul>
     *     <li>{@code flip=true} — {@code src} is in write-mode; data size is
     *         {@code src.position()}. The chunking branch flips {@code src} once
     *         to enter read-mode; the short-circuit branch passes the flag to
     *         {@link #_sslWrite}.</li>
     *     <li>{@code flip=false} — {@code src} is already read-mode (e.g. from
     *         {@link ByteBuffer#wrap}); data size is {@code src.remaining()}.
     *         No extra flip is performed.</li>
     * </ul>
     * <p>
     * All data is drained on success. On channel error or EOF mid-stream the
     * undrained remainder is compacted to the start of {@code src} — the
     * buffer ends in write-mode regardless of the input mode. Callers that
     * handed in a throwaway read-mode buffer via {@code wrap()} should discard
     * it; callers that expect their write-mode buffer to be empty-and-ready
     * for the next {@code put()} can reuse it directly.
     * </p>
     *
     * @param src          plaintext to encrypt
     * @param flip         {@code true} if {@code src} is in write-mode; {@code false} if read-mode
     * @return total ciphertext bytes written to the channel, or -1 if EOF occurred before any bytes were sent
     * @throws SSLException if the session is still handshaking
     * @throws IOException  on channel error
     */
//    private  int sslChunkedWrite(ByteBuffer src, boolean flip) throws IOException {
//        // dataSize semantics depend on caller's buffer mode:
//        //   flip=true  → src is write-mode, data is [0..position), size = position()
//        //   flip=false → src is read-mode,  data is [position..limit), size = remaining()
//        int dataSize = flip ? src.position() : src.remaining();
//        if (dataSize < Math.min(sslConfig.getApplicationBufferSize(), SharedIOUtil.K_8)) {
//            return _sslWrite(src,flip);
//        }
//
//        // Ensure src is in read-mode for the chunking loop regardless of caller convention.
//        if (flip)
//            src.flip();                            // write-mode → read-mode: [0..dataEnd)
//        int savedLimit = src.limit();
//        int total = 0, written = 0;
//        try {
//            while (src.hasRemaining()) {
//                int n = Math.min(SharedIOUtil.K_8, src.remaining());
//                src.limit(src.position() + n);
//                ByteBuffer view = src.slice();
//                view.position(n);              // write-mode for smartWrap's flip
//                src.limit(savedLimit);
//                written = _sslWrite(view, true);
//                if (written < 0) break;
//                total += written;
//                src.position(src.position() + n);
//            }
//        } finally {
//            src.compact();                     // back to write-mode for next caller put()
//        }
//        return written < 0 && total == 0 ? -1 : total;
//
//    }

}
