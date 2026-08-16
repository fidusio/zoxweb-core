package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.NamedValue;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the builder contract: composition by predefined states (each configured by its own
 * properties bag), synchronous machine, transport state registered, context wired, bind rules
 * enforced, one-machine-per-callback rule preserved, and the composition fail-fasts.
 */
public class ClientConSMBuilderTest {

    /** Programmatic controller config with the given steps. */
    private static NVGenericMap controllerConfig(GetNameValue<?>... steps) {
        List<GetNameValue<?>> list = new ArrayList<GetNameValue<?>>();
        for (GetNameValue<?> s : steps)
            list.add(s);
        NVGenericMap config = new NVGenericMap();
        config.add(new NamedValue<List<GetNameValue<?>>>("exchange", list));
        return config;
    }

    private static NVGenericMap pingConfig() {
        return controllerConfig(new NVPair(ProtocolControllerState.OP_SEND, "txt:PING\r\n"));
    }

    @Test
    public void builtMachineIsSynchronousWithTransportState() {
        ClientConSM sm = ClientConSMBuilder.create("plain-client").build();

        assertNull(sm.getExecutor(), "machine must be synchronous (null executor)");
        assertFalse(sm.isScheduledTaskEnabled(), "machine must not be scheduler-driven");
        assertNotNull(sm.lookupState(ClientTransportState.NAME), "transport state must be registered");
        assertNotNull(sm.getContext(), "context must be wired as the machine config");
        assertSame(sm, sm.getContext().getStateMachine());
        assertNotNull(sm.getContext().getSettings(), "settings bag must never be null");
    }

    @Test
    public void newSessionCallbackBindsContext() {
        ClientConSM sm = ClientConSMBuilder.create("bind-client").build();
        TCPSMCallback cb = sm.newSessionCallback();

        assertSame(cb, sm.getContext().getSession(), "callback must be bound to the context");
        assertSame(sm, cb.getConfig(), "machine must be the callback's config");
        // one machine per callback still enforced by TCPSMCallback
        assertThrows(IllegalArgumentException.class, () -> new TCPSMCallback(sm));
        // context rebind rejected
        assertThrows(IllegalStateException.class, () -> sm.getContext().bind(cb));
    }

    @Test
    public void duplicateStateNameRejected() {
        // same-named gating states would collapse to one READY-gate entry (premature READY)
        // and their consumers would double-consume shared events
        ClientConSMBuilder builder = ClientConSMBuilder.create("dup")
                .state(new MessageAssemblerState());
        assertThrows(IllegalArgumentException.class, () -> builder.state(new MessageAssemblerState()));
    }

    @Test
    public void controllerRequiresAssembler() {
        // a controller consumes IN_MESSAGE — without an assembler its expects could never be
        // satisfied
        assertThrows(IllegalArgumentException.class, () -> ClientConSMBuilder.create("no-assembler")
                .state(new ProtocolControllerState(pingConfig()))
                .build());
    }

    @Test
    public void immediateSSLAfterControllerRejected() {
        // broadcast order is declaration order: the controller's CONNECTED consumer would run
        // before the TLS auto-start and pump its first send in plaintext into the handshake
        assertThrows(IllegalArgumentException.class, () -> ClientConSMBuilder.create("bad-order")
                .state(new MessageAssemblerState())
                .state(new ProtocolControllerState(pingConfig()))
                .state(new SSLClientState(SSLClientState.TLSMode.IMMEDIATE, true)));
        // the correct order builds — the ssl state brings the engine states with it
        ClientConSM good = ClientConSMBuilder.create("good-order")
                .state(new SSLClientState(SSLClientState.TLSMode.IMMEDIATE, true))
                .state(new MessageAssemblerState())
                .state(new ProtocolControllerState(pingConfig()))
                .state(new ResponseControllerState())
                .build();
        assertNotNull(good.lookupState(SSLClientHandshakeState.NAME));
        assertNotNull(good.lookupState(SSLClientDataState.NAME));
        // ON_DEMAND after the controller is the STARTTLS shape — allowed in either order
        assertNotNull(ClientConSMBuilder.create("starttls")
                .state(new MessageAssemblerState())
                .state(new ProtocolControllerState(pingConfig()))
                .state(new SSLClientState(SSLClientState.TLSMode.ON_DEMAND, true))
                .build());
    }

    @Test
    public void startTLSStepRequiresOnDemandSSLState() {
        NVGenericMap withStartTLS = controllerConfig(new NVPair(ProtocolControllerState.OP_START_TLS, "true"));
        // no ssl state: the START_TLS publish would have no consumer — a silent forever-hang
        assertThrows(IllegalArgumentException.class, () -> ClientConSMBuilder.create("no-ssl")
                .state(new MessageAssemblerState())
                .state(new ProtocolControllerState(withStartTLS))
                .build());
        // IMMEDIATE ssl state: the session is already secure, SECURE never refires
        assertThrows(IllegalArgumentException.class, () -> ClientConSMBuilder.create("immediate")
                .state(new SSLClientState(SSLClientState.TLSMode.IMMEDIATE, true))
                .state(new MessageAssemblerState())
                .state(new ProtocolControllerState(withStartTLS))
                .build());
    }

    @Test
    public void bindRejectsForeignCallback() {
        ClientConSM smA = ClientConSMBuilder.create("client-a").build();
        ClientConSM smB = ClientConSMBuilder.create("client-b").build();
        TCPSMCallback cbB = smB.newSessionCallback();

        assertThrows(IllegalArgumentException.class, () -> smA.getContext().bind(cbB),
                "a callback bound to a different machine must be rejected");
    }
}
