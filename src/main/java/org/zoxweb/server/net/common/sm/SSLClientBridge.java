package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.common.ConnectionCallback;
import org.zoxweb.server.net.ssl.SSLConfigInt;
import org.zoxweb.server.net.ssl.SSLSessionConfig;
import org.zoxweb.shared.io.SharedIOUtil;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * The handler-facing SSL callback of a client session: the SSLUtil handlers require a
 * {@code BaseSessionCallback<SSLSessionConfig>} that also implements {@code ConnectionCallback}
 * (for the {@code _finished} client notification), and TCPSMCallback is deliberately neither —
 * this bridge is. One bridge per session, created by {@link SSLClientPhase#upgrade}.
 * <ul>
 * <li>{@link #accept(ByteBuffer)} — the single decrypted-data delivery point
 * ({@code _notHandshaking}): the engine's {@code inAppData} arrives in <b>write-mode and is
 * reused</b> across unwrap iterations, so the plaintext is copied into a detached pooled buffer
 * published as {@link ClientEvent#IN_DATA}, and the source is fully drained (cleared) — the
 * "BUFFER_OVERFLOW unreachable" invariant depends on that.</li>
 * <li>{@link #sslHandshakeSuccessful(SSLConfigInt)} — flips the session output stream to
 * encrypted writes, marks the session secure, publishes {@link ClientEvent#SECURE} and
 * completes the SSL phase.</li>
 * <li>{@link #exception(Throwable)} — routes into the session's failure path.</li>
 * <li>The closeable delegate closes the session, so {@code _finished}'s error path
 * ({@code SharedIOUtil.close(config, callback)}) performs a real teardown.</li>
 * </ul>
 */
class SSLClientBridge extends BaseSessionCallback<SSLSessionConfig>
        implements ConnectionCallback<ByteBuffer> {

    private final ClientConSM sm;

    SSLClientBridge(ClientConSM sm) {
        this.sm = sm;
        closeableDelegate.setDelegate(() -> SharedIOUtil.close(sm.getContext().getSession()));
    }

    @Override
    public void accept(ByteBuffer inAppData) {
        ((Buffer) inAppData).flip();
        if (inAppData.hasRemaining()) {
            ByteBuffer copy = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.HEAP,
                    inAppData.array(), 0, inAppData.remaining(), true);
            sm.publishSync(ClientEvent.IN_DATA, copy);
        }
        ((Buffer) inAppData).clear();
    }

    @Override
    public void sslHandshakeSuccessful(SSLConfigInt sci) throws IOException {
        ClientSessionContext ctx = sm.getContext();
        ctx.getSession().sslHandshakeSuccessful(sci);
        ctx.setMode(ClientSessionContext.Mode.TLS_SECURE);
        sm.publishSync(ClientEvent.SECURE, sci);
        ctx.phaseComplete(SSLClientPhase.NAME);
    }

    @Override
    public void exception(Throwable e) {
        sm.getContext().fail(e);
    }

    @Override
    public void accept(SelectionKey key) {
        throw new UnsupportedOperationException("bridge is not a read dispatch target");
    }

    @Override
    public int interestOps() {
        return SelectionKey.OP_READ;
    }
}
