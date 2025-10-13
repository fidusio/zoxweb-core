package org.zoxweb.server.net.ssl;

import org.zoxweb.server.fsm.MonoStateMachine;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.util.Identifier;
import org.zoxweb.shared.util.RateCounter;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

import javax.net.ssl.SSLEngineResult;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

class CustomSSLStateMachine extends MonoStateMachine<SSLEngineResult.HandshakeStatus, SSLSessionCallback>
        implements SSLConnectionHelper, Closeable, Identifier<Long> {
    public static final LogWrapper log = new LogWrapper(CustomSSLStateMachine.class).setEnabled(false);

    static RateCounter rcNotHandshaking = new RateCounter("NotHandshaking");
    static RateCounter rcNeedWrap = new RateCounter("NeedWrap");
    static RateCounter rcNeedUnwrap = new RateCounter("NeedUnwrap");
    static RateCounter rcNeedTask = new RateCounter("NeedTask");
    static RateCounter rcFinished = new RateCounter("Finished");

    private static final AtomicLong counter = new AtomicLong();
    private final SSLNIOSocketHandler sslns;
    private final long id;

    public static long getIDCount() {
        return counter.get();
    }

    CustomSSLStateMachine(SSLNIOSocketHandler sslns) {
        super(false);
        this.sslns = sslns;
        sslns.getConfig().sslConnectionHelper = this;
        id = counter.incrementAndGet();
        register(NOT_HANDSHAKING, this::notHandshaking)
                .register(NEED_WRAP, this::needWrap)
                .register(NEED_UNWRAP, this::needUnwrap)
                .register(FINISHED, this::finished)
                .register(NEED_TASK, this::needTask)
        ;
    }

    @Override
    public void close() throws IOException {
        getConfig().close();
    }

    public Long getID() {
        return id;
    }

    public void needWrap(SSLSessionCallback callback) {
        rcNeedWrap.register(SSLUtil._needWrap(getConfig(), callback));
    }

    public void needUnwrap(SSLSessionCallback callback) {
        rcNeedUnwrap.register(SSLUtil._needUnwrap(getConfig(), callback));
    }

    public void needTask(SSLSessionCallback callback) {
        rcNeedTask.register(SSLUtil._needTask(getConfig(), callback));
    }

    public void finished(SSLSessionCallback callback) {
        rcFinished.register(SSLUtil._finished(getConfig(), callback));
    }

    public void notHandshaking(SSLSessionCallback callback) {
        rcNotHandshaking.register(SSLUtil._notHandshaking(getConfig(), callback));
    }

    @Override
    public void createRemoteConnection() {
        sslns.createRemoteConnection();
    }

    public SSLSessionConfig getConfig() {
        return sslns.getConfig();
    }

    public static String rates() {
        return SharedUtil.toCanonicalID(',', rcNeedWrap, rcNeedUnwrap, rcNeedTask, rcFinished, rcNotHandshaking);
    }

    public static <T> T lookupType(String type) {
        type = SharedStringUtil.toUpperCase(type);
        switch (type) {
            case "NEED_WRAP":
                return (T) rcNeedWrap;
            case "NEED_UNWRAP":
                return (T) rcNeedUnwrap;
            case "NEED_TASK":
                return (T) rcNeedTask;
            case "FINISHED":
                return (T) rcFinished;
            case "NOT_HANDSHAKING":
                return (T) rcNotHandshaking;
            default:
                System.out.println("***************************************** SHIT: " + type);
        }
        return null;
    }

}
