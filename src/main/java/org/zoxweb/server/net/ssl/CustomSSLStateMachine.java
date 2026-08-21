package org.zoxweb.server.net.ssl;

import org.zoxweb.server.fsm.MonoStateMachine;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.Identifier;
import org.zoxweb.shared.util.RateCounter;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

import javax.net.ssl.SSLEngineResult;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

/**
 * The default {@link SSLConnectionHelper} dispatcher (META-SSL-ENGINE-DESIGN.md §2, §7): a plain
 * {@code HandshakeStatus} → handler table over {@link MonoStateMachine}, delegating each status
 * to the shared {@link SSLUtil} engine-step handlers and counting per-status rates. It invents no
 * protocol state — the {@code SSLEngine} owns the TLS state; {@code publish} <b>is</b> the loop:
 * each handler performs one engine step and re-publishes the status the engine returned, a
 * synchronous re-entrant chain on the calling worker thread.
 * <p>
 * Built with {@code MonoStateMachine(synchronous = false)} — publishes run unsynchronized on the
 * calling thread, because NIOSocket's key-interest gating already serializes each session on one
 * worker (the gate is the serialization mechanism, no lock needed).
 * <p>
 * One instance per session. The single constructor
 * {@link #CustomSSLStateMachine(SSLConfigInt, SSLHandshakeFinished)} <b>self-installs</b> this
 * dispatcher into the session's {@link SSLConfigInt} ({@code setSSLConnectionHelper(this)})
 * before any publish can occur — the invariant that lets {@code SSLUtil._finished} call
 * {@link #notifySSLHandshakeFinished()} unconditionally. The {@link SSLHandshakeFinished}
 * target passed in is where handshake completion lands:
 * <ul>
 * <li><b>server transport</b> ({@code SSLNIOSocketHandler}) — completion is the <b>tunnel
 * hook</b>: {@code _finished} → {@link #notifySSLHandshakeFinished()} →
 * {@code SSLNIOSocketHandler.sslHandshakeSuccessful()}, the lazy opener of a tunnel's remote
 * leg (SSL↔SSL / SSL↔PLAIN tunnel, HTTP CONNECT proxy). That chain is the hook's <b>only</b>
 * caller, and plain TLS termination never exercises it ({@code remoteConnection == null}), so
 * severing it is invisible to the test suite — do not.</li>
 * <li><b>plain client</b> ({@code TCPSessionCallback.sslUpgrade()} passes the callback
 * itself) — completion triggers {@code connectedFinished()}, the secure-connection-ready
 * notification. No tunnel on this path.</li>
 * </ul>
 * The full-FSM alternative ({@link SSLStateMachine} + engine states, selected by
 * {@code simpleStateMachine = false}) drives the same five handlers behind the
 * {@code org.zoxweb.server.fsm} framework.
 */
public class CustomSSLStateMachine extends MonoStateMachine<SSLEngineResult.HandshakeStatus, BaseSessionCallback<SSLConfigInt>>
        implements SSLConnectionHelper<SSLConfigInt>, Closeable, Identifier<Long> {
    public static final LogWrapper log = new LogWrapper(CustomSSLStateMachine.class).setEnabled(false);

    // per-status engine-step rates, process-wide across every session; see rates()/lookupType()
    static RateCounter rcNotHandshaking = new RateCounter("NotHandshaking");
    static RateCounter rcNeedWrap = new RateCounter("NeedWrap");
    static RateCounter rcNeedUnwrap = new RateCounter("NeedUnwrap");
    static RateCounter rcNeedTask = new RateCounter("NeedTask");
    static RateCounter rcFinished = new RateCounter("Finished");

    private static final AtomicLong counter = new AtomicLong();
    private final SSLConfigInt sslConfigInt;
    private final SSLHandshakeFinished sslHandshakeFinished;
    private final long id;

    /**
     * @return the total number of dispatchers created by this process (session counter)
     */
    public static long getIDCount() {
        return counter.get();
    }

    /**
     * Drives the engine of any {@link SSLConfigInt} session — server transport
     * ({@code SSLNIOSocketHandler.setupConnection}) and plain client
     * ({@code TCPSessionCallback.sslUpgrade()}) alike. Self-installs as the session's
     * {@link SSLConnectionHelper} before any publish can occur.
     *
     * @param sslConfigInt         the session state this dispatcher drives
     * @param sslHandshakeFinished where handshake completion lands (see class javadoc);
     *                             guarded — a null target makes completion a no-op
     */
    public CustomSSLStateMachine(SSLConfigInt sslConfigInt, SSLHandshakeFinished sslHandshakeFinished) {
        super(false);
        this.sslHandshakeFinished = sslHandshakeFinished;
        this.sslConfigInt = sslConfigInt;
        sslConfigInt.setSSLConnectionHelper(this);
        id = counter.incrementAndGet();
        register(NOT_HANDSHAKING, this::notHandshaking)
                .register(NEED_WRAP, this::needWrap)
                .register(NEED_UNWRAP, this::needUnwrap)
                .register(FINISHED, this::finished)
                .register(NEED_TASK, this::needTask)
        ;
    }

    /**
     * Closes the session state ({@link SSLConfigInt#close()}: close_notify drain, channels,
     * buffer recache).
     *
     * @throws IOException in case of error
     */
    @Override
    public void close() throws IOException {
        SharedIOUtil.close(getConfig());
    }

    /**
     * @return this dispatcher's creation-order id
     */
    public Long getID() {
        return id;
    }

    /** {@code NEED_WRAP}: one {@code wrap} + channel write via {@link SSLUtil#_needWrap}. */
    public void needWrap(BaseSessionCallback<SSLConfigInt> callback) {
        rcNeedWrap.register(SSLUtil._needWrap(getConfig(), callback));
    }

    /** {@code NEED_UNWRAP}: one channel read + {@code unwrap} via {@link SSLUtil#_needUnwrap}. */
    public void needUnwrap(BaseSessionCallback<SSLConfigInt> callback) {
        rcNeedUnwrap.register(SSLUtil._needUnwrap(getConfig(), callback));
    }

    /** {@code NEED_TASK}: runs the engine's delegated tasks inline via {@link SSLUtil#_needTask}. */
    public void needTask(BaseSessionCallback<SSLConfigInt> callback) {
        rcNeedTask.register(SSLUtil._needTask(getConfig(), callback));
    }

    /**
     * {@code FINISHED}: post-handshake hook via {@link SSLUtil#_finished} —
     * {@link #notifySSLHandshakeFinished()} (tunnel hook / client completion), then the
     * buffered-bytes drain chain into {@code NOT_HANDSHAKING} (delivery belongs to the
     * state transition).
     */
    public void finished(BaseSessionCallback<SSLConfigInt> callback) {
        rcFinished.register(SSLUtil._finished(getConfig(), callback));
    }

    /** {@code NOT_HANDSHAKING}: the post-handshake unwrap/deliver loop via {@link SSLUtil#_notHandshaking}. */
    public void notHandshaking(BaseSessionCallback<SSLConfigInt> callback) {
        rcNotHandshaking.register(SSLUtil._notHandshaking(getConfig(), callback));
    }

    /**
     * Invoked by {@code SSLUtil._finished} on handshake completion: delegates to the
     * {@link SSLHandshakeFinished} target supplied at construction — the tunnel hook on the
     * server path ({@code SSLNIOSocketHandler.sslHandshakeSuccessful()}, and this chain is
     * that method's only caller — see the class javadoc), {@code connectedFinished()} on the
     * plain-client path. Guarded no-op when the target is null.
     */
    @Override
    public void notifySSLHandshakeFinished() throws IOException {
        if(sslHandshakeFinished != null) {
            sslHandshakeFinished.sslHandshakeSuccessful(getConfig());
        }
    }

    /**
     * @return the session state this dispatcher drives, never null
     */
    public SSLConfigInt getConfig() {
        return sslConfigInt;
    }

    /**
     * @return the process-wide per-status engine-step rates as one line
     */
    public static String rates() {
        return SUS.toCanonicalID(',', rcNeedWrap, rcNeedUnwrap, rcNeedTask, rcFinished, rcNotHandshaking);
    }

    /**
     * Looks up one per-status {@link RateCounter} by status name (case-insensitive).
     *
     * @param type the status name, e.g. {@code "NEED_WRAP"}
     * @return the matching counter, or null for an unknown name
     */
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
                System.out.println("***************************************** : " + type);
        }
        return null;
    }

}
