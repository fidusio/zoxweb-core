package org.zoxweb.server.net.ssl;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.shared.util.RateCounter;

import java.util.concurrent.atomic.AtomicLong;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

public class SSLHandshakingState extends State {

    private final static AtomicLong counter = new AtomicLong(0);

    static RateCounter rcFinished = new RateCounter("Finished");
    static RateCounter rcNeedWrap = new RateCounter("NeedWrap");
    static RateCounter rcNeedTask = new RateCounter("NeedTask");
    static RateCounter rcNeedUnwrap = new RateCounter("NeedUnwrap");

    static class NeedWrap extends TriggerConsumer<SSLSessionCallback>
    {
        NeedWrap()
        {
            super(NEED_WRAP);
        }

        @Override
        public void accept(SSLSessionCallback callback)
        {
            rcNeedWrap.register(SSLUtil._needWrap((SSLSessionConfig)getStateMachine().getConfig(), callback));
        }
    }

    static class NeedUnwrap extends TriggerConsumer<SSLSessionCallback>
    {
        NeedUnwrap()
        {
            super("NEED_UNWRAP", "NEED_UNWRAP_AGAIN");
        }

        @Override
        public void accept(SSLSessionCallback callback)
        {
            rcNeedUnwrap.register(SSLUtil._needUnwrap((SSLSessionConfig)getStateMachine().getConfig(), callback));
        }
    }

    static class NeedTask extends TriggerConsumer<SSLSessionCallback>
    {

        NeedTask() {
            super(NEED_TASK);
        }

        @Override
        public void accept(SSLSessionCallback callback)
        {
            rcNeedTask.register(SSLUtil._needTask((SSLSessionConfig)getStateMachine().getConfig(), callback));
        }
    }



    static class Finished extends TriggerConsumer<SSLSessionCallback>
    {

        Finished() {
            super(FINISHED);
        }

        @Override
        public void accept(SSLSessionCallback callback)
        {
            rcFinished.register(SSLUtil._finished((SSLSessionConfig)getStateMachine().getConfig(), callback));
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
