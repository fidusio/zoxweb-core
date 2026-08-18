package org.zoxweb.server.net.ssl;

import org.zoxweb.server.net.IOBuffers;
import org.zoxweb.shared.io.CloseableType;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * Read-side facade over a live TLS session's state: the {@link SSLEngine}, the
 * encrypted channel, and the three session buffers (net-in, net-out, application).
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
 * consumers borrow, never manage. The buffers are {@code null} until
 * {@code SSLSessionConfig.beginHandshake(...)} allocates them from the
 * {@code ByteBufferUtil} pool, and are recached into the pool by {@link #close()} —
 * so a reference must never be retained or read after the session is closed
 * (check {@link #isClosed()}). {@link #close()} performs the full session teardown:
 * best-effort TLS close_notify, channel close, and buffer recache; it is idempotent.
 * </p>
 * <p>
 * <b>Concurrency.</b> None of the accessors synchronize. The handshake is serialized
 * per session on one worker by design; after the handshake, callers coordinate their
 * own access — e.g. {@code SSLUtil._sslWrite} wraps into {@link #getSSLIOBuffers().getOutBuffer()}
 * and drains it to the channel within a single call.
 * </p>
 */
public interface SSLConfigInt
extends CloseableType {
    /** The session's crypto engine; also the source of the defaults below. Never {@code null}. */
    SSLEngine getSSLEngine();

    /** The encrypted (network-facing) channel ciphertext is read from and written to. */
    ByteChannel getChannel();

    /**
     * Inbound ciphertext buffer (network &rarr; {@code SSLEngine.unwrap} source).
     * Capacity &ge; {@link #getPacketBufferSize()}. {@code null} before
     * {@code beginHandshake}; recached (invalid) after {@link #close()}.
     */
    //ByteBuffer getSSLInBuffer();

    /**
     * Outbound ciphertext buffer ({@code SSLEngine.wrap} destination &rarr; network).
     * Capacity &ge; {@link #getPacketBufferSize()}. {@code null} before
     * {@code beginHandshake}; recached (invalid) after {@link #close()}.
     */
    //ByteBuffer getSSLOutBuffer();


    IOBuffers getSSLIOBuffers();

    /**
     * Decrypted plaintext buffer ({@code SSLEngine.unwrap} destination), handed to the
     * application read callback. Capacity &ge; {@link #getApplicationBufferSize()}.
     * {@code null} before {@code beginHandshake}; recached (invalid) after {@link #close()}.
     */
    ByteBuffer getSSLApplicationBuffer();

    /**
     * Maximum plaintext size a single {@code unwrap} can produce, per the engine's
     * current {@code SSLSession}; sizes {@link #getSSLApplicationBuffer()} and caps
     * {@code SSLUtil.sslChunkedWrite}'s single-shot path.
     */
    default int getApplicationBufferSize() {
        return getSSLEngine().getSession().getApplicationBufferSize();
    }

    /**
     * Maximum TLS record size, per the engine's current {@code SSLSession}; sizes
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
