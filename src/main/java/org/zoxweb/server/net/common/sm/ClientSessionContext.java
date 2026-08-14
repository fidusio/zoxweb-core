package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.ssl.SSLSessionConfig;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SUS;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-session mutable state of a {@link ClientConSM}, set as the machine's config so every
 * trigger consumer reaches it via {@code getStateMachine().getConfig()}.
 * <p>
 * Holds the session callback binding, the transport {@link Mode}, the SSL session state once an
 * upgrade ran, the declarative settings bag, and the phase chain that publishes the single
 * {@link ClientEvent#READY} when the last gating phase completes.
 */
public class ClientSessionContext {

    /**
     * Transport mode driving the {@link ClientTransportState} router.
     */
    public enum Mode {
        /** Wire bytes are application bytes; {@code RAW_IN_DATA} passes through as {@code IN_DATA}. */
        PLAIN,
        /** TLS upgrade in progress; wire bytes feed the SSL engine's inbound net buffer. */
        TLS_HANDSHAKING,
        /** Handshake done; wire bytes are ciphertext, decrypted data re-enters as {@code IN_DATA}. */
        TLS_SECURE,
    }

    private final ClientConSM sm;
    private final NVGenericMap settings;
    private final NVGenericMap vars = new NVGenericMap("vars");
    private final Set<String> pendingPhases = new LinkedHashSet<String>();
    private final AtomicBoolean readyPublished = new AtomicBoolean(false);

    private volatile TCPSMCallback session;
    private volatile Mode mode = Mode.PLAIN;
    private volatile SSLSessionConfig sslConfig;
    private volatile BaseSessionCallback<SSLSessionConfig> sslBridge;

    ClientSessionContext(ClientConSM sm, NVGenericMap settings, Set<String> gatingPhases) {
        this.sm = sm;
        this.settings = settings != null ? settings : new NVGenericMap();
        this.pendingPhases.addAll(gatingPhases);
    }

    /**
     * Binds the session callback; must run before the callback is handed to NIOSocket.
     *
     * @param cb the session callback whose config is this context's machine
     * @throws IllegalStateException    if already bound
     * @throws IllegalArgumentException if the callback is bound to a different machine
     */
    public synchronized void bind(TCPSMCallback cb) {
        SUS.checkIfNull("session callback null", cb);
        if (session != null)
            throw new IllegalStateException("context already bound to a session callback");
        if (cb.getConfig() != sm)
            throw new IllegalArgumentException("session callback belongs to a different state machine");
        session = cb;
    }

    /**
     * @return the bound session callback, null before {@link #bind(TCPSMCallback)}
     */
    public TCPSMCallback getSession() {
        return session;
    }

    public ClientConSM getStateMachine() {
        return sm;
    }

    /**
     * @return the declarative settings bag (never null; empty when built programmatically
     * without settings)
     */
    public NVGenericMap getSettings() {
        return settings;
    }

    /**
     * The caller-supplied variable bag resolving {@code ${name}} placeholders in {@code exchange}
     * {@code send}/{@code expect} literals — the mechanism that keeps a protocol config generic: it
     * declares <i>that</i> a value is injected (e.g. the client HELO name), the caller provides the
     * value here before the connection is created. Seeded from a config {@code vars} block, if any.
     *
     * @return the variable bag (never null)
     */
    public NVGenericMap getVars() {
        return vars;
    }

    /**
     * Sets an {@code exchange} variable used to resolve {@code ${name}} placeholders. Must be called
     * before the connection is created (i.e. before the dialogue runs).
     *
     * @param name  the variable name (matches {@code ${name}} in a literal)
     * @param value the value substituted in
     * @return this context, for chaining
     */
    public ClientSessionContext setVar(String name, String value) {
        vars.build(name, value);
        return this;
    }

    public Mode getMode() {
        return mode;
    }

    void setMode(Mode mode) {
        this.mode = mode;
    }

    public boolean isSecure() {
        return mode == Mode.TLS_SECURE;
    }

    /**
     * @return the SSL session state, null until an upgrade ran
     */
    public SSLSessionConfig getSSLConfig() {
        return sslConfig;
    }

    void setSSLConfig(SSLSessionConfig sslConfig) {
        this.sslConfig = sslConfig;
    }

    /**
     * @return the handler-facing SSL callback bridge, null until an upgrade ran
     */
    public BaseSessionCallback<SSLSessionConfig> getSSLBridge() {
        return sslBridge;
    }

    void setSSLBridge(BaseSessionCallback<SSLSessionConfig> sslBridge) {
        this.sslBridge = sslBridge;
    }

    /**
     * One-shot complete write of the buffer through the session output stream — plaintext before
     * the upgrade, encrypted after {@link ClientEvent#SECURE}.
     *
     * @param bb payload in read-mode (e.g. from {@link ByteBuffer#wrap(byte[])})
     * @return bytes transmitted to the channel
     * @throws IOException on channel or SSL error, or when invoked mid-handshake — the output
     *                     stream encrypts only after the handshake completes, so a write during
     *                     {@link Mode#TLS_HANDSHAKING} would leave in plaintext and corrupt the
     *                     handshake byte stream; it is rejected instead
     */
    public int write(ByteBuffer bb) throws IOException {
        if (mode == Mode.TLS_HANDSHAKING)
            throw new IOException("TLS handshake in progress: writes are gated until SECURE");
        return session.get().write(bb, false);
    }

    /**
     * Fatal session termination: routes the cause through the session's exception path, so
     * teardown publishes {@code CLOSED} with the Throwable as payload.
     *
     * @param t the terminating cause
     */
    public void fail(Throwable t) {
        TCPSMCallback s = session;
        if (s != null)
            s.exception(t);
    }

    /**
     * Reports a gating phase as finished; when the last one completes, publishes the single
     * {@link ClientEvent#READY}. Unknown or repeated names are no-ops, as is completion after
     * the machine closed (a consumer may have failed the session inline during the completing
     * dispatch — publishing READY on the closed machine would throw instead of tearing down
     * cleanly).
     *
     * @param phaseName the completed phase's name
     */
    public void phaseComplete(String phaseName) {
        boolean empty;
        synchronized (this) {
            pendingPhases.remove(phaseName);
            empty = pendingPhases.isEmpty();
        }
        if (empty && !readyPublished.getAndSet(true) && !sm.isClosed())
            sm.publishSync(ClientEvent.READY, null);
    }
}
