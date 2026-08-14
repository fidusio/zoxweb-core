package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the builder contract: synchronous machine, transport state registered, context
 * wired, bind rules enforced, one-machine-per-callback rule preserved.
 */
public class ClientConnectionSMBuilderTest {

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
    public void duplicatePhaseNameRejected() {
        // same-named gating phases would collapse to one READY-gate entry (premature READY)
        // and both would consume IN_DATA (double ownership)
        ClientConSMBuilder builder = ClientConSMBuilder.create("dup")
                .phase(new SSHBannerPhase());
        assertThrows(IllegalArgumentException.class, () -> builder.phase(new SSHBannerPhase()));
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
