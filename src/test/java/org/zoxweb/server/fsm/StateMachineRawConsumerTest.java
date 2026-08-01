package org.zoxweb.server.fsm;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies StateMachine registration and dispatch semantics:
 * <ul>
 *   <li>TriggerConsumer registration and dispatch via start()</li>
 *   <li>raw Consumer registration: adapted into a TriggerConsumer, indexed, and dispatched end to end</li>
 *   <li>multiple consumers sharing a canonical ID all receive the trigger (broadcast by design)</li>
 *   <li>publishing a canonical ID with no registered consumer is a silent no-op</li>
 *   <li>registering a raw Consumer with no canonical IDs is rejected</li>
 * </ul>
 */
public class StateMachineRawConsumerTest {

    /**
     * Control: the TriggerConsumerInt registration path works end to end via start().
     */
    @Test
    public void triggerConsumerPathWorks() {
        StateMachine<Void> sm = new StateMachine<Void>("control-fsm", (Executor) null);

        final AtomicBoolean fired = new AtomicBoolean(false);
        State<Object> init = new State<Object>(StateInt.States.INIT);
        init.register(new TriggerConsumer<Void>(StateInt.States.INIT) {
            @Override
            public void accept(Void v) {
                fired.set(true);
            }
        });

        sm.register(init);
        sm.start(true);

        assertTrue(fired.get(), "TriggerConsumer registered on INIT must fire on start()");
    }

    /**
     * A raw Consumer registered via register(Consumer, canIDs) is adapted into a
     * TriggerConsumer, indexed by the state machine, and receives the published payload.
     */
    @Test
    public void rawConsumerRegistersAndDispatches() {
        StateMachine<Void> sm = new StateMachine<Void>("raw-consumer-fsm", (Executor) null);

        final AtomicReference<String> received = new AtomicReference<String>();
        State<Object> state = new State<Object>("processing");
        state.register((Consumer<String>) received::set, "MY_EVENT");

        sm.register(state);
        sm.publishSync(state, "MY_EVENT", "hello");

        assertEquals("hello", received.get(), "raw Consumer must receive the published payload");
    }

    /**
     * The raw Consumer adapter is a real TriggerConsumerInt, visible to the typed lookup.
     */
    @Test
    public void rawConsumerVisibleToTypedLookup() {
        State<Object> state = new State<Object>("processing");
        state.register((Consumer<String>) data -> {
        }, "MY_EVENT");

        TriggerConsumerInt<?> tci = state.lookupTriggerConsumer("MY_EVENT");
        assertNotNull(tci, "typed lookup must return the adapter registered for MY_EVENT");
    }

    /**
     * By design, multiple TriggerConsumers may register the same canonical ID —
     * publishing that ID must reach all of them.
     */
    @Test
    public void sharedCanonicalIdBroadcastsToAllConsumers() {
        StateMachine<Void> sm = new StateMachine<Void>("broadcast-fsm", (Executor) null);
        final AtomicInteger count = new AtomicInteger();

        State<Object> stateA = new State<Object>("state-a");
        stateA.register(new TriggerConsumer<String>("SHARED_EVENT") {
            @Override
            public void accept(String s) {
                count.incrementAndGet();
            }
        });

        State<Object> stateB = new State<Object>("state-b");
        stateB.register(new TriggerConsumer<String>("SHARED_EVENT") {
            @Override
            public void accept(String s) {
                count.incrementAndGet();
            }
        });

        sm.register(stateA);
        sm.register(stateB);
        sm.publishSync(stateA, "SHARED_EVENT", "ping");

        assertEquals(2, count.get(), "both consumers registered for SHARED_EVENT must fire");
    }

    /**
     * Publishing a canonical ID nobody registered is a silent no-op, not an error.
     */
    @Test
    public void unknownCanonicalIdIsSilentNoOp() {
        StateMachine<Void> sm = new StateMachine<Void>("noop-fsm", (Executor) null);

        State<Object> state = new State<Object>("processing");
        state.register((Consumer<String>) data -> {
        }, "MY_EVENT");
        sm.register(state);

        assertDoesNotThrow(() -> sm.publishSync(state, "NOBODY_LISTENS", "ping"));
        assertDoesNotThrow(() -> sm.publish(state, "NOBODY_LISTENS", "ping"));
    }

    /**
     * Registering a raw Consumer without canonical IDs is rejected instead of
     * silently registering nothing.
     */
    @Test
    public void rawConsumerWithoutCanonicalIdsIsRejected() {
        State<Object> state = new State<Object>("processing");

        assertThrows(IllegalArgumentException.class,
                () -> state.register((Consumer<String>) data -> {
                }, new String[0]),
                "empty canonical IDs must be rejected at registration");
    }
}
