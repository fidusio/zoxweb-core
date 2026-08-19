package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.net.BaseSessionCallback;
import org.zoxweb.server.net.ssl.SSLConfigInt;
import org.zoxweb.server.net.ssl.SSLSessionConfig;
import org.zoxweb.shared.util.NVBoolean;
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
 * upgrade ran, the declarative settings bag, and the READY gate that publishes the single
 * {@link CommonTrigger#READY} when the last gating state completes.
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

    /**
     * The machine's transport, fixed at build time ({@link ClientConSMBuilder#transport}); selects
     * the transport router state and which session callback type the context accepts.
     */
    public enum Transport {
        /** Stream session driven by a {@link TCPSMCallback}; supports the ssl state. */
        TCP,
        /** Datagram session driven by a {@link UDPSMCallback}; always plaintext (no DTLS). */
        UDP,
    }

    private final ClientConSM sm;
    private final NVGenericMap settings;
    private final NVGenericMap vars = new NVGenericMap("vars");
    private final Set<String> pendingGates = new LinkedHashSet<String>();
    private final AtomicBoolean readyPublished = new AtomicBoolean(false);
    private final Transport transport;

    private volatile TCPSMCallback session;
    private volatile UDPSMCallback udpSession;
    private volatile Mode mode = Mode.PLAIN;
    private volatile SSLSessionConfig sslConfig;
    private volatile BaseSessionCallback<SSLConfigInt> sslBridge;

    ClientSessionContext(ClientConSM sm, NVGenericMap settings, Set<String> readyGates, Transport transport) {
        this.sm = sm;
        this.settings = settings != null ? settings : new NVGenericMap();
        this.pendingGates.addAll(readyGates);
        this.transport = transport;
    }

    /**
     * @return the machine's transport, fixed at build time
     */
    public Transport getTransport() {
        return transport;
    }

    /**
     * Binds the TCP session callback; must run before the callback is handed to NIOSocket.
     *
     * @param cb the session callback whose config is this context's machine
     * @throws IllegalStateException    if already bound, or the machine transport is not TCP
     * @throws IllegalArgumentException if the callback is bound to a different machine
     */
    public synchronized void bind(TCPSMCallback cb) {
        SUS.checkIfNull("session callback null", cb);
        if (transport != Transport.TCP)
            throw new IllegalStateException("machine transport is " + transport + ": TCP session callback rejected");
        if (session != null || udpSession != null)
            throw new IllegalStateException("context already bound to a session callback");
        if (cb.getConfig() != sm)
            throw new IllegalArgumentException("session callback belongs to a different state machine");
        session = cb;
    }

    /**
     * Binds the UDP session callback; must run before the callback is handed to NIOSocket.
     *
     * @param cb the session callback whose config is this context's machine
     * @throws IllegalStateException    if already bound, or the machine transport is not UDP
     * @throws IllegalArgumentException if the callback is bound to a different machine
     */
    public synchronized void bind(UDPSMCallback cb) {
        SUS.checkIfNull("session callback null", cb);
        if (transport != Transport.UDP)
            throw new IllegalStateException("machine transport is " + transport + ": UDP session callback rejected");
        if (session != null || udpSession != null)
            throw new IllegalStateException("context already bound to a session callback");
        if (cb.getConfig() != sm)
            throw new IllegalArgumentException("session callback belongs to a different state machine");
        udpSession = cb;
    }

    /**
     * @return the bound TCP session callback, null before {@link #bind(TCPSMCallback)} (always
     * null on a UDP-transport machine)
     */
    public TCPSMCallback getSession() {
        return session;
    }

    /**
     * @return the bound UDP session callback, null before {@link #bind(UDPSMCallback)} (always
     * null on a TCP-transport machine)
     */
    public UDPSMCallback getUDPSession() {
        return udpSession;
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
    public BaseSessionCallback<SSLConfigInt> getSSLBridge() {
        return sslBridge;
    }

    void setSSLBridge(BaseSessionCallback<SSLConfigInt> sslBridge) {
        this.sslBridge = sslBridge;
    }

    /**
     * One-shot complete write of the buffer to the peer — over TCP through the session output
     * stream (plaintext before the upgrade, encrypted after {@link CommonTrigger#SECURE}), over UDP
     * as one datagram to the connected remote.
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
        UDPSMCallback udp = udpSession;
        return udp != null ? udp.send(bb, false) : session.get().write(bb, false);
    }

    /**
     * Fatal session termination: routes the cause through the session's exception path, so
     * teardown publishes {@code CLOSED} with the Throwable as payload.
     *
     * @param t the terminating cause
     */
    public void fail(Throwable t) {
        TCPSMCallback s = session;
        if (s != null) {
            s.exception(t);
            return;
        }
        UDPSMCallback u = udpSession;
        if (u != null)
            u.exception(t);
    }

    /**
     * Reports a READY-gating state as finished; when the last gate completes, publishes the
     * single {@link CommonTrigger#READY}. Unknown or repeated names are no-ops, as is completion
     * after the machine closed (a consumer may have failed the session inline during the
     * completing dispatch — publishing READY on the closed machine would throw instead of
     * tearing down cleanly).
     *
     * @param gateName the completed gating state's name
     */
    public void gateComplete(String gateName) {
        boolean empty;
        synchronized (this) {
            pendingGates.remove(gateName);
            empty = pendingGates.isEmpty();
        }
        if (empty && !readyPublished.getAndSet(true) && !sm.isClosed()) {
            // record the completed pipeline in the machine's results bag before the broadcast
            SMProtoUtil.results(sm).build(new NVBoolean("ready", true));
            sm.publishSync(CommonTrigger.READY, null);
        }
    }
}
