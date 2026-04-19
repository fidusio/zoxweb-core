package org.zoxweb.server.net.ssl;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.common.ConnectionCallback;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.UsageTracker;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

/**
 * Utility for driving the {@link javax.net.ssl.SSLEngine} state machine over non-blocking channels.
 * <p>
 * Each public handler corresponds to one handshake status returned by
 * {@link javax.net.ssl.SSLEngine#getHandshakeStatus()}. The caller dispatches
 * based on current status; the handler performs one step (handshake wrap/unwrap,
 * delegated tasks, or post-handshake app-data I/O) and re-publishes the resulting
 * status through {@link SSLSessionConfig#sslConnectionHelper} so the next step
 * can be scheduled.
 * </p>
 * <p>
 * <b>Threading model.</b> Within a single SSL session, handshake steps must run
 * serially on one worker thread at a time. {@code SSLEngine}'s handshake state
 * transitions are not safe under concurrent {@code wrap}/{@code unwrap}/
 * {@code getDelegatedTask}. The selector thread never blocks — it dispatches
 * ready events to a worker pool; a session's handshake may hop between workers
 * but never overlaps. Once {@link javax.net.ssl.SSLEngineResult.HandshakeStatus#NOT_HANDSHAKING}
 * is reached, app-data paths may fan out across threads.
 * </p>
 *
 * @see SSLSessionConfig
 * @see javax.net.ssl.SSLEngine
 */
public final class SSLUtil {
    public static final LogWrapper log = new LogWrapper(SSLUtil.class).setEnabled(false);

    private SSLUtil() {
    }

    /**
     * Handler for {@link javax.net.ssl.SSLEngineResult.HandshakeStatus#NOT_HANDSHAKING}:
     * read ciphertext from the channel and decrypt app-data records.
     * <p>
     * Reads into {@link SSLSessionConfig#inSSLNetData}, then loops calling
     * {@link SSLSessionConfig#smartUnwrap} until either the net buffer is fully
     * drained or a {@code BUFFER_UNDERFLOW} signals more wire bytes are needed.
     * Each decrypted record is delivered to {@code callback}. A read of -1 or
     * {@code CLOSED} unwrap status closes the session.
     * </p>
     *
     * @param config   current SSL session state
     * @param callback receives decrypted application data (or exceptions); may be {@code null}
     * @return elapsed processing time in milliseconds
     */
    public static long _notHandshaking(SSLSessionConfig config, BaseSessionCallback<SSLSessionConfig> callback) {
        long ts = System.currentTimeMillis();
        if (log.isEnabled()) log.getLogger().info("" + config.getHandshakeStatus());

        if (config.sslChannel.isOpen()) {
            if (config.getHandshakeStatus() == NOT_HANDSHAKING) {
                try {
                    int bytesRead = config.sslChannel.read(config.inSSLNetData);
                    if (bytesRead == -1) {
                        if (log.isEnabled())
                            log.getLogger().info("SSLCHANNEL-CLOSED-NOT_HANDSHAKING: " + config.getHandshakeStatus() + " bytesRead: " + bytesRead);
                        config.close();
                    } else {
                        SSLEngineResult result;
                        // even if we have read zero it will trigger BUFFER_UNDERFLOW then we wait for incoming
                        // data
                        do {
                            result = config.smartUnwrap(config.inSSLNetData, config.inAppData);
                            if (log.isEnabled())
                                log.getLogger().info("AFTER-NOT_HANDSHAKING-PROCESSING: " + result + " bytesRead: " + bytesRead + " callback: " + callback);
                            switch (result.getStatus()) {
                                case BUFFER_UNDERFLOW:
                                    // no incoming data available we need to wait for more socket data
                                    // return and let the NIOSocket or the data handler call back
                                    if (log.isEnabled())
                                        log.getLogger().info("AFTER-NOT_HANDSHAKING-PROCESSING: " + result + " bytesRead: " + bytesRead + " callback: " + callback);

                                    return System.currentTimeMillis() - ts;

                                case BUFFER_OVERFLOW:
                                    throw new IllegalStateException("NOT_HANDSHAKING should never be " + result.getStatus());
                                    // this should never happen
                                case OK:
                                    // check if we have data to process
                                    if (callback != null && bytesRead >= 0 && result.bytesProduced() > 0) {
                                        // we have decrypted data to process
                                        //config.inSSLNetData.flip();
                                        callback.accept(config.inAppData);
                                    }
                                    break;
                                case CLOSED:
                                    // closed result here
                                    if (log.isEnabled())
                                        log.getLogger().info("CLOSED-DURING-NOT_HANDSHAKING: " + result + " bytesRead: " + bytesRead);
                                    config.close();
                                    break;
                            }
                        }// check if we still have encrypted data to process
                        while (config.inSSLNetData.hasRemaining() && !config.isClosed());


                    }
                } catch (Exception e) {
                    if (log.isEnabled())
                        e.printStackTrace();

                    if (callback != null)
                        callback.exception(e);

                    config.close();
                }
            } else
                config.sslConnectionHelper.publish(config.getHandshakeStatus(), callback);

        }
        return System.currentTimeMillis() - ts;
    }


    /**
     * Handler for {@link javax.net.ssl.SSLEngineResult.HandshakeStatus#FINISHED}:
     * post-handshake completion hook.
     * <p>
     * Creates the upstream connection when this session is the server side of an
     * SSL tunnel, signals successful handshake to client-mode callbacks, and
     * re-publishes the current status if {@code inSSLNetData} still holds
     * buffered bytes — TLS 1.3 in particular may interleave application data
     * in the same flight as the handshake finish.
     * </p>
     *
     * @param config   current SSL session state
     * @param callback notified via {@link ConnectionCallback#sslHandshakeSuccessful()} in client mode; may be {@code null}
     * @return elapsed processing time in milliseconds
     */
    public static long _finished(SSLSessionConfig config, BaseSessionCallback<SSLSessionConfig> callback) {
        long ts = System.currentTimeMillis();

        // ********************************************
        // Very crucial steps
        // ********************************************
        if (config.remoteConnection != null) {
            // we have SSL tunnel
            config.sslConnectionHelper.createRemoteConnection();
        }

        if (config.isClientMode() && callback instanceof ConnectionCallback) {
            /*
             * special case if the connection is a client connection
             */
            ((ConnectionCallback) callback).sslHandshakeSuccessful();
        }

        if (config.inSSLNetData.position() > 0) {
            //**************************************************
            // ||-----DATA BUFFER------ ||
            // ||Handshake data|App data||
            // ||-----------------------||
            // The buffer has app data that needs to be decrypted
            //**************************************************
            config.sslConnectionHelper.publish(config.getHandshakeStatus(), callback);
        }

        return System.currentTimeMillis() - ts;
    }

    /**
     * Handler for {@link javax.net.ssl.SSLEngineResult.HandshakeStatus#NEED_TASK}:
     * run all pending delegated tasks, then re-publish the updated status.
     * <p>
     * Tasks are drained from {@link SSLSessionConfig#getDelegatedTask()} and
     * executed synchronously on the calling worker thread. Blocking operations
     * inside a task (certificate chain validation, OCSP/CRL lookup, HSM
     * signature) only stall this worker — the selector continues dispatching
     * events to other workers unaffected. Session handshake remains serialized
     * on one worker at a time by design.
     * </p>
     *
     * @param config   current SSL session state
     * @param callback passed through to the next state handler
     * @return elapsed processing time in milliseconds
     */
    public static long _needTask(SSLSessionConfig config, BaseSessionCallback<SSLSessionConfig> callback) {
        long ts = System.currentTimeMillis();

        Runnable toRun;
        while ((toRun = config.getDelegatedTask()) != null)
            toRun.run();

        SSLEngineResult.HandshakeStatus status = config.getHandshakeStatus();

        ts = System.currentTimeMillis() - ts;

        if (log.isEnabled()) log.getLogger().info("After run: " + status);

        config.sslConnectionHelper.publish(status, callback);

        return ts;
    }


    /**
     * Handler for {@link javax.net.ssl.SSLEngineResult.HandshakeStatus#NEED_UNWRAP}
     * (and Java 9+ {@code NEED_UNWRAP_AGAIN}): read ciphertext and unwrap into
     * the engine during handshake.
     * <p>
     * The destination is {@link ByteBufferUtil#EMPTY} because no application
     * data is produced during handshake. {@code BUFFER_UNDERFLOW} simply returns
     * — the selector will re-dispatch when more wire bytes arrive.
     * {@code BUFFER_OVERFLOW} is not expected here and is treated as a fatal
     * invariant violation.
     * </p>
     *
     * @param config   current SSL session state
     * @param callback passed through to the next state handler
     * @return elapsed processing time in milliseconds
     */
    public static long _needUnwrap(SSLSessionConfig config, BaseSessionCallback<SSLSessionConfig> callback) {

        long ts = System.currentTimeMillis();
        if (log.isEnabled()) log.getLogger().info("Entry: " + config.getHandshakeStatus());

        if (config.getHandshakeStatus() == NEED_UNWRAP || SUS.enumName(config.getHandshakeStatus()).equals("NEED_UNWRAP_AGAIN")) {
            try {

                int bytesRead = config.sslChannel.read(config.inSSLNetData);
                if (bytesRead == -1) {
                    if (log.isEnabled())
                        log.getLogger().info("SSLCHANNEL-CLOSED-NEED_UNWRAP: " + config.getHandshakeStatus() + " bytes read: " + bytesRead);
                    config.close();
                } else {
                    // bytesRead 0 or more
                    // even if we have read zero it will trigger BUFFER_UNDERFLOW then we wait for incoming
                    // data
                    if (log.isEnabled())
                        log.getLogger().info("BEFORE-UNWRAP: " + config.inSSLNetData + " bytes read " + bytesRead);
                    SSLEngineResult result = config.smartUnwrap(config.inSSLNetData, ByteBufferUtil.EMPTY);


                    if (log.isEnabled()) {
                        log.getLogger().info("AFTER-NEED_UNWRAP-HANDSHAKING: " + result + " bytes read: " + bytesRead);
                        log.getLogger().info("AFTER-NEED_UNWRAP-HANDSHAKING inNetData: " + config.inSSLNetData + " inAppData: " + config.inAppData);
                    }

                    switch (result.getStatus()) {
                        case BUFFER_UNDERFLOW:
                            // no incoming data available we need to wait for more socket data
                            // return and let the NIOSocket or the data handler call back
                            // config.sslChannelSelectableStatus.set(true);
                            // config.sslRead.set(true);
                            return System.currentTimeMillis() - ts;
                        case BUFFER_OVERFLOW:
                            throw new IllegalStateException("NEED_UNWRAP should never happen: " + result.getStatus());
                            // this should never happen
                        case OK:
                            config.sslConnectionHelper.publish(result.getHandshakeStatus(), callback);
                            break;
                        case CLOSED:
                            // check result here
                            if (log.isEnabled())
                                log.getLogger().info("CLOSED-DURING-NEED_UNWRAP: " + result + " bytes read: " + bytesRead);
                            config.close();
                            break;
                    }
                }
            } catch (Exception e) {
                if (log.isEnabled())
                    e.printStackTrace();
                config.close();
            }
        }
        return System.currentTimeMillis() - ts;
    }


    /**
     * Handler for {@link javax.net.ssl.SSLEngineResult.HandshakeStatus#NEED_WRAP}:
     * generate outbound handshake bytes and send them on the channel.
     * <p>
     * The source is {@link ByteBufferUtil#EMPTY} because no application data is
     * consumed during handshake. On {@code OK}, the produced ciphertext in
     * {@code outSSLNetData} is drained to the channel via
     * {@link ByteBufferUtil#smartWrite} (which flips before draining and
     * compacts after). {@code BUFFER_UNDERFLOW}/{@code OVERFLOW} are treated as
     * fatal invariant violations; {@code forcedClose} is set and an exception
     * is raised.
     * </p>
     *
     * @param config   current SSL session state
     * @param callback passed through to the next state handler
     * @return elapsed processing time in milliseconds
     */
    public static long _needWrap(SSLSessionConfig config, BaseSessionCallback<SSLSessionConfig> callback) {
        long ts = System.currentTimeMillis();

        if (config.getHandshakeStatus() == NEED_WRAP) {
            try {
                SSLEngineResult result = config.smartWrap(ByteBufferUtil.EMPTY, config.outSSLNetData, true);
                // at handshake stage, data in appOut won't be
                // processed hence dummy buffer
                if (log.isEnabled())
                    log.getLogger().info("AFTER-NEED_WRAP-HANDSHAKING: " + result);

                switch (result.getStatus()) {
                    case BUFFER_UNDERFLOW:
                    case BUFFER_OVERFLOW:
                        config.forcedClose = true;
                        throw new IllegalStateException(result + " invalid state context " + config.outSSLNetData + " " + config.sslChannel.getRemoteAddress());
                    case OK:
                        int written = ByteBufferUtil.smartWrite(null, config.sslChannel, config.outSSLNetData, true);
                        if (log.isEnabled())
                            log.getLogger().info(result.getHandshakeStatus() + " After writing data HANDSHAKING-NEED_WRAP: " + config.outSSLNetData + " written:" + written);
                        config.sslConnectionHelper.publish(result.getHandshakeStatus(), callback);
                        break;
                    case CLOSED:
                        config.close();
                        break;
                }
            } catch (Exception e) {
                if (log.isEnabled())
                    e.printStackTrace();

                config.close();
            }
        }
        return System.currentTimeMillis() - ts;
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
     * @param sslConfig    session state providing the engine and net buffer
     * @param dataChannel  channel to receive ciphertext
     * @param bb           plaintext source
     * @param usageTracker activity notifier; may be {@code null}
     * @param closeable    closed via {@link SharedIOUtil#close} on channel I/O error
     * @param flip         {@code true} if {@code bb} is in write-mode; {@code false} if read-mode
     * @return ciphertext bytes written to the channel, or -1 on channel EOF
     * @throws SSLException if the session is still handshaking
     * @throws IOException  on {@code BUFFER_*}, {@code CLOSED}, or channel error
     */
    private static int _sslWrite(SSLSessionConfig sslConfig, ByteChannel dataChannel, ByteBuffer bb, UsageTracker usageTracker, AutoCloseable closeable, boolean flip) throws IOException {
        int written = -1;
        if (sslConfig.getHandshakeStatus() == NOT_HANDSHAKING) {



            SSLEngineResult result = sslConfig.smartWrap(bb, sslConfig.outSSLNetData, flip); // at handshake stage, data in appOut won't be
            if (log.isEnabled())
                log.getLogger().info("AFTER-NEED_WRAP-PROCESSING: " + result);
            switch (result.getStatus()) {
                case BUFFER_UNDERFLOW:
                case BUFFER_OVERFLOW:
                    throw new IOException(result.getStatus() + " invalid state context buffer size " +
                            SUS.toCanonicalID(',', sslConfig.outSSLNetData.capacity(), sslConfig.outSSLNetData.limit(), sslConfig.outSSLNetData.position()));
                case OK:
                    try {
                        written = ByteBufferUtil.smartWrite(null, dataChannel, sslConfig.outSSLNetData, true);
                        if(usageTracker != null) usageTracker.updateUsage();
                    } catch (IOException e) {
                        SharedIOUtil.close(closeable);
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
     * Inside the chunking loop each {@link ByteBuffer#slice() slice} is handed
     * to {@link #_sslWrite} with {@code flip=false} — slices are born read-mode
     * (pos=0, lim=n), so no additional flip round-trip is needed.
     * </p>
     * <p>
     * All data is drained on success. On channel error or EOF mid-stream the
     * undrained remainder is compacted to the start of {@code src} — the
     * buffer ends in write-mode regardless of the input mode. Callers that
     * handed in a throwaway read-mode buffer via {@code wrap()} should discard
     * it; callers that expect their write-mode buffer to be empty-and-ready
     * for the next {@code put()} can reuse it directly.
     * </p>
     *
     * @param sslConfig    session state
     * @param dataChannel  channel to receive ciphertext
     * @param src          plaintext to encrypt
     * @param usageTracker activity notifier; may be {@code null}
     * @param closeable    closed via {@link SharedIOUtil#close} on channel I/O error
     * @param flip         {@code true} if {@code src} is in write-mode; {@code false} if read-mode
     * @return total ciphertext bytes written to the channel, or -1 if EOF occurred before any bytes were sent
     * @throws SSLException if the session is still handshaking
     * @throws IOException  on channel error
     */
    public static int sslChunkedWrite(SSLSessionConfig sslConfig, ByteChannel dataChannel, ByteBuffer src, UsageTracker usageTracker, AutoCloseable closeable, boolean flip) throws IOException {
        // dataSize semantics depend on caller's buffer mode:
        //   flip=true  → src is write-mode, data is [0..position), size = position()
        //   flip=false → src is read-mode,  data is [position..limit), size = remaining()
        int dataSize = flip ? src.position() : src.remaining();
        if (dataSize < Math.min(sslConfig.getApplicationBufferSize(), SharedIOUtil.K_8)) {
            return _sslWrite(sslConfig, dataChannel, src, usageTracker, closeable, flip);
        }

        // Ensure src is in read-mode for the chunking loop regardless of caller convention.
        if (flip)
            src.flip();                            // write-mode → read-mode: [0..dataEnd)
        int savedLimit = src.limit();
        int total = 0, written = 0;
        try {
            while (src.hasRemaining()) {
                int n = Math.min(SharedIOUtil.K_8, src.remaining());
                src.limit(src.position() + n);
                ByteBuffer view = src.slice();   // already read-mode: pos=0, lim=n
                src.limit(savedLimit);
                written = _sslWrite(sslConfig, dataChannel, view, usageTracker, closeable, false);
                if (written < 0) break;
                total += written;
                src.position(src.position() + n);
            }
        } finally {
            src.compact();                     // back to write-mode for next caller put()
        }
        return written < 0 && total == 0 ? -1 : total;

    }
}
