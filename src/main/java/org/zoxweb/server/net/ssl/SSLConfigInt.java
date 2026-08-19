package org.zoxweb.server.net.ssl;

import org.zoxweb.server.io.IOBuffers;
import org.zoxweb.shared.io.CloseableType;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * Read-side facade over a live TLS session's state: the {@link SSLEngine}, the
 * encrypted channel, the net-in/net-out {@link IOBuffers} pair, and the
 * application (plaintext) buffer.
 * <p>
 * The sole implementation is {@link SSLSessionConfig}; this interface exists so that
 * code outside the SSL engine room — {@link SSLUtil}'s post-handshake write path
 * ({@code sslChunkedWrite}), {@code ConnectionCallback.sslHandshakeSuccessful},
 * {@code CommonChannelOutputStream}, and the {@code net.common.sm} client states —
 * can encrypt/decrypt against the session without seeing the handshake machinery
 * (helpers, selector plumbing, remote-tunnel fields) that {@code SSLSessionConfig}
 * also carries.
 * </p>
 * <p>
 * <b>Ownership and lifecycle.</b> The implementation owns every object exposed here;
 * consumers borrow, never manage. {@link #getSSLIOBuffers()} is {@code null} until
 * {@link #beginHandshake(IOBuffers)} installs the pair and fills any missing or
 * undersized buffer from the {@code ByteBufferUtil} pool. On session {@link #close()}
 * the net buffers are recached via {@code IOBuffers.close()} and the application
 * buffer directly — so no buffer reference may be retained or read after the session
 * is closed (check {@link #isClosed()}). {@link #close()} performs the full session
 * teardown: best-effort TLS close_notify, channel close, and buffer recache; it is
 * idempotent.
 * </p>
 * <p>
 * <b>Concurrency.</b> None of the accessors synchronize. The handshake is serialized
 * per session on one worker by design; after the handshake, callers coordinate their
 * own access — e.g. {@code SSLUtil._sslWrite} wraps into the {@link IOBuffers}
 * out-buffer and drains it to the channel within a single call.
 * </p>
 */
public interface SSLConfigInt
extends CloseableType {
    /** The session's crypto engine; also the source of the defaults below. Never {@code null}. */
    SSLEngine getSSLEngine();

    /** The encrypted (network-facing) channel ciphertext is read from and written to. */
    ByteChannel getChannel();

    //ByteBuffer getSSLInBuffer();

    //ByteBuffer getSSLOutBuffer();


    /**
     * The session's ciphertext buffer pair, each with capacity &ge;
     * {@link #getPacketBufferSize()}:
     * <ul>
     *     <li>{@code getInBuffer()} — inbound ciphertext, network &rarr;
     *         {@code SSLEngine.unwrap} source;</li>
     *     <li>{@code getOutBuffer()} — outbound ciphertext,
     *         {@code SSLEngine.wrap} destination &rarr; network.</li>
     * </ul>
     * {@code null} before {@link #beginHandshake(IOBuffers)}; closed (buffers
     * recached, invalid) after {@link #close()}.
     */
    IOBuffers getSSLIOBuffers();

    /**
     * Decrypted plaintext buffer ({@code SSLEngine.unwrap} destination), handed to the
     * application read callback. Capacity &ge; {@link #getApplicationBufferSize()}.
     * {@code null} before {@link #beginHandshake(IOBuffers)}; recached (invalid) after
     * {@link #close()}.
     */
    ByteBuffer getInDecryptionBuffer();



    /**
     * One-time session start: installs the net-buffer pair, sets the engine's
     * client/server mode, and calls {@code SSLEngine.beginHandshake()}. Only the
     * first invocation acts; subsequent calls are no-ops that return the installed pair.
     * <p>
     * {@code ioBuffers} lets a caller donate buffers for reuse across sessions:
     * {@code null} means allocate a fresh pair from the pool; a supplied pair keeps
     * any buffer whose capacity is &ge; {@link #getPacketBufferSize()} and replaces
     * the rest. A replaced (undersized) buffer is <b>deliberately not recached</b> —
     * only right-sized buffers may return to the pool — it is simply dropped to GC.
     * The session takes ownership either way — the pair is closed (buffers recached)
     * by {@link #close()}.
     * </p>
     *
     * @param ioBuffers donated buffer pair, or {@code null} for pooled allocation
     * @return the installed pair, as {@link #getSSLIOBuffers()} will return it
     * @throws SSLException if the engine rejects the handshake start
     */
    IOBuffers beginHandshake(IOBuffers ioBuffers) throws SSLException;

    /**
     * Maximum plaintext size a single {@code unwrap} can produce, per the engine's
     * current {@code SSLSession}; sizes {@link #getInDecryptionBuffer()} and caps
     * {@code SSLUtil.sslChunkedWrite}'s single-shot path.
     */
    default int getApplicationBufferSize() {
        return getSSLEngine().getSession().getApplicationBufferSize();
    }

    /**
     * Maximum TLS record size, per the engine's current {@code SSLSession}; sizes
     * both buffers of {@link #getSSLIOBuffers()}.
     */
    default int getPacketBufferSize() {return getSSLEngine().getSession().getPacketBufferSize();}

    /**
     * The engine's live handshake status. Application data may flow only at
     * {@code NOT_HANDSHAKING} — {@code SSLUtil._sslWrite} rejects sends in any other state.
     */
    default SSLEngineResult.HandshakeStatus getHandshakeStatus() {
        return getSSLEngine().getHandshakeStatus();
    }
}
