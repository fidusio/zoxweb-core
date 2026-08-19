package org.zoxweb.server.net.ssl;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.shared.util.RateCounter;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;

public class SSLDataReadyState
        extends State {
    static RateCounter rcNotHandshaking = new RateCounter("NotHandshaking");

    static class NotHandshaking extends TriggerConsumer<BaseSessionCallback<SSLConfigInt>> {
        NotHandshaking() {
            super(NOT_HANDSHAKING);
        }

        @Override
        public void accept(BaseSessionCallback<SSLConfigInt> callback) {
            rcNotHandshaking.register(SSLUtil._notHandshaking((SSLConfigInt) getStateMachine().getConfig(), callback));
        }
    }

    public SSLDataReadyState() {
        super(SSLStateMachine.SessionState.DATA_READY);
        register(new NotHandshaking());
    }

}
