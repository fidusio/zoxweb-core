package org.zoxweb.server.fsm;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies StateMachine observability (transition listeners):
 * <ul>
 *   <li>STATE_CHANGED fires only on actual current-state changes</li>
 *   <li>TRIGGER_PUBLISHED reports the dispatch snapshot size (incl. 0 for unrouted IDs)</li>
 *   <li>TRIGGER_CONSUMED fires per (trigger, consumer) execution with the consumer attached</li>
 *   <li>register/deregister/start/close lifecycle events</li>
 *   <li>listener exception isolation and add/remove/dedupe/ordering semantics</li>
 *   <li>StateMachineEventHistory: bounded, ordered, string-only (GC-safe) history</li>
 * </ul>
 */
public class StateMachineListenerTest {

    private static class RecordingListener implements StateMachineListener {
        final List<StateMachineEvent> events = new ArrayList<StateMachineEvent>();

        @Override
        public void handleEvent(StateMachineEvent event) {
            events.add(event);
        }

        List<StateMachineEvent> byType(StateMachineEvent.Type type) {
            List<StateMachineEvent> ret = new ArrayList<StateMachineEvent>();
            for (StateMachineEvent e : events)
                if (e.getType() == type)
                    ret.add(e);
            return ret;
        }
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
    public void stateChangedFiresOnlyOnActualChange() {
        StateMachine<Void> sm = new StateMachine<Void>("sc-fsm", (Executor) null);
        State<Object> stateA = new State<Object>("state-a");
        stateA.register(countingConsumer(new AtomicInteger(), "EVENT_A"));
        State<Object> stateB = new State<Object>("state-b");
        stateB.register(countingConsumer(new AtomicInteger(), "EVENT_B"));
        sm.register(stateA);
        sm.register(stateB);

        RecordingListener listener = new RecordingListener();
        sm.addListener(listener);

        sm.publishSync(null, "EVENT_A", "ping");   // null -> A
        sm.publishSync(null, "EVENT_B", "ping");   // A -> B
        sm.publishSync(null, "EVENT_B", "ping");   // B -> B: silent

        List<StateMachineEvent> changes = listener.byType(StateMachineEvent.Type.STATE_CHANGED);
        assertEquals(2, changes.size(), "same-state dispatch must not fire STATE_CHANGED");
        assertNull(changes.get(0).getOldState());
        assertSame(stateA, changes.get(0).getNewState());
        assertSame(stateA, changes.get(1).getOldState());
        assertSame(stateB, changes.get(1).getNewState());
        assertTrue(changes.get(0).getTimeStamp() > 0, "event must carry a timestamp");
        assertTrue(changes.get(1).getID() > changes.get(0).getID(), "sequence ids must ascend");
    }

    @Test
    public void triggerPublishedReportsConsumerCount() {
        StateMachine<Void> sm = new StateMachine<Void>("tp-fsm", (Executor) null);
        State<Object> stateA = new State<Object>("state-a");
        stateA.register(countingConsumer(new AtomicInteger(), "SHARED_EVENT"));
        State<Object> stateB = new State<Object>("state-b");
        stateB.register(countingConsumer(new AtomicInteger(), "SHARED_EVENT"));
        sm.register(stateA);
        sm.register(stateB);

        RecordingListener listener = new RecordingListener();
        sm.addListener(listener);

        sm.publishSync(null, "SHARED_EVENT", "ping");
        sm.publish(null, "NOBODY_LISTENS", "ping");

        List<StateMachineEvent> published = listener.byType(StateMachineEvent.Type.TRIGGER_PUBLISHED);
        assertEquals(2, published.size());
        assertEquals(2, published.get(0).getConsumerCount(), "shared ID must report both consumers");
        assertEquals("SHARED_EVENT", published.get(0).getCanonicalID());
        assertNotNull(published.get(0).getData(), "the trigger must ride on the event");
        assertEquals(0, published.get(1).getConsumerCount(), "unrouted ID must be reported with count 0");
        assertEquals("NOBODY_LISTENS", published.get(1).getCanonicalID());
    }

    @Test
    public void triggerConsumedFiresPerConsumer() {
        StateMachine<Void> sm = new StateMachine<Void>("tc-fsm", (Executor) null);
        State<Object> stateA = new State<Object>("state-a");
        stateA.register(countingConsumer(new AtomicInteger(), "SHARED_EVENT"));
        State<Object> stateB = new State<Object>("state-b");
        stateB.register(countingConsumer(new AtomicInteger(), "SHARED_EVENT"));
        sm.register(stateA);
        sm.register(stateB);

        RecordingListener listener = new RecordingListener();
        sm.addListener(listener);

        sm.publishSync(null, "SHARED_EVENT", "ping");

        List<StateMachineEvent> consumed = listener.byType(StateMachineEvent.Type.TRIGGER_CONSUMED);
        assertEquals(2, consumed.size(), "one TRIGGER_CONSUMED per consumer");
        assertNotSame(consumed.get(0).getConsumer(), consumed.get(1).getConsumer(),
                "each event must carry its own consumer");
        for (StateMachineEvent e : consumed) {
            assertEquals("SHARED_EVENT", e.getCanonicalID());
            assertNotNull(e.getData(), "the trigger must ride on the event");
            assertNotNull(e.getConsumer().getState(), "the consumer identifies its owning state");
        }
        // published first, then the consumptions (ascending sequence)
        StateMachineEvent published = listener.byType(StateMachineEvent.Type.TRIGGER_PUBLISHED).get(0);
        assertTrue(published.getID() < consumed.get(0).getID());
        assertTrue(consumed.get(0).getID() < consumed.get(1).getID());
    }

    @Test
    public void lifecycleEventsOnRegisterDeregister() {
        StateMachine<Void> sm = new StateMachine<Void>("lc-fsm", (Executor) null);
        RecordingListener listener = new RecordingListener();
        sm.addListener(listener);

        State<Object> state = new State<Object>("worker");
        state.register(countingConsumer(new AtomicInteger(), "MY_EVENT"));
        sm.register(state);

        List<StateMachineEvent> registered = listener.byType(StateMachineEvent.Type.STATE_REGISTERED);
        assertEquals(1, registered.size());
        assertSame(state, registered.get(0).getNewState());

        sm.publishSync(null, "MY_EVENT", "ping");   // makes 'state' current
        assertTrue(sm.deregister(state));

        List<StateMachineEvent> changes = listener.byType(StateMachineEvent.Type.STATE_CHANGED);
        StateMachineEvent cleared = changes.get(changes.size() - 1);
        assertSame(state, cleared.getOldState());
        assertNull(cleared.getNewState(), "deregistering the current state must report state -> null");

        List<StateMachineEvent> deregistered = listener.byType(StateMachineEvent.Type.STATE_DEREGISTERED);
        assertEquals(1, deregistered.size());
        assertSame(state, deregistered.get(0).getNewState());
        assertTrue(deregistered.get(0).getID() > cleared.getID(),
                "STATE_DEREGISTERED is the completion event, fired last");
    }

    @Test
    public void startAndCloseEvents() {
        StateMachine<Void> sm = new StateMachine<Void>("bk-fsm", (Executor) null);
        final List<String> order = new ArrayList<String>();

        State<Object> init = new State<Object>(StateInt.States.INIT);
        init.register(new TriggerConsumer<Void>(StateInt.States.INIT) {
            @Override
            public void accept(Void v) {
                order.add("init-run");
            }
        });
        sm.register(init);

        sm.addListener(event -> {
            if (event.getType() == StateMachineEvent.Type.MACHINE_STARTED
                    || event.getType() == StateMachineEvent.Type.MACHINE_CLOSED)
                order.add(event.getType().getName());
        });

        sm.start(true);
        sm.close();
        sm.close();   // second close must not re-fire

        assertEquals(3, order.size(), "started, init-run, closed - exactly once each");
        assertEquals("machine-started", order.get(0), "MACHINE_STARTED must precede the INIT consumer");
        assertEquals("init-run", order.get(1));
        assertEquals("machine-closed", order.get(2));
    }

    @Test
    public void listenerExceptionIsolation() {
        StateMachine<Void> sm = new StateMachine<Void>("iso-fsm", (Executor) null);
        final AtomicInteger consumerRuns = new AtomicInteger();
        State<Object> state = new State<Object>("worker");
        state.register(countingConsumer(consumerRuns, "MY_EVENT"));
        sm.register(state);

        RecordingListener good = new RecordingListener();
        sm.addListener(event -> {
            throw new RuntimeException("bad listener");
        });
        sm.addListener(good);

        assertDoesNotThrow(() -> sm.publishSync(null, "MY_EVENT", "ping"));
        assertEquals(1, consumerRuns.get(), "consumer dispatch must be unaffected by a failing listener");
        assertFalse(good.events.isEmpty(), "the second listener must still receive every event");
    }

    @Test
    public void historyListenerKeepsBoundedOrderedHistory() {
        StateMachine<Void> sm = new StateMachine<Void>("hist-fsm", (Executor) null);
        StateMachineEventHistory history = new StateMachineEventHistory(4);
        sm.addListener(history);

        State<Object> state = new State<Object>("worker");
        state.register(countingConsumer(new AtomicInteger(), "MY_EVENT"));
        sm.register(state);                               // events: registered
        sm.publishSync(null, "MY_EVENT", "one");          // published, state-changed, consumed
        sm.publishSync(null, "MY_EVENT", "two");          // published, consumed

        assertEquals(4, history.size(), "capacity must bound the history");
        List<String> lines = history.history();
        long lastId = -1;
        for (String line : lines) {
            assertTrue(line.contains("hist-fsm"), "line must carry the machine name: " + line);
            long id = Long.parseLong(line.substring(0, line.indexOf(' ')));
            assertTrue(id > lastId, "sequence ids must ascend, oldest first");
            lastId = id;
        }
        assertTrue(history.toLog("\n").contains("\n"), "joined report must contain separators");

        history.clear();
        assertEquals(0, history.size());
    }

    @Test
    public void historyStoresOnlyStrings() {
        StateMachine<Void> sm = new StateMachine<Void>("gc-fsm", (Executor) null);
        StateMachineEventHistory history = new StateMachineEventHistory();
        sm.addListener(history);

        State<Object> state = new State<Object>("transient-state");
        state.register(countingConsumer(new AtomicInteger(), "MY_EVENT"));
        sm.register(state);
        sm.publishSync(null, "MY_EVENT", "payload");
        sm.deregister(state);

        for (Object line : history.history())
            assertTrue(line instanceof String,
                    "history must retain only log strings - never event objects (GC contract)");
        assertTrue(history.size() > 0);
    }

    @Test
    public void addRemoveAndDedupe() {
        StateMachine<Void> sm = new StateMachine<Void>("ar-fsm", (Executor) null);
        State<Object> state = new State<Object>("worker");
        state.register(countingConsumer(new AtomicInteger(), "MY_EVENT"));
        sm.register(state);

        final List<String> arrival = new ArrayList<String>();
        StateMachineListener l1 = event -> arrival.add("L1");
        StateMachineListener l2 = event -> arrival.add("L2");
        StateMachineListener l3 = event -> arrival.add("L3");

        sm.addListener(l1).addListener(l2).addListener(l3);
        sm.addListener(l1);   // duplicate: must register once

        sm.publish(null, "NOBODY_LISTENS", "x");   // exactly one event: TRIGGER_PUBLISHED count 0
        assertEquals(3, arrival.size(), "duplicate add must not double-notify");
        assertEquals("L1", arrival.get(0));
        assertEquals("L2", arrival.get(1));
        assertEquals("L3", arrival.get(2));

        assertTrue(sm.removeListener(l2));
        assertFalse(sm.removeListener(l2), "second remove must return false");
        arrival.clear();

        sm.publish(null, "NOBODY_LISTENS", "x");
        assertEquals(2, arrival.size(), "removed listener must stop receiving");
        assertEquals("L1", arrival.get(0));
        assertEquals("L3", arrival.get(1));
    }

    @Test
    public void eventLogKillSwitch() {
        StateMachine<Void> sm = new StateMachine<Void>("gate-fsm", (Executor) null);
        State<Object> state = new State<Object>("worker");
        state.register(countingConsumer(new AtomicInteger(), "MY_EVENT"));
        sm.register(state);

        RecordingListener listener = new RecordingListener();
        sm.addListener(listener);

        assertTrue(sm.isEventLogEnabled(), "event log must be enabled by default (kill-switch semantics)");
        sm.publishSync(null, "MY_EVENT", "ping");
        assertFalse(listener.events.isEmpty(), "an attached listener must receive events out of the box");

        int count = listener.events.size();
        sm.setEventLogEnabled(false);
        sm.publishSync(null, "MY_EVENT", "ping");
        assertEquals(count, listener.events.size(), "disabling the gate must mute delivery immediately");

        sm.setEventLogEnabled(true);
        sm.publishSync(null, "MY_EVENT", "ping");
        assertTrue(listener.events.size() > count, "re-enabling the gate must resume delivery");
    }
}
