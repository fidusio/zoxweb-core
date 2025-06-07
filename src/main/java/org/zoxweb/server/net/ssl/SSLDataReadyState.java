package org.zoxweb.server.net.ssl;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.shared.util.RateCounter;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;

public class SSLDataReadyState
        extends State {
    static RateCounter rcNotHandshaking = new RateCounter("NotHandshaking");

    static class NotHandshaking extends TriggerConsumer<SSLSessionCallback> {
        NotHandshaking() {
            super(NOT_HANDSHAKING);
        }

        @Override
        public void accept(SSLSessionCallback callback) {
            rcNotHandshaking.register(SSLUtil._notHandshaking((SSLSessionConfig) getStateMachine().getConfig(), callback));
        }
    }

    public SSLDataReadyState() {
        super(SSLStateMachine.SessionState.DATA_READY);
        register(new NotHandshaking());
    }

}
