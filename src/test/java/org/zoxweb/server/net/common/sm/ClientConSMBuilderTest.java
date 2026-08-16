package org.zoxweb.server.net.common.sm;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVPair;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the builder contract: synchronous machine, transport state registered, context
 * wired, bind rules enforced, one-machine-per-callback rule preserved.
 */
public class ClientConSMBuilderTest {

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

    private static List<GetNameValue<String>> pingSteps() {
        return Arrays.<GetNameValue<String>>asList(new NVPair(DataExchangePhase.OP_SEND, "txt:PING\r\n"));
    }

    @Test
    public void bannerPlusExchangeRejected() {
        // both phases consume IN_DATA — one active owner per buffer (mirrors the factory check)
        assertThrows(IllegalArgumentException.class, () -> ClientConSMBuilder.create("mix")
                .phase(new SSHBannerPhase())
                .phase(new DataExchangePhase(pingSteps())));
        assertThrows(IllegalArgumentException.class, () -> ClientConSMBuilder.create("mix")
                .phase(new DataExchangePhase(pingSteps()))
                .phase(new SSHBannerPhase()));
    }

    @Test
    public void immediateSSLAfterExchangeRejected() {
        // broadcast order is declaration order: the dialogue's CONNECTED consumer would run
        // before the TLS auto-start and pump its first send in plaintext into the handshake
        assertThrows(IllegalArgumentException.class, () -> ClientConSMBuilder.create("bad-order")
                .phase(new DataExchangePhase(pingSteps()))
                .phase(new SSLClientPhase(SSLClientPhase.TLSMode.IMMEDIATE, true)));
        // the correct order builds
        assertNotNull(ClientConSMBuilder.create("good-order")
                .phase(new SSLClientPhase(SSLClientPhase.TLSMode.IMMEDIATE, true))
                .phase(new DataExchangePhase(pingSteps()))
                .build());
        // ON_DEMAND after the exchange is the STARTTLS shape — allowed in either order
        assertNotNull(ClientConSMBuilder.create("starttls")
                .phase(new DataExchangePhase(pingSteps()))
                .phase(new SSLClientPhase(SSLClientPhase.TLSMode.ON_DEMAND, true))
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
