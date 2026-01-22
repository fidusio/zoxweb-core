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

    static class NeedWrap extends TriggerConsumer<BaseSessionCallback<SSLSessionConfig>> {
        NeedWrap() {
            super(NEED_WRAP);
        }

        @Override
        public void accept(BaseSessionCallback<SSLSessionConfig> callback) {
            rcNeedWrap.register(SSLUtil._needWrap((SSLSessionConfig) getStateMachine().getConfig(), callback));
        }
    }

    static class NeedUnwrap extends TriggerConsumer<BaseSessionCallback<SSLSessionConfig>> {
        NeedUnwrap() {
            super("NEED_UNWRAP", "NEED_UNWRAP_AGAIN");
        }

        @Override
        public void accept(BaseSessionCallback<SSLSessionConfig> callback) {
            rcNeedUnwrap.register(SSLUtil._needUnwrap((SSLSessionConfig) getStateMachine().getConfig(), callback));
        }
    }

    static class NeedTask extends TriggerConsumer<BaseSessionCallback<SSLSessionConfig>> {

        NeedTask() {
            super(NEED_TASK);
        }

        @Override
        public void accept(BaseSessionCallback<SSLSessionConfig> callback) {
            rcNeedTask.register(SSLUtil._needTask((SSLSessionConfig) getStateMachine().getConfig(), callback));
        }
    }


    static class Finished extends TriggerConsumer<BaseSessionCallback<SSLSessionConfig>> {

        Finished() {
            super(FINISHED);
        }

        @Override
        public void accept(BaseSessionCallback<SSLSessionConfig> callback) {
            rcFinished.register(SSLUtil._finished((SSLSessionConfig) getStateMachine().getConfig(), callback));
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
