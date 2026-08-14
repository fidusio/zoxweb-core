package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.server.net.ssl.SSLSessionConfig;
import org.zoxweb.shared.util.SUS;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;

/**
 * Client TLS phase — the machine owns the handshake orchestration (SSLStateMachineV2):
 * registers {@link SSLClientHandshakeState}, {@link SSLClientDataState} and a control state
 * whose {@link ClientEvent#START_TLS} consumer performs the upgrade.
 * <ul>
 * <li>{@link TLSMode#IMMEDIATE} — the upgrade auto-starts on {@code CONNECTED}
 * (HTTPS/SMTPS-style client); the phase gates {@link ClientEvent#READY}.</li>
 * <li>{@link TLSMode#ON_DEMAND} — STARTTLS-ready: the states are registered but nothing
 * auto-starts; a protocol negotiator publishes {@code START_TLS} after its go-ahead, having
 * verified that <b>no residue followed the go-ahead line</b> (residue is attacker-controllable
 * plaintext — fatal, never clear-and-continue). The phase does not gate {@code READY}.</li>
 * </ul>
 * The upgrade always calls {@code beginHandshake(null, null)} — fresh pooled buffers, so
 * pre-upgrade plaintext can never become handshake input — and leaves
 * {@code SSLSessionConfig.sslOutputStream} null: the session's one output stream (TCPSMCallback's
 * {@code bcos}) is flipped to encrypted writes by the bridge on handshake completion.
 * <p>
 * <b>Endpoint binding.</b> The protocol configuration carries no remote address — the endpoint is
 * whatever the session connected to. The {@link ClientSMFactory} path builds the phase deferred
 * ({@link #SSLClientPhase(TLSMode, boolean)}): the {@code SSLContextInfo} is created at upgrade
 * time from the connected socket's actual remote address, so SNI and hostname verification pin to
 * the real peer the caller dialed. The programmatic constructors taking an explicit
 * {@code SSLContextInfo} remain for advanced use (a custom {@code SSLContext}: private trust
 * store, client certificates, named groups) where the address is already baked into the context.
 */
public class SSLClientPhase implements ConnectionPhase {

    public static final LogWrapper log = new LogWrapper(SSLClientPhase.class).setEnabled(false);
    public static final String NAME = "ssl";

    /**
     * When the TLS upgrade starts.
     */
    public enum TLSMode {
        /** Handshake right after connect. */
        IMMEDIATE,
        /** STARTTLS-ready: upgrade on {@link ClientEvent#START_TLS} from a negotiator. */
        ON_DEMAND,
    }

    /** Non-null only for the programmatic explicit-context path; null = deferred (built at upgrade). */
    private final SSLContextInfo sslContextInfo;
    private final TLSMode mode;
    private final boolean certValidation;
    private final boolean endpointIdentification;

    /**
     * Deferred endpoint-bound phase (the {@link ClientSMFactory} path): the protocol config carries
     * no address, so the {@code SSLContextInfo} is built at upgrade time from the connected socket's
     * remote address. Endpoint identification (hostname verification) tracks {@code certValidation}:
     * chain validation alone accepts any CA-valid certificate for <b>any</b> host (MITM), only
     * hostname verification pins it to the dialed peer; a trust-all phase ({@code certValidation}
     * false) skips both.
     *
     * @param mode           when the upgrade starts
     * @param certValidation true to validate the server chain against the JVM trust store AND
     *                       verify the certificate identity against the connected host; false to
     *                       trust any certificate (self-signed / fingerprinting)
     */
    public SSLClientPhase(TLSMode mode, boolean certValidation) {
        SUS.checkIfNulls("mode null", mode);
        this.sslContextInfo = null;
        this.mode = mode;
        this.certValidation = certValidation;
        this.endpointIdentification = certValidation;
    }

    /**
     * Programmatic phase with an explicit context and no endpoint identification — for
     * trust-all/test contexts, or a custom {@code SSLContext}. Production configurations validating
     * certificates must use the three-argument constructor with {@code endpointIdentification = true}
     * (or the deferred {@link #SSLClientPhase(TLSMode, boolean)}): chain validation alone accepts any
     * CA-valid certificate for <b>any</b> host (MITM).
     *
     * @param sslContextInfo client-mode context info (built with the remote address, so
     *                       {@code SSLSessionConfig.isClientMode()} holds — required by the
     *                       {@code _finished} client notification)
     * @param mode           when the upgrade starts
     */
    public SSLClientPhase(SSLContextInfo sslContextInfo, TLSMode mode) {
        this(sslContextInfo, mode, false);
    }

    /**
     * @param sslContextInfo         client-mode context info (built with the remote address, so
     *                               {@code SSLSessionConfig.isClientMode()} holds — required by
     *                               the {@code _finished} client notification)
     * @param mode                   when the upgrade starts
     * @param endpointIdentification true to enable HTTPS-style hostname verification against
     *                               the peer certificate (required whenever certificates are
     *                               validated; must be false with a trust-all context, whose
     *                               wrapped trust manager would otherwise still verify the
     *                               hostname)
     */
    public SSLClientPhase(SSLContextInfo sslContextInfo, TLSMode mode, boolean endpointIdentification) {
        SUS.checkIfNulls("sslContextInfo or mode null", sslContextInfo, mode);
        if (!sslContextInfo.isClient())
            throw new IllegalArgumentException("SSLContextInfo is not client mode");
        this.sslContextInfo = sslContextInfo;
        this.mode = mode;
        this.certValidation = false;
        this.endpointIdentification = endpointIdentification;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean gatesReady() {
        return mode == TLSMode.IMMEDIATE;
    }

    public TLSMode getMode() {
        return mode;
    }

    @Override
    public void contribute(ClientConSM sm) {
        sm.register(new SSLClientHandshakeState());
        sm.register(new SSLClientDataState());
        State<Object> control = new State<Object>("ssl-control");
        control.register(new StartTLS());
        if (mode == TLSMode.IMMEDIATE)
            control.register(new AutoStart());
        sm.register(control);
    }

    /**
     * The upgrade sequence (idempotent): builds the SSL session state, wires the helper and the
     * bridge, flips the router to TLS mode and kicks the client handshake flight inline.
     */
    void upgrade(ClientConSM sm) {
        ClientSessionContext ctx = sm.getContext();
        if (ctx.getSSLConfig() != null || ctx.getMode() != ClientSessionContext.Mode.PLAIN)
            return;

        SocketChannel channel = (SocketChannel) ctx.getSession().getChannel();
        SSLContextInfo sci;
        try {
            // deferred path: bind the TLS context to the endpoint the session actually connected
            // to (SNI + hostname verification pin to the real peer); explicit-context path uses
            // the caller-supplied context as-is
            sci = sslContextInfo != null
                    ? sslContextInfo
                    : new SSLContextInfo((InetSocketAddress) channel.getRemoteAddress(), certValidation);
        } catch (java.io.IOException | GeneralSecurityException e) {
            ctx.fail(e);
            return;
        }

        SSLSessionConfig cfg = new SSLSessionConfig(sci);
        cfg.sslChannel = channel;
        cfg.sslConnectionHelper = new ClientSSLHelper(sm);
        if (endpointIdentification) {
            // chain validation alone accepts any CA-valid cert for any host; HTTPS endpoint
            // identification pins the peer certificate to the configured hostname (MITM guard)
            SSLEngine engine = cfg.getSSLEngine();
            SSLParameters params = engine.getSSLParameters();
            params.setEndpointIdentificationAlgorithm("HTTPS");
            engine.setSSLParameters(params);
        }
        SSLClientBridge bridge = new SSLClientBridge(sm);
        bridge.setConfig(cfg);
        ctx.setSSLConfig(cfg);
        ctx.setSSLBridge(bridge);
        ctx.setMode(ClientSessionContext.Mode.TLS_HANDSHAKING);
        // teardown must close the SSL session state even when the session dies mid-handshake
        // (before the output stream ever learns about it): closes the engine outbound and
        // recaches the pooled net/app buffers — registered AFTER the socket, so the close
        // drain sees a closed channel and cannot spin
        ctx.getSession().registerAutoCloseable(cfg);
        try {
            // always null,null: fresh pooled buffers, pre-upgrade residue can never enter the session
            cfg.beginHandshake(null, null);
        } catch (SSLException e) {
            ctx.fail(e);
            return;
        }
        if (log.isEnabled()) log.getLogger().info(sm.getName() + " TLS upgrade, first status: " + cfg.getHandshakeStatus());
        // first client status = NEED_WRAP (ClientHello); the whole flight runs inline
        cfg.sslConnectionHelper.publish(cfg.getHandshakeStatus(), bridge);
    }

    private class StartTLS extends TriggerConsumer<Object> {
        StartTLS() {
            super(ClientEvent.START_TLS);
        }

        @Override
        public void accept(Object ignored) {
            upgrade((ClientConSM) getStateMachine());
        }
    }

    private class AutoStart extends TriggerConsumer<SelectionKey> {
        AutoStart() {
            super(SMProtoUtil.BasicEvent.CONNECTED);
        }

        @Override
        public void accept(SelectionKey key) {
            publishSync(ClientEvent.START_TLS, null);
        }
    }
}
