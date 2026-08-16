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
 * Programmatic composition of a {@link ClientConSM}: <b>add predefined states</b>
 * ({@link #state(State)}), then {@link #build()}. Each state is configured by its own
 * properties bag and composed of TriggerConsumers — there is no other composition mechanism.
 * <pre>
 * ClientConSM sm = ClientConSMBuilder.create("dns-probe")
 *         .transport(ClientSessionContext.Transport.UDP)
 *         .state(new MessageAssemblerState(assemblerConfig))
 *         .state(new ProtocolControllerState(controllerConfig))
 *         .state(new ResponseControllerState())
 *         .state(new ProtocolTypeValidatorState())
 *         .build();
 * </pre>
 * The builder registers the transport router state first, then the composed states in declared
 * order (broadcast order = registration order). The {@link ClientEvent#READY} gate is seeded
 * with the transport state plus every composed state whose bag carries
 * {@code ready_gate = true} (the controller's completion rule, the IMMEDIATE ssl handshake) —
 * the last gate to complete publishes the single {@code READY}.
 */
public class ClientConSMBuilder {

    /** Bag flag a state sets to declare it gates {@link ClientEvent#READY}. */
    public static final String READY_GATE = "ready_gate";

    private final String name;
    private final List<State<?>> states = new ArrayList<State<?>>();
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
     * Attaches the declarative settings bag, exposed to states via
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
     * stream-only compositions on them.
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
     * Appends a predefined state; states register in declared order (broadcast order).
     *
     * @throws IllegalArgumentException on a duplicate state name (same-named gating states
     *                                  would collapse to one READY-gate entry — premature
     *                                  READY — and their consumers would double-consume shared
     *                                  events); on a second ssl state (one TLS session per
     *                                  connection); or on an IMMEDIATE ssl state declared
     *                                  after a controller (broadcast order is declaration
     *                                  order, so the controller's CONNECTED consumer would run
     *                                  before the TLS auto-start and pump its first send in
     *                                  plaintext into the handshake)
     */
    public ClientConSMBuilder state(State<?> state) {
        SUS.checkIfNull("state null", state);
        for (State<?> s : states) {
            if (state instanceof SSLClientState && s instanceof SSLClientState)
                throw new IllegalArgumentException("at most one ssl state per machine");
            if (s.getName().equals(state.getName()))
                throw new IllegalArgumentException("duplicate state name: " + state.getName());
            if (state instanceof SSLClientState && s instanceof ProtocolControllerState
                    && ((SSLClientState) state).getMode() == SSLClientState.TLSMode.IMMEDIATE)
                throw new IllegalArgumentException(
                        "IMMEDIATE ssl state declared after the controller: the script would start" +
                        " in plaintext before the TLS auto-start; declare the ssl state first");
        }
        states.add(state);
        return this;
    }

    /**
     * Builds the synchronous machine: context wired, transport router state registered first
     * (TCP or UDP per {@link #transport}), composed states in declared order (an ssl state
     * brings the engine states {@link SSLClientHandshakeState}/{@link SSLClientDataState}
     * with it).
     *
     * @throws IllegalArgumentException on a UDP transport combined with an ssl state or a
     *                                  {@code start_tls} script step (no DTLS in this stack);
     *                                  on a controller with no assembler (its {@code expect}s
     *                                  could never be satisfied); or on a {@code start_tls}
     *                                  step without an ON_DEMAND ssl state (the publish would
     *                                  have no consumer — a silent forever-hang)
     */
    public ClientConSM build() {
        boolean udp = transport == ClientSessionContext.Transport.UDP;
        ProtocolControllerState controller = null;
        MessageAssemblerState assembler = null;
        SSLClientState ssl = null;
        for (State<?> s : states) {
            if (s instanceof ProtocolControllerState)
                controller = (ProtocolControllerState) s;
            else if (s instanceof MessageAssemblerState)
                assembler = (MessageAssemblerState) s;
            else if (s instanceof SSLClientState)
                ssl = (SSLClientState) s;
        }
        if (udp && ssl != null)
            throw new IllegalArgumentException("ssl state over UDP transport: no DTLS in this stack");
        if (controller != null && assembler == null)
            throw new IllegalArgumentException("controller without an assembler: expects could never be satisfied");
        if (controller != null && controller.hasStartTLS()) {
            if (udp)
                throw new IllegalArgumentException("start_tls step over UDP transport: no DTLS in this stack");
            if (ssl == null || ssl.getMode() != SSLClientState.TLSMode.ON_DEMAND)
                throw new IllegalArgumentException("start_tls step requires an ssl state with mode on_demand"
                        + (ssl == null ? " (no ssl state composed)" : " (mode is immediate)"));
        }

        Set<String> gates = new LinkedHashSet<String>();
        gates.add(udp ? UDPClientTransportState.NAME : ClientTransportState.NAME);
        for (State<?> s : states) {
            if (SMProtoUtil.booleanValue(s.getProperties(), READY_GATE, false))
                gates.add(s.getName());
        }

        ClientConSM sm = new ClientConSM(name);
        ClientSessionContext ctx = new ClientSessionContext(sm, settings, gates, transport);
        sm.setConfig(ctx);
        // machine-wide result accumulation bag: every state and TriggerConsumer writes its
        // outcome here via SMProtoUtil.results(sm), the caller reads it after CLOSED
        SMProtoUtil.results(sm);
        if (udp)
            sm.register(new UDPClientTransportState());
        else
            sm.register(new ClientTransportState());
        for (State<?> s : states) {
            if (s instanceof SSLClientState) {
                // the ssl state brings the engine states with it (the untouchable SSLUtil
                // handlers behind them — Rule 10)
                sm.register(new SSLClientHandshakeState());
                sm.register(new SSLClientDataState());
            }
            sm.register(s);
        }
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
