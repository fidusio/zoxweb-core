package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.ssl.SSLContextInfo;
import org.zoxweb.server.net.ssl.SSLSessionConfig;
import org.zoxweb.shared.util.NVBoolean;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NamedValue;
import org.zoxweb.shared.util.SUS;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;

/**
 * Catalog state {@code ssl} — the machine owns the handshake orchestration
 * (SSLStateMachineV2): its {@link ClientEvent#START_TLS} consumer performs the upgrade, and in
 * {@link TLSMode#IMMEDIATE} an auto-start consumer publishes {@code START_TLS} on
 * {@code CONNECTED}. The engine states ({@link SSLClientHandshakeState},
 * {@link SSLClientDataState}) are registered alongside this state by the builder — the
 * untouchable {@code SSLUtil} handlers do the engine steps (zero changes to {@code net.ssl}).
 * <ul>
 * <li>{@link TLSMode#IMMEDIATE} — the upgrade auto-starts on {@code CONNECTED}
 * (HTTPS/SMTPS-style client); the state gates {@link ClientEvent#READY} ({@code ready_gate}
 * bag flag), completed by the bridge on handshake success.</li>
 * <li>{@link TLSMode#ON_DEMAND} — STARTTLS-ready: nothing auto-starts; the controller (or a
 * custom negotiator state) publishes {@code START_TLS} after its go-ahead, having verified
 * that <b>no residue followed the go-ahead</b> (residue is attacker-controllable plaintext —
 * fatal, never clear-and-continue). Does not gate {@code READY}.</li>
 * </ul>
 * <b>Configuration is the state's properties bag</b>: {@code mode}
 * ({@code immediate}/{@code on_demand}), {@code cert_validation} (chain validation <b>and</b>
 * hostname verification — the two always move together; default true),
 * {@code endpoint_identification}, and optionally an explicit {@code ssl_context}
 * ({@code NamedValue<SSLContextInfo>}) for custom trust stores / client certificates.
 * <p>
 * The upgrade always calls {@code beginHandshake(null, null)} — fresh pooled buffers, so
 * pre-upgrade plaintext can never become handshake input — and leaves
 * {@code SSLSessionConfig.sslOutputStream} null: the session's one output stream is flipped to
 * encrypted writes by the bridge on handshake completion.
 * <p>
 * <b>Endpoint binding.</b> The protocol configuration carries no remote address. The deferred
 * path (no {@code ssl_context} in the bag) builds the {@code SSLContextInfo} at upgrade time
 * from the connected socket's actual remote address, so SNI and hostname verification pin to
 * the real peer the caller dialed.
 */
public class SSLClientState extends State<Object> {

    public static final LogWrapper log = new LogWrapper(SSLClientState.class).setEnabled(false);
    public static final String NAME = "ssl";

    // bag keys
    static final String MODE = "mode";
    static final String CERT_VALIDATION = "cert_validation";
    static final String ENDPOINT_IDENTIFICATION = "endpoint_identification";
    static final String SSL_CONTEXT = "ssl_context";

    /**
     * When the TLS upgrade starts.
     */
    public enum TLSMode {
        /** Handshake right after connect. */
        IMMEDIATE,
        /** STARTTLS-ready: upgrade on {@link ClientEvent#START_TLS} from a negotiator. */
        ON_DEMAND,
    }

    /**
     * Deferred endpoint-bound state (the {@link ClientSMFactory} path): the protocol config
     * carries no address, so the {@code SSLContextInfo} is built at upgrade time from the
     * connected socket's remote address. Endpoint identification (hostname verification)
     * tracks {@code certValidation}: chain validation alone accepts any CA-valid certificate
     * for <b>any</b> host (MITM), only hostname verification pins it to the dialed peer; a
     * trust-all state ({@code certValidation} false) skips both.
     *
     * @param mode           when the upgrade starts
     * @param certValidation true to validate the server chain against the JVM trust store AND
     *                       verify the certificate identity against the connected host; false
     *                       to trust any certificate (self-signed / negotiation checks)
     */
    public SSLClientState(TLSMode mode, boolean certValidation) {
        this(null, mode, certValidation, certValidation);
    }

    /**
     * Programmatic state with an explicit context and no endpoint identification — for
     * trust-all/test contexts, or a custom {@code SSLContext}. Production configurations
     * validating certificates must use {@link #SSLClientState(TLSMode, boolean)} or the
     * three-argument constructor with {@code endpointIdentification = true}: chain validation
     * alone accepts any CA-valid certificate for <b>any</b> host (MITM).
     *
     * @param sslContextInfo client-mode context info (built with the remote address, so
     *                       {@code SSLSessionConfig.isClientMode()} holds — required by the
     *                       {@code _finished} client notification)
     * @param mode           when the upgrade starts
     */
    public SSLClientState(SSLContextInfo sslContextInfo, TLSMode mode) {
        this(sslContextInfo, mode, false, false);
        SUS.checkIfNulls("sslContextInfo null", sslContextInfo);
    }

    /**
     * @param sslContextInfo         explicit client-mode context info
     * @param mode                   when the upgrade starts
     * @param endpointIdentification true to enable HTTPS-style hostname verification against
     *                               the peer certificate (required whenever certificates are
     *                               validated; must be false with a trust-all context)
     */
    public SSLClientState(SSLContextInfo sslContextInfo, TLSMode mode, boolean endpointIdentification) {
        this(sslContextInfo, mode, false, endpointIdentification);
        SUS.checkIfNulls("sslContextInfo null", sslContextInfo);
    }

    private SSLClientState(SSLContextInfo sslContextInfo, TLSMode mode, boolean certValidation,
                           boolean endpointIdentification) {
        super(NAME);
        SUS.checkIfNulls("mode null", mode);
        if (sslContextInfo != null && !sslContextInfo.isClient())
            throw new IllegalArgumentException("SSLContextInfo is not client mode");
        NVGenericMap bag = getProperties();
        bag.build(MODE, mode.name())
                .build(new NVBoolean(CERT_VALIDATION, certValidation))
                .build(new NVBoolean(ENDPOINT_IDENTIFICATION, endpointIdentification))
                .build(new NVBoolean("ready_gate", mode == TLSMode.IMMEDIATE));
        if (sslContextInfo != null)
            bag.add(new NamedValue<SSLContextInfo>(SSL_CONTEXT, sslContextInfo));
        register(new StartTLS());
        if (mode == TLSMode.IMMEDIATE)
            register(new AutoStart());
    }

    public TLSMode getMode() {
        return TLSMode.valueOf(SMProtoUtil.stringValue(getProperties(), MODE, TLSMode.ON_DEMAND.name()));
    }

    private SSLContextInfo explicitContext() {
        NamedValue<SSLContextInfo> nv = getProperties().getNV(SSL_CONTEXT);
        return nv != null ? nv.getValue() : null;
    }

    /**
     * The upgrade sequence (idempotent): builds the SSL session state, wires the helper and the
     * bridge, flips the router to TLS mode and kicks the client handshake flight inline.
     */
    void upgrade(ClientConSM sm) {
        ClientSessionContext ctx = sm.getContext();
        if (ctx.getSSLConfig() != null || ctx.getMode() != ClientSessionContext.Mode.PLAIN)
            return;

        boolean certValidation = SMProtoUtil.booleanValue(getProperties(), CERT_VALIDATION, true);
        boolean endpointIdentification = SMProtoUtil.booleanValue(getProperties(), ENDPOINT_IDENTIFICATION, certValidation);
        SocketChannel channel = (SocketChannel) ctx.getSession().getChannel();
        SSLContextInfo sci;
        try {
            // deferred path: bind the TLS context to the endpoint the session actually connected
            // to (SNI + hostname verification pin to the real peer); explicit-context path uses
            // the caller-supplied context as-is
            SSLContextInfo explicit = explicitContext();
            sci = explicit != null
                    ? explicit
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
        // drain sees a dead channel and cannot spin
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
            super(ClientEvent.CONNECTED);
        }

        @Override
        public void accept(SelectionKey key) {
            publishSync(ClientEvent.START_TLS, null);
        }
    }
}
