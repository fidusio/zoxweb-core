package org.zoxweb.server.fsm;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies StateMachine state deregistration semantics:
 * <ul>
 *   <li>deregistered states stop receiving broadcasts; unknown-ID publish stays a silent no-op</li>
 *   <li>shared canonical IDs keep delivering to surviving states</li>
 *   <li>deregistering the current state clears the current-state marker</li>
 *   <li>suspend/resume: re-registration fully restores delivery</li>
 *   <li>the INIT state cannot be deregistered</li>
 *   <li>consumers added to a detached state are not routed until re-registration</li>
 * </ul>
 */
public class StateMachineDeregisterTest {

    private enum TestState {
        PHASE_ONE,
        PHASE_TWO
    }

    private static TriggerConsumer<String> countingConsumer(final AtomicInteger counter, String canID) {
        return new TriggerConsumer<String>(canID) {
            @Override
            public void accept(String s) {
                counter.incrementAndGet();
            }
        };
    }

    @Test
    public void deregisterRemovesBroadcastDelivery() {
        StateMachine<Void> sm = new StateMachine<Void>("dereg-fsm", (Executor) null);
        final AtomicInteger count = new AtomicInteger();

        State<Object> state = new State<Object>("worker");
        state.register(countingConsumer(count, "MY_EVENT"));
        sm.register(state);

        sm.publishSync(state, "MY_EVENT", "one");
        assertEquals(1, count.get(), "registered state must receive the trigger");

        assertTrue(sm.deregister(state), "deregister of a registered state must return true");

        assertDoesNotThrow(() -> sm.publishSync(state, "MY_EVENT", "two"),
                "publish to an ID with no consumers must be a silent no-op");
        assertEquals(1, count.get(), "deregistered state must not receive further triggers");
        assertNull(sm.lookupState("worker"), "deregistered state must not be discoverable");
    }

    @Test
    public void deregisterPreservesSharedCanonicalId() {
        StateMachine<Void> sm = new StateMachine<Void>("shared-fsm", (Executor) null);
        final AtomicInteger countA = new AtomicInteger();
        final AtomicInteger countB = new AtomicInteger();

        State<Object> stateA = new State<Object>("state-a");
        stateA.register(countingConsumer(countA, "SHARED_EVENT"));
        State<Object> stateB = new State<Object>("state-b");
        stateB.register(countingConsumer(countB, "SHARED_EVENT"));

        sm.register(stateA);
        sm.register(stateB);
        sm.publishSync(stateA, "SHARED_EVENT", "ping");
        assertEquals(1, countA.get());
        assertEquals(1, countB.get());

        assertTrue(sm.deregister(stateA));
        sm.publishSync(stateB, "SHARED_EVENT", "ping");

        assertEquals(1, countA.get(), "deregistered state's consumer must not fire");
        assertEquals(2, countB.get(), "surviving state's consumer must keep firing");
    }

    @Test
    public void deregisterCurrentStateClearsIt() {
        StateMachine<Void> sm = new StateMachine<Void>("current-fsm", (Executor) null);
        final AtomicInteger countA = new AtomicInteger();
        final AtomicInteger countB = new AtomicInteger();

        State<Object> stateA = new State<Object>("state-a");
        stateA.register(countingConsumer(countA, "EVENT_A"));
        State<Object> stateB = new State<Object>("state-b");
        stateB.register(countingConsumer(countB, "EVENT_B"));
        sm.register(stateA);
        sm.register(stateB);

        sm.publishSync(stateA, "EVENT_A", "ping");
        assertSame(stateA, sm.getCurrentState(), "dispatch must set the current state");

        assertTrue(sm.deregister(stateA));
        assertNull(sm.getCurrentState(), "deregistering the current state must clear the marker");

        // pre-init delivery policy: null current state -> publishToCurrentState broadcasts
        sm.publishToCurrentState(new Trigger<String>(sm, "EVENT_B", null, "ping"));
        assertEquals(1, countB.get(), "broadcast fallback must reach the surviving consumer");
    }

    @Test
    public void reRegisterRestoresState() {
        StateMachine<Void> sm = new StateMachine<Void>("resume-fsm", (Executor) null);
        final AtomicInteger count = new AtomicInteger();

        State<Object> state = new State<Object>("suspendable");
        state.register(countingConsumer(count, "MY_EVENT"));
        sm.register(state);

        assertTrue(sm.deregister(state));
        sm.publishSync(state, "MY_EVENT", "while-suspended");
        assertEquals(0, count.get());

        sm.register(state);
        sm.publishSync(state, "MY_EVENT", "after-resume");
        assertEquals(1, count.get(), "re-registered state must receive triggers again");
        assertSame(state, sm.lookupState("suspendable"));
    }

    @Test
    public void deregisterUnknownStateIsNoOp() {
        StateMachine<Void> sm = new StateMachine<Void>("noop-fsm", (Executor) null);

        State<Object> neverRegistered = new State<Object>("ghost");
        neverRegistered.register(countingConsumer(new AtomicInteger(), "GHOST_EVENT"));

        assertFalse(sm.deregister(neverRegistered), "never-registered state must return false");
        assertFalse(sm.deregister((StateInt<?>) null), "null state must return false");
        assertFalse(sm.deregister("no-such-state"), "unknown name must return false");
    }

    @Test
    public void deregisterInitStateThrows() {
        StateMachine<Void> sm = new StateMachine<Void>("init-fsm", (Executor) null);
        final AtomicBoolean initFired = new AtomicBoolean();

        State<Object> init = new State<Object>(StateInt.States.INIT);
        init.register(new TriggerConsumer<Void>(StateInt.States.INIT) {
            @Override
            public void accept(Void v) {
                initFired.set(true);
            }
        });
        sm.register(init);

        assertThrows(IllegalArgumentException.class, () -> sm.deregister(init), "by object");
        assertThrows(IllegalArgumentException.class, () -> sm.deregister(StateInt.States.INIT.getName()), "by name");
        assertThrows(IllegalArgumentException.class, () -> sm.deregister(StateInt.States.INIT), "by enum");

        sm.start(true);
        assertTrue(initFired.get(), "machine must still start after refused INIT deregistration");
    }

    @Test
    public void deregisterByNameAndEnum() {
        StateMachine<Void> sm = new StateMachine<Void>("byname-fsm", (Executor) null);
        final AtomicInteger count = new AtomicInteger();

        State<Object> phaseOne = new State<Object>(TestState.PHASE_ONE);
        phaseOne.register(countingConsumer(count, "EVENT_ONE"));
        State<Object> phaseTwo = new State<Object>(TestState.PHASE_TWO);
        phaseTwo.register(countingConsumer(count, "EVENT_TWO"));
        sm.register(phaseOne);
        sm.register(phaseTwo);

        assertTrue(sm.deregister("PHASE_ONE"), "deregister by name");
        assertTrue(sm.deregister(TestState.PHASE_TWO), "deregister by enum");

        sm.publishSync(null, "EVENT_ONE", "ping");
        sm.publishSync(null, "EVENT_TWO", "ping");
        assertEquals(0, count.get(), "neither deregistered state may fire");
    }

    @Test
    public void lateConsumerAfterDeregisterNotRouted() {
        StateMachine<Void> sm = new StateMachine<Void>("late-fsm", (Executor) null);
        final AtomicReference<String> received = new AtomicReference<String>();

        State<Object> state = new State<Object>("detached");
        state.register(countingConsumer(new AtomicInteger(), "EARLY_EVENT"));
        sm.register(state);
        assertTrue(sm.deregister(state));

        // consumer added to a detached state: forwarding is stopped, not broadcast-visible
        state.register(new TriggerConsumer<String>("LATE_EVENT") {
            @Override
            public void accept(String s) {
                received.set(s);
            }
        });
        sm.publishSync(state, "LATE_EVENT", "should-not-arrive");
        assertNull(received.get(), "consumer on a detached state must not be routed");

        // re-registration restores everything, including the late consumer
        sm.register(state);
        sm.publishSync(state, "LATE_EVENT", "now-arrives");
        assertEquals("now-arrives", received.get(), "re-registration must route the late consumer");
    }
}
