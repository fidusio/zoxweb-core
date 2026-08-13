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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

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

    private final SSLContextInfo sslContextInfo;
    private final TLSMode mode;
    private final boolean endpointIdentification;

    /**
     * Phase without endpoint identification — for trust-all/test contexts. Production
     * configurations validating certificates must use the three-argument constructor with
     * {@code endpointIdentification = true}: chain validation alone accepts any CA-valid
     * certificate for <b>any</b> host (MITM), only hostname verification pins it to the peer.
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
    public void contribute(ClientConnectionSM sm) {
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
    void upgrade(ClientConnectionSM sm) {
        ClientSessionContext ctx = sm.getContext();
        if (ctx.getSSLConfig() != null || ctx.getMode() != ClientSessionContext.Mode.PLAIN)
            return;

        SSLSessionConfig cfg = new SSLSessionConfig(sslContextInfo);
        cfg.sslChannel = (SocketChannel) ctx.getSession().getChannel();
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
            upgrade((ClientConnectionSM) getStateMachine());
        }
    }

    private class AutoStart extends TriggerConsumer<SelectionKey> {
        AutoStart() {
            super(TCPSMCallback.BasicEvent.CONNECTED);
        }

        @Override
        public void accept(SelectionKey key) {
            publishSync(ClientEvent.START_TLS, null);
        }
    }
}
