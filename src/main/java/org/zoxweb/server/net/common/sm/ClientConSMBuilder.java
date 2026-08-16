package org.zoxweb.server.net.common.sm;

import org.zoxweb.server.fsm.State;
import org.zoxweb.shared.io.SharedIOUtil;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SUS;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Programmatic configuration of a {@link ClientConSM}: compose {@link ConnectionPhase}s,
 * then {@link #build()}.
 * <pre>
 * ClientConSM sm = ClientConSMBuilder.create("ssh-fingerprint")
 *         .phase(new SSHBannerPhase("SSH-2.0-", "OpenSSH"))
 *         .build();
 * TCPSMCallback cb = sm.newSessionCallback();
 * nioSocket.addClientSocket(remote, cb, 5, null);
 * </pre>
 * The builder registers {@link ClientTransportState} first, then phases in declared order
 * (broadcast order = registration order), and seeds the context's phase chain with the
 * transport plus every phase whose {@link ConnectionPhase#gatesReady()} is true — the last of
 * them to complete publishes the single {@link ClientEvent#READY}.
 */
public class ClientConSMBuilder {

    private final String name;
    private final List<ConnectionPhase> phases = new ArrayList<ConnectionPhase>();
    private NVGenericMap settings;
    private ClientSessionContext.Transport transport = ClientSessionContext.Transport.TCP;
    private boolean closeOnReady = false;

    private ClientConSMBuilder(String name) {
        this.name = name;
    }

    public static ClientConSMBuilder create(String name) {
        SUS.checkIfNull("name null", name);
        return new ClientConSMBuilder(name);
    }

    /**
     * Attaches the declarative settings bag, exposed to phases via
     * {@link ClientSessionContext#getSettings()}.
     */
    public ClientConSMBuilder settings(NVGenericMap settings) {
        this.settings = settings;
        return this;
    }

    /**
     * Selects the machine's transport (default {@link ClientSessionContext.Transport#TCP}):
     * decides the transport router state registered first and which session callback type the
     * machine mints. UDP machines are always plaintext — {@link #build()} rejects TLS and
     * stream-only phases on them.
     */
    public ClientConSMBuilder transport(ClientSessionContext.Transport transport) {
        SUS.checkIfNull("transport null", transport);
        this.transport = transport;
        return this;
    }

    /**
     * Registers a machine action that closes the session the moment {@link ClientEvent#READY}
     * publishes — the machine itself ends the session when its configured pipeline completes
     * (probe shape: connect, run the dialogue, validate, close; the whole lifecycle is machine
     * dictated, no external driver needed). Default false: an interactive client keeps the
     * session open for the application after READY.
     */
    public ClientConSMBuilder closeOnReady(boolean closeOnReady) {
        this.closeOnReady = closeOnReady;
        return this;
    }

    /**
     * Appends a phase; phases contribute their states in declared order.
     *
     * @throws IllegalArgumentException on a second SSL phase — one TLS session per connection —
     *                                  or a duplicate phase name: same-named gating phases would
     *                                  collapse to one entry in the READY gate (premature READY)
     *                                  and their consumers would double-consume shared events;
     *                                  on a banner phase combined with an exchange phase (both
     *                                  would consume IN_DATA — one active owner per buffer); or
     *                                  on an IMMEDIATE SSL phase declared after an exchange phase
     *                                  (broadcast order is declaration order, so the dialogue's
     *                                  CONNECTED consumer would run before the TLS auto-start and
     *                                  send its first bytes in plaintext into the handshake)
     */
    public ClientConSMBuilder phase(ConnectionPhase phase) {
        SUS.checkIfNull("phase null", phase);
        for (ConnectionPhase p : phases) {
            if (phase instanceof SSLClientPhase && p instanceof SSLClientPhase)
                throw new IllegalArgumentException("at most one SSL phase per machine");
            if (p.getName().equals(phase.getName()))
                throw new IllegalArgumentException("duplicate phase name: " + phase.getName());
            if ((phase instanceof SSHBannerPhase && p instanceof DataExchangePhase)
                    || (phase instanceof DataExchangePhase && p instanceof SSHBannerPhase))
                throw new IllegalArgumentException(
                        "banner and exchange phases on one machine: both would consume IN_DATA (one active owner per buffer)");
            if (phase instanceof SSLClientPhase && p instanceof DataExchangePhase
                    && ((SSLClientPhase) phase).getMode() == SSLClientPhase.TLSMode.IMMEDIATE)
                throw new IllegalArgumentException(
                        "IMMEDIATE SSL phase declared after the exchange phase: the dialogue would start" +
                        " in plaintext before the TLS auto-start; declare the SSL phase first");
        }
        phases.add(phase);
        return this;
    }

    /**
     * Builds the synchronous machine: context wired, transport router state registered first
     * (TCP or UDP per {@link #transport}), phases contributed in order.
     *
     * @throws IllegalArgumentException on a UDP transport combined with a phase that requires a
     *                                  stream: an SSL phase (no DTLS in this stack), an SSH banner
     *                                  phase, or an exchange script with a {@code start_tls} step
     */
    public ClientConSM build() {
        boolean udp = transport == ClientSessionContext.Transport.UDP;
        if (udp) {
            for (ConnectionPhase p : phases) {
                if (p instanceof SSLClientPhase)
                    throw new IllegalArgumentException("TLS phase over UDP transport: no DTLS in this stack");
                if (p instanceof SSHBannerPhase)
                    throw new IllegalArgumentException("SSH banner phase over UDP transport: SSH requires a stream");
                if (p instanceof DataExchangePhase && ((DataExchangePhase) p).hasStartTLS())
                    throw new IllegalArgumentException("start_tls exchange step over UDP transport: no DTLS in this stack");
            }
        }
        Set<String> gating = new LinkedHashSet<String>();
        gating.add(udp ? UDPClientTransportState.NAME : ClientTransportState.NAME);
        for (ConnectionPhase p : phases) {
            if (p.gatesReady())
                gating.add(p.getName());
        }
        ClientConSM sm = new ClientConSM(name);
        ClientSessionContext ctx = new ClientSessionContext(sm, settings, gating, transport);
        sm.setConfig(ctx);
        // machine-wide result accumulation bag: every state and TriggerConsumer writes its
        // outcome here via SMProtoUtil.results(sm), the caller reads it after CLOSED
        SMProtoUtil.results(sm);
        if (udp)
            sm.register(new UDPClientTransportState());
        else
            sm.register(new ClientTransportState());
        for (ConnectionPhase p : phases)
            p.contribute(sm);
        if (closeOnReady) {
            // machine-dictated session end: the configured pipeline completing IS the session —
            // close it, teardown publishes CLOSED (clean), no external driver involved
            State<Object> autoClose = new State<Object>("auto-close");
            autoClose.register((Consumer<Object>) o -> {
                ClientSessionContext c = sm.getContext();
                SharedIOUtil.close(c.getSession() != null ? (AutoCloseable) c.getSession() : c.getUDPSession());
            }, ClientEvent.READY);
            sm.register(autoClose);
        }
        return sm;
    }
}
