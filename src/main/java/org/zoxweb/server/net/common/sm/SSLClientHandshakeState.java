package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.ssl.SSLSessionConfig;
import org.zoxweb.server.net.ssl.SSLUtil;
import org.zoxweb.shared.util.SUS;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

/**
 * Client-side handshake orchestration state (the SSLStateMachineV2 shape): one consumer per
 * {@link javax.net.ssl.SSLEngineResult.HandshakeStatus}. {@code NEED_WRAP}, {@code NEED_TASK}
 * and {@code FINISHED} are one-line delegations to the load-proven {@code SSLUtil} handlers —
 * orchestration only, the engine steps are untouched.
 * <p>
 * {@code NEED_UNWRAP} is handled here <b>without a channel read</b> (the same discipline as
 * {@link SSLClientDataState}): in this stack TCPSMCallback drains the socket and the transport
 * router feeds {@code inSSLNetData}. {@code SSLUtil._needUnwrap}'s own {@code sslChannel.read}
 * would compete with the router — when a packet is fed in chunks (net buffer nearly full), a
 * handler-side read between chunks pulls newer socket bytes ahead of the still-unfed older
 * bytes, corrupting the ciphertext order. Unwrapping only what the router buffered keeps the
 * byte order exactly as received; {@code BUFFER_UNDERFLOW} simply waits for the router's next
 * feed, and EOF detection belongs to TCPSMCallback's read loop.
 * <p>
 * The trigger payload is the session's {@link SSLClientBridge}, or <b>null</b> during the
 * {@code SSLSessionConfig.close()} drain — the handlers tolerate a null callback.
 * {@code NEED_UNWRAP_AGAIN} (DTLS, string ID — the enum constant does not exist on Java 8) is
 * routed to the same unwrap consumer, mirroring {@code SSLHandshakingState}.
 */
public class SSLClientHandshakeState extends State<Object> {

    public static final String NAME = "ssl-handshaking";

    public SSLClientHandshakeState() {
        super(NAME);
        register(new NeedWrap());
        register(new NeedUnwrap());
        register(new NeedTask());
        register(new Finished());
    }

    private static SSLSessionConfig config(TriggerConsumer<?> tc) {
        return ((ClientSessionContext) tc.getStateMachine().getConfig()).getSSLConfig();
    }

    private class NeedWrap extends TriggerConsumer<BaseSessionCallback<SSLSessionConfig>> {
        NeedWrap() {
            super(NEED_WRAP);
        }

        @Override
        public void accept(BaseSessionCallback<SSLSessionConfig> callback) {
            SSLUtil._needWrap(config(this), callback);
        }
    }

    private class NeedUnwrap extends TriggerConsumer<BaseSessionCallback<SSLSessionConfig>> {
        NeedUnwrap() {
            super("NEED_UNWRAP", "NEED_UNWRAP_AGAIN");
        }

        @Override
        public void accept(BaseSessionCallback<SSLSessionConfig> callback) {
            SSLSessionConfig config = config(this);
            if (config == null || config.isClosed())
                return;
            // same guard as SSLUtil._needUnwrap: a stale status publish must not unwrap
            SSLEngineResult.HandshakeStatus hs = config.getHandshakeStatus();
            if (hs != NEED_UNWRAP && !"NEED_UNWRAP_AGAIN".equals(SUS.enumName(hs)))
                return;
            try {
                // router-fed unwrap — NO channel read, see class javadoc; handshake records
                // produce no app data, hence the EMPTY destination (as SSLUtil._needUnwrap)
                SSLEngineResult result = SSLUtil.smartSSLUnwrap(
                        config.getSSLEngine(), config.getSSLInBuffer(), ByteBufferUtil.EMPTY, true, true);
                switch (result.getStatus()) {
                    case BUFFER_UNDERFLOW:
                        // partial record — wait for the router's next feed
                        return;
                    case BUFFER_OVERFLOW:
                        throw new SSLException("BUFFER_OVERFLOW: NEED_UNWRAP should never overflow");
                    case OK:
                        config.sslConnectionHelper.publish(result.getHandshakeStatus(), callback);
                        break;
                    case CLOSED:
                        config.close();
                        break;
                }
            } catch (Exception e) {
                if (callback != null)
                    callback.exception(e);
                config.close();
            }
        }
    }

    private class NeedTask extends TriggerConsumer<BaseSessionCallback<SSLSessionConfig>> {
        NeedTask() {
            super(NEED_TASK);
        }

        @Override
        public void accept(BaseSessionCallback<SSLSessionConfig> callback) {
            SSLUtil._needTask(config(this), callback);
        }
    }

    private class Finished extends TriggerConsumer<BaseSessionCallback<SSLSessionConfig>> {
        Finished() {
            super(FINISHED);
        }

        @Override
        public void accept(BaseSessionCallback<SSLSessionConfig> callback) {
            SSLUtil._finished(config(this), callback);
        }
    }
}
