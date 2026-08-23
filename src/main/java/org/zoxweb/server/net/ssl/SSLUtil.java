package org.zoxweb.server.net.ssl;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.UsageTracker;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.Buffer;
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
 * status through {@link SSLConfigInt#getSSLConnectionHelper()} so the next step
 * can be scheduled.
 * </p>
 *
 * <h2>Load-bearing invariants</h2>
 * The code below looks like it would race or leak under casual reading. It does
 * not, because three architectural invariants hold across the entire SSL/NIO
 * stack. Anyone editing this class without internalizing all three will introduce
 * real bugs.
 *
 * <h3>1. Key-interest gating &rArr; single-thread-per-session per dispatch</h3>
 * The selector thread never blocks and never calls
 * {@code wrap}/{@code unwrap}/{@code getDelegatedTask} itself. When a channel
 * becomes readable, the selector <b>sets the key's interest set to {@code 0}</b>
 * before dispatching to a worker. The key is re-armed to {@code OP_READ} only
 * after the worker finishes its full cycle and returns to the pool. While a
 * worker holds the session, the channel cannot be dispatched to any other
 * worker — there is no concurrent access to the {@link javax.net.ssl.SSLEngine},
 * the net/app buffers, or {@link SSLConfigInt} state.
 * <p>
 * The assigned worker performs the entire sequence on the same thread:
 * </p>
 * <ol>
 *     <li>read encrypted bytes from the channel</li>
 *     <li>decrypt via {@link SSLUtil#smartSSLUnwrap smartUnwrap}</li>
 *     <li>deliver decrypted app data to the callback</li>
 *     <li>write any response back through the channel (synchronous — see invariant 2)</li>
 *     <li><b>try again</b> — re-read in case more ciphertext arrived during processing
 *         (this is exactly what the {@code do/while} loop in
 *         {@link #_notHandshaking _notHandshaking} is doing — it is not defensive
 *         code, it is the documented "try again before releasing" step)</li>
 *     <li>re-arm {@code OP_READ} and release the thread to the pool</li>
 * </ol>
 * <p>
 * Different sessions process fully in parallel on different workers; a single
 * session is strictly single-thread per dispatch window. The {@code synchronized}
 * keyword on {@link SSLUtil#smartSSLWrap smartWrite}/{@link SSLUtil#smartSSLUnwrap smartUnwrap}
 * is belt-and-suspenders, not the actual serialization mechanism — the key gate is.
 * </p>
 *
 * <h3>2. NIO write is synchronous and complete-or-fail</h3>
 * {@link org.zoxweb.server.io.ByteBufferUtil#smartWrite ByteBufferUtil.smartWrite}
 * loops until every byte queued for the current call is drained to the channel,
 * or it fails. By the time {@link #sslChunkedWrite} returns normally, every byte
 * of {@code src} has been encrypted AND sent on the wire. There is no
 * "partial write left in the net out-buffer" steady state to reason about;
 * callers never need to retry an unsent tail because there is no unsent tail
 * on success.
 *
 * <h3>3. Plaintext IO buffers &lt; ½ SSL net buffers &rArr; {@code BUFFER_OVERFLOW} unreachable</h3>
 * Application IO buffers in this codebase are bounded to 4–8&nbsp;KB in any
 * direction. The SSL net buffers (the {@code IOBuffers} in/out pair)
 * are sized to {@link javax.net.ssl.SSLSession#getPacketBufferSize() packetBufferSize}
 * (≥ ~16&nbsp;KB for TLS), and the inbound plaintext destination —
 * {@link SSLConfigInt#getInDecryptedBuffer() the decryption buffer} — is allocated
 * once by {@link SSLConfigInt#beginHandshake(org.zoxweb.server.io.IOBuffers) beginHandshake} at
 * {@link javax.net.ssl.SSLSession#getApplicationBufferSize() applicationBufferSize},
 * the engine's own ceiling for the plaintext a single {@code unwrap} can produce.
 * With at most 8&nbsp;KB of plaintext going into a ≥ 16&nbsp;KB net destination on
 * {@code wrap}, and every {@code unwrap} decrypting into a destination sized to the
 * engine's maximum, {@code BUFFER_OVERFLOW} cannot occur — in the handshake handlers
 * as well as the data ones, since {@link #_needUnwrap _needUnwrap} and
 * {@link #_notHandshaking _notHandshaking} share that one destination buffer.
 * The {@code IllegalStateException} paths for {@code BUFFER_OVERFLOW}
 * in {@link #_notHandshaking _notHandshaking}, {@link #_needUnwrap _needUnwrap} and
 * {@link #_needWrap _needWrap} are dead-on-arrival guards, not recovery gaps — do not
 * "fix" them by adding drain-and-retry logic without first changing the
 * buffer-sizing contract.
 *
 * @see SSLConfigInt
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
     * Reads ciphertext into the net in-buffer of {@link SSLConfigInt#getSSLIOBuffers()},
     * then loops calling {@link SSLUtil#smartSSLUnwrap} — decrypting into
     * {@link SSLConfigInt#getInDecryptedBuffer()} — until either the net buffer is fully
     * drained or a {@code BUFFER_UNDERFLOW} signals more wire bytes are needed.
     * Each decrypted record is delivered to {@code callback}. A read of -1 or
     * {@code CLOSED} unwrap status closes the session.
     * </p>
     *
     * @param config   current SSL session state
     * @param callback receives decrypted application data (or exceptions); may be {@code null}
     * @return elapsed processing time in milliseconds
     */
    public static long _notHandshaking(SSLConfigInt config, BaseSessionCallback<SSLConfigInt> callback) {
        long ts = System.currentTimeMillis();
        if (log.isEnabled()) log.getLogger().info("" + config.getHandshakeStatus());

        if (config.getChannel().isOpen()) {
            if (config.getHandshakeStatus() == NOT_HANDSHAKING) {
                try {
                    int bytesRead = config.getChannel().read(config.getSSLIOBuffers().getInBuffer());
                    if (bytesRead == -1) {
                        if (log.isEnabled())
                            log.getLogger().info("SSLCHANNEL-CLOSED-NOT_HANDSHAKING: " + config.getHandshakeStatus() + " bytesRead: " + bytesRead);
                        config.close();
                    } else {
                        SSLEngineResult result;
                        // even if we have read zero it will trigger BUFFER_UNDERFLOW then we wait for incoming
                        // data
                        do {
                            result = smartSSLUnwrap(config.getSSLEngine(), config.getSSLIOBuffers().getInBuffer(), config.getInDecryptedBuffer(), true, true);
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
                                        callback.accept(config.getInDecryptedBuffer());
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
                        while (config.getSSLIOBuffers().getInBuffer().hasRemaining() && !config.isClosed());


                    }
                } catch (Exception e) {
                    if (log.isEnabled())
                        e.printStackTrace();

                    if (callback != null)
                        callback.exception(e);

                    SharedIOUtil.close(config);
                }
            } else
                config.getSSLConnectionHelper().publish(config.getHandshakeStatus(), callback);

        }
        return System.currentTimeMillis() - ts;
    }


    /**
     * Handler for {@link javax.net.ssl.SSLEngineResult.HandshakeStatus#FINISHED}:
     * post-handshake completion hook.
     * <p>
     * Routes handshake completion exclusively through the session helper's
     * {@code notifySSLHandshakeFinished()} — unconditional, valid because every
     * dispatcher installs the helper before any publish. The helper delivers it to
     * the session's {@link SSLHandshakeFinished} target: the tunnel hook on the
     * server transport ({@code SSLNIOSocketHandler.sslHandshakeSuccessful}), the
     * {@code connectedFinished()} notification on the plain client
     * ({@code TCPSessionCallback}), and the SECURE/READY gate on the validator
     * machine ({@code ClientSSLHelper} delegating to its {@code SSLClientBridge}).
     * Then re-publishes the current status if the net in-buffer still holds
     * buffered bytes — TLS 1.3 in particular may interleave application data in
     * the same flight as the handshake finish.
     * </p>
     * <p>
     * The exception path is <b>terminal</b>: {@code SharedIOUtil.close(config, callback)}
     * (null-tolerant, never throws) fully tears the session down before the callback is
     * notified — there is no continuation after a completion failure.
     * </p>
     *
     * @param config   current SSL session state
     * @param callback passed to the drain-chain publish; may be {@code null}
     * @return elapsed processing time in milliseconds
     */
    public static long _finished(SSLConfigInt config, BaseSessionCallback<SSLConfigInt> callback) {
        long ts = System.currentTimeMillis();

        // ********************************************
        // Very crucial steps
        // ********************************************
        try {
            config.getSSLConnectionHelper().notifySSLHandshakeFinished();
        } catch (Exception e) {
            SharedIOUtil.close(config, callback);
            callback.exception(e);
        }


        if (config.getSSLIOBuffers().getInBuffer().position() > 0) {
            //**************************************************
            // ||-----DATA BUFFER------ ||
            // ||Handshake data|App data||
            // ||-----------------------||
            // The buffer has app data that needs to be decrypted
            //**************************************************
            config.getSSLConnectionHelper().publish(config.getHandshakeStatus(), callback);
        }

        return System.currentTimeMillis() - ts;
    }

    /**
     * Handler for {@link javax.net.ssl.SSLEngineResult.HandshakeStatus#NEED_TASK}:
     * run all pending delegated tasks, then re-publish the updated status.
     * <p>
     * Tasks are drained from the engine's {@code getDelegatedTask()} and
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
    public static long _needTask(SSLConfigInt config, BaseSessionCallback<SSLConfigInt> callback) {
        long ts = System.currentTimeMillis();

        Runnable toRun;
        while ((toRun = config.getSSLEngine().getDelegatedTask()) != null)
            toRun.run();

        SSLEngineResult.HandshakeStatus status = config.getHandshakeStatus();

        ts = System.currentTimeMillis() - ts;

        if (log.isEnabled()) log.getLogger().info("After run: " + status);

        config.getSSLConnectionHelper().publish(status, callback);

        return ts;
    }


    /**
     * Handler for {@link javax.net.ssl.SSLEngineResult.HandshakeStatus#NEED_UNWRAP}
     * (and Java 9+ {@code NEED_UNWRAP_AGAIN}): read ciphertext and unwrap into
     * the engine during handshake.
     * <p>
     * The destination is {@link SSLConfigInt#getInDecryptedBuffer()} — the session's real
     * decryption buffer, allocated at {@code SSLSession.getApplicationBufferSize()} by
     * {@code beginHandshake} — not {@link ByteBufferUtil#EMPTY}. A single {@code unwrap}
     * can never produce more plaintext than that, so {@code BUFFER_OVERFLOW} is unreachable
     * here (invariant 3 above): its branch is a dead-on-arrival guard, not a recovery gap.
     * {@code BUFFER_UNDERFLOW} simply returns — the selector will re-dispatch when more
     * wire bytes arrive.
     * </p>
     * <p>
     * Handshake records produce no application data. When a record does produce some — a
     * flight interleaving app data with the handshake finish — this handler deliberately
     * does not dispatch it: the published status carries the session forward through the
     * handshake chain to {@link #_finished _finished}, which re-publishes
     * {@code NOT_HANDSHAKING} while the net in-buffer still holds bytes, and
     * {@link #_notHandshaking _notHandshaking} then delivers the decryption buffer to the
     * callback in stream order. The state transition owns delivery, not this handler.
     * </p>
     *
     * @param config   current SSL session state
     * @param callback passed through to the next state handler
     * @return elapsed processing time in milliseconds
     */
    public static long _needUnwrap(SSLConfigInt config, BaseSessionCallback<SSLConfigInt> callback) {

        long ts = System.currentTimeMillis();
        if (log.isEnabled()) log.getLogger().info("Entry: " + config.getHandshakeStatus());

        if (config.getHandshakeStatus() == NEED_UNWRAP || SUS.enumName(config.getHandshakeStatus()).equals("NEED_UNWRAP_AGAIN")) {
            try {

                int bytesRead = config.getChannel().read(config.getSSLIOBuffers().getInBuffer());
                if (bytesRead == -1) {
                    if (log.isEnabled())
                        log.getLogger().info("SSLCHANNEL-CLOSED-NEED_UNWRAP: " + config.getHandshakeStatus() + " bytes read: " + bytesRead);
                    config.close();
                } else {
                    // bytesRead 0 or more
                    // even if we have read zero it will trigger BUFFER_UNDERFLOW then we wait for incoming
                    // data
                    if (log.isEnabled())
                        log.getLogger().info("BEFORE-UNWRAP: " + config.getSSLIOBuffers().getInBuffer() + " bytes read " + bytesRead);
                    SSLEngineResult result = smartSSLUnwrap(config.getSSLEngine(), config.getSSLIOBuffers().getInBuffer(), config.getInDecryptedBuffer(), true, true);


                    if (log.isEnabled()) {
                        log.getLogger().info("AFTER-NEED_UNWRAP-HANDSHAKING: " + result + " bytes read: " + bytesRead);
                        log.getLogger().info("AFTER-NEED_UNWRAP-HANDSHAKING inNetData: " + config.getSSLIOBuffers().getInBuffer() + " inAppData: " + config.getInDecryptedBuffer());
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
                            config.getSSLConnectionHelper().publish(result.getHandshakeStatus(), callback);
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
                if (callback != null) callback.exception(e);
                SharedIOUtil.close(config);
            }
        }
        return System.currentTimeMillis() - ts;
    }


    public static SSLEngineResult smartSSLWrap(SSLEngine sslEngine, ByteBuffer source, ByteBuffer destination, boolean flipSource, boolean compactSource)
            throws SSLException {
        if (flipSource)
            ((Buffer) source).flip();
        SSLEngineResult ret = sslEngine.wrap(source, destination);
        if (compactSource)
            source.compact();
        return ret;
    }


    public static SSLEngineResult smartSSLUnwrap(SSLEngine sslEngine, ByteBuffer source, ByteBuffer destination, boolean flipSource, boolean compactSource) throws SSLException {
        if (flipSource)
            ((Buffer) source).flip();
        SSLEngineResult ret = sslEngine.unwrap(source, destination);
        if (compactSource)
            source.compact();
        return ret;
    }


    /**
     * Handler for {@link javax.net.ssl.SSLEngineResult.HandshakeStatus#NEED_WRAP}:
     * generate outbound handshake bytes and send them on the channel.
     * <p>
     * The source is {@link ByteBufferUtil#EMPTY} because no application data is
     * consumed during handshake. On {@code OK}, the produced ciphertext in
     * the net out-buffer is drained to the channel via
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
    public static long _needWrap(SSLConfigInt config, BaseSessionCallback<SSLConfigInt> callback) {
        long ts = System.currentTimeMillis();

        if (config.getHandshakeStatus() == NEED_WRAP) {
            try {
                SSLEngineResult result = smartSSLWrap(config.getSSLEngine(), ByteBufferUtil.EMPTY, config.getSSLIOBuffers().getOutBuffer(), true, true);
                // at handshake stage, data in appOut won't be
                // processed hence dummy buffer
                if (log.isEnabled())
                    log.getLogger().info("AFTER-NEED_WRAP-HANDSHAKING: " + result);

                switch (result.getStatus()) {
                    case BUFFER_UNDERFLOW:
                    case BUFFER_OVERFLOW:
                        config.forceCloseEnabled(true);
                        throw new IllegalStateException(result + " invalid state context " + config.getSSLIOBuffers().getOutBuffer());
                    case OK:
                        int written = ByteBufferUtil.smartWrite(null, config.getChannel(), config.getSSLIOBuffers().getOutBuffer(), true);
                        if (log.isEnabled())
                            log.getLogger().info(result.getHandshakeStatus() + " After writing data HANDSHAKING-NEED_WRAP: " + config.getSSLIOBuffers().getOutBuffer() + " written:" + written);
                        config.getSSLConnectionHelper().publish(result.getHandshakeStatus(), callback);
                        break;
                    case CLOSED:
                        config.close();
                        break;
                }
            } catch (Exception e) {
                if (log.isEnabled())
                    e.printStackTrace();
                if (callback != null) callback.exception(e);

                SharedIOUtil.close(config);
            }
        }
        return System.currentTimeMillis() - ts;
    }

    /**
     * Single-record SSL write: encrypt {@code bb} into the net out-buffer and
     * drain the resulting ciphertext to {@code dataChannel}.
     * <p>
     * <b>Buffer-mode contract.</b> The {@code flip} flag describes the caller's
     * buffer mode for {@code bb}:
     * </p>
     * <ul>
     *     <li>{@code flip=true} — {@code bb} is in write-mode
     *         (position = end of plaintext, limit = capacity);
     *         {@link SSLUtil#smartSSLWrap} will flip it before wrap.</li>
     *     <li>{@code flip=false} — {@code bb} is already in read-mode
     *         (position = start of plaintext, limit = end); no flip performed.</li>
     * </ul>
     * <p>
     * After wrap, {@link SSLUtil#smartSSLWrap} compacts {@code bb}.
     * The destination net out-buffer is always in write-mode after wrap,
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
    private static int _sslWrite(SSLConfigInt sslConfig, ByteChannel dataChannel, ByteBuffer bb, UsageTracker usageTracker, AutoCloseable closeable, boolean flip) throws IOException {
        int written = -1;
        if (sslConfig.getSSLEngine().getHandshakeStatus() == NOT_HANDSHAKING) {


            SSLEngineResult result = smartSSLWrap(sslConfig.getSSLEngine(), bb, sslConfig.getSSLIOBuffers().getOutBuffer(), flip, true);
            if (log.isEnabled())
                log.getLogger().info("AFTER-NEED_WRAP-PROCESSING: " + result);
            switch (result.getStatus()) {
                case BUFFER_UNDERFLOW:
                case BUFFER_OVERFLOW:
                    throw new IOException(result.getStatus() + " invalid state context buffer size " +
                            SUS.toCanonicalID(',', sslConfig.getSSLIOBuffers().getOutBuffer().capacity(), sslConfig.getSSLIOBuffers().getOutBuffer().limit(), sslConfig.getSSLIOBuffers().getOutBuffer().position()));
                case OK:
                    try {
                        written = ByteBufferUtil.smartWrite(null, dataChannel, sslConfig.getSSLIOBuffers().getOutBuffer(), true);
                        if (usageTracker != null) usageTracker.updateUsage();
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
    public static int sslChunkedWrite(SSLConfigInt sslConfig, ByteChannel dataChannel, ByteBuffer src, UsageTracker usageTracker, AutoCloseable closeable, boolean flip) throws IOException {
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
