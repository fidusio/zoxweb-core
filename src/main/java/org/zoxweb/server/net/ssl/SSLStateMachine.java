package org.zoxweb.server.net.ssl;

import org.zoxweb.server.fsm.*;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public class SSLStateMachine extends StateMachine<SSLConfigInt>
        implements SSLConnectionHelper<SSLConfigInt> {


    @Override
    public void publish(SSLEngineResult.HandshakeStatus status, BaseSessionCallback<SSLConfigInt> callback) {
        if (!isClosed())
            publishSync(new Trigger<BaseSessionCallback<SSLConfigInt>>(this, status, getCurrentState(), callback));
    }


    //private final static AtomicLong HANDSHAKE_COUNTER = new AtomicLong();
    public enum SessionState
            implements GetName {
        DATA_READY("data-ready"),
        HANDSHAKING("handshaking"),
        POST_HANDSHAKE("post-handshake"),


        /**
         * Read data state will unwrap data via it trigger in the read state
         * and in the handshaking state will unwrap data for the handshake process
         * it is identified by checking the SSLEngine NOT_HANDSHAKING status
         */

        CLOSE("close"),

        ;


        private final String name;

        SessionState(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }


    static final AtomicLong counter = new AtomicLong();


    private volatile SSLNIOSocketHandler sslNIOSocket = null;



    private SSLStateMachine(long id, Executor executor) {
        super("SSLSessionStateMachine-" + id, executor);
    }

    @Override
    public void notifySSLHandshakeFinished() throws IOException {
        sslNIOSocket.sslHandshakeSuccessful(getConfig());
    }


    public static SSLStateMachine create(SSLNIOSocketHandler sslnioSocket) {
        SSLStateMachine sslSessionSM = new SSLStateMachine(counter.incrementAndGet(), null);
        sslSessionSM.sslNIOSocket = sslnioSocket;


        SSLSessionConfig config = new SSLSessionConfig(sslnioSocket.getSSLContextInfo());
        sslSessionSM.setConfig(config);
        config.sslConnectionHelper = sslSessionSM;



        sslSessionSM.setConfig(config)
                .setEventLogEnabled(false)
                .register(new State<>(StateInt.States.INIT).register((a)->{if (log.isEnabled()) log.getLogger().info(sslSessionSM.getName() + " CREATED");}, StateInt.States.INIT))
                .register(new SSLHandshakingState())
                .register(new SSLDataReadyState());
        //.register(new State(SessionState.CLOSE).register(closed))



        return sslSessionSM;
    }


    public static String rates() {
        return SUS.toCanonicalID(',', SSLHandshakingState.rcNeedWrap, SSLHandshakingState.rcNeedUnwrap, SSLHandshakingState.rcNeedTask, SSLHandshakingState.rcFinished, SSLDataReadyState.rcNotHandshaking);
    }

    public static <T> T lookupType(String type) {
        type = SharedStringUtil.toUpperCase(type);
        switch (type) {
            case "NEED_WRAP":
                return (T) SSLHandshakingState.rcNeedWrap;
            case "NEED_UNWRAP":
                return (T) SSLHandshakingState.rcNeedUnwrap;
            case "NEED_TASK":
                return (T) SSLHandshakingState.rcNeedTask;
            case "FINISHED":
                return (T) SSLHandshakingState.rcFinished;
            case "NOT_HANDSHAKING":
                return (T) SSLDataReadyState.rcNotHandshaking;
            case "SSL_CONNECTION_COUNT":
                return (T) Long.valueOf(counter.get());


        }
        return null;
    }

}
