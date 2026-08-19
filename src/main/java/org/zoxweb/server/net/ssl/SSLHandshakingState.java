package org.zoxweb.server.net.ssl;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.shared.util.RateCounter;

import java.util.concurrent.atomic.AtomicLong;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

public class SSLHandshakingState extends State {

    private final static AtomicLong counter = new AtomicLong(0);

    static RateCounter rcFinished = new RateCounter("Finished");
    static RateCounter rcNeedWrap = new RateCounter("NeedWrap");
    static RateCounter rcNeedTask = new RateCounter("NeedTask");
    static RateCounter rcNeedUnwrap = new RateCounter("NeedUnwrap");

    static class NeedWrap extends TriggerConsumer<BaseSessionCallback<SSLConfigInt>> {
        NeedWrap() {
            super(NEED_WRAP);
        }

        @Override
        public void accept(BaseSessionCallback<SSLConfigInt> callback) {
            rcNeedWrap.register(SSLUtil._needWrap((SSLConfigInt) getStateMachine().getConfig(), callback));
        }
    }

    static class NeedUnwrap extends TriggerConsumer<BaseSessionCallback<SSLConfigInt>> {
        NeedUnwrap() {
            super("NEED_UNWRAP", "NEED_UNWRAP_AGAIN");
        }

        @Override
        public void accept(BaseSessionCallback<SSLConfigInt> callback) {
            rcNeedUnwrap.register(SSLUtil._needUnwrap((SSLConfigInt) getStateMachine().getConfig(), callback));
        }
    }

    static class NeedTask extends TriggerConsumer<BaseSessionCallback<SSLConfigInt>> {

        NeedTask() {
            super(NEED_TASK);
        }

        @Override
        public void accept(BaseSessionCallback<SSLConfigInt> callback) {
            rcNeedTask.register(SSLUtil._needTask((SSLConfigInt) getStateMachine().getConfig(), callback));
        }
    }


    static class Finished extends TriggerConsumer<BaseSessionCallback<SSLConfigInt>> {

        Finished() {
            super(FINISHED);
        }

        @Override
        public void accept(BaseSessionCallback<SSLConfigInt> callback) {
            rcFinished.register(SSLUtil._finished((SSLConfigInt) getStateMachine().getConfig(), callback));
        }
    }

    public SSLHandshakingState() {
        super(SSLStateMachine.SessionState.HANDSHAKING);
        counter.incrementAndGet();
        register(new NeedTask())
                .register(new NeedWrap())
                .register(new NeedUnwrap())
                .register(new Finished())
        ;

    }

}
