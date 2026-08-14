package org.zoxweb.server.net.common.sm;

import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SUS;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Programmatic configuration of a {@link ClientConSM}: compose {@link ConnectionPhase}s,
 * then {@link #build()}.
 * <pre>
 * ClientConnectionSM sm = ClientConnectionSMBuilder.create("ssh-fingerprint")
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
     * Appends a phase; phases contribute their states in declared order.
     *
     * @throws IllegalArgumentException on a second SSL phase — one TLS session per connection —
     *                                  or a duplicate phase name: same-named gating phases would
     *                                  collapse to one entry in the READY gate (premature READY)
     *                                  and their consumers would double-consume shared events
     */
    public ClientConSMBuilder phase(ConnectionPhase phase) {
        SUS.checkIfNull("phase null", phase);
        for (ConnectionPhase p : phases) {
            if (phase instanceof SSLClientPhase && p instanceof SSLClientPhase)
                throw new IllegalArgumentException("at most one SSL phase per machine");
            if (p.getName().equals(phase.getName()))
                throw new IllegalArgumentException("duplicate phase name: " + phase.getName());
        }
        phases.add(phase);
        return this;
    }

    /**
     * Builds the synchronous machine: context wired, transport state registered first, phases
     * contributed in order.
     */
    public ClientConSM build() {
        Set<String> gating = new LinkedHashSet<String>();
        gating.add(ClientTransportState.NAME);
        for (ConnectionPhase p : phases) {
            if (p.gatesReady())
                gating.add(p.getName());
        }
        ClientConSM sm = new ClientConSM(name);
        ClientSessionContext ctx = new ClientSessionContext(sm, settings, gating);
        sm.setConfig(ctx);
        sm.register(new ClientTransportState());
        for (ConnectionPhase p : phases)
            p.contribute(sm);
        return sm;
    }
}
