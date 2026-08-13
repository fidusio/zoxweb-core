package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.ssl.SSLSessionConfig;
import org.zoxweb.server.net.ssl.SSLUtil;
import org.zoxweb.shared.io.SharedIOUtil;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;

/**
 * Post-handshake data state: unwraps the ciphertext the transport router buffered in
 * {@code inSSLNetData} via {@link SSLUtil#smartSSLUnwrap} — deliberately <b>without a channel
 * read</b>. In this stack TCPSMCallback drains the socket and the router feeds the net buffer;
 * a handler that reads the channel itself ({@code _notHandshaking}) would see {@code -1} when
 * the peer's final data and FIN arrive together and close the session with the last records
 * still buffered and undecrypted. EOF detection belongs to TCPSMCallback's read loop, which
 * orders data-before-EOF correctly.
 * <p>
 * Decrypted app data goes to the callback (the session's {@link SSLClientBridge}), which fully
 * drains the write-mode application buffer. {@code CLOSED} (close_notify) tears the session
 * down; a post-handshake status change (KeyUpdate, renegotiation) is republished to the
 * handshake states.
 */
public class SSLClientDataState extends State<Object> {

    public static final String NAME = "ssl-data";

    public SSLClientDataState() {
        super(NAME);
        register(new NotHandshaking());
    }

    private class NotHandshaking extends TriggerConsumer<BaseSessionCallback<SSLSessionConfig>> {
        NotHandshaking() {
            super(NOT_HANDSHAKING);
        }

        @Override
        public void accept(BaseSessionCallback<SSLSessionConfig> callback) {
            ClientSessionContext ctx = (ClientSessionContext) getStateMachine().getConfig();
            SSLSessionConfig cfg = ctx.getSSLConfig();
            ByteBuffer net = cfg.getSSLInboundBuffer();
            try {
                SSLEngineResult result;
                do {
                    result = SSLUtil.smartSSLUnwrap(cfg.getSSLEngine(), net, cfg.getSSLApplicationBuffer(), true, true);
                    switch (result.getStatus()) {
                        case BUFFER_UNDERFLOW:
                            // partial record — wait for the router's next feed
                            return;
                        case BUFFER_OVERFLOW:
                            throw new SSLException("BUFFER_OVERFLOW: application buffer not drained");
                        case OK:
                            if (callback != null && result.bytesProduced() > 0)
                                callback.accept(cfg.getSSLApplicationBuffer());
                            break;
                        case CLOSED:
                            // close_notify received — full session teardown (publishes CLOSED)
                            SharedIOUtil.close(ctx.getSession());
                            return;
                    }
                    if (cfg.getHandshakeStatus() != NOT_HANDSHAKING) {
                        // KeyUpdate / renegotiation — hand back to the handshake states
                        cfg.sslConnectionHelper.publish(cfg.getHandshakeStatus(), callback);
                        return;
                    }
                    // net is back in write-mode after compact: position > 0 = leftover ciphertext
                } while (net.position() > 0 && (result.bytesConsumed() > 0 || result.bytesProduced() > 0));
            } catch (SSLException e) {
                if (callback != null)
                    callback.exception(e);
                else
                    ctx.fail(e);
            }
        }
    }
}
