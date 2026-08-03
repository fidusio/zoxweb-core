package org.zoxweb.server.fsm;

import org.zoxweb.shared.data.events.EventHandlerListener;

/**
 * Observer of {@link StateMachine} activity — state changes, trigger routing and
 * consumption, state registration lifecycle, and machine start/close.
 * <p>
 * Register via {@link StateMachineInt#addListener(StateMachineListener)}. Listeners are
 * fully optional: a machine without listeners allocates no events and behaves exactly as
 * before. Filter by {@link StateMachineEvent#getType()} when only some events matter.
 * </p>
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li><b>Callbacks run inline</b> on the thread where the event occurred (publisher
 *       thread for TRIGGER_PUBLISHED; dispatch worker for STATE_CHANGED and
 *       TRIGGER_CONSUMED; caller thread for the rest). Listeners must be cheap —
 *       heavy work should be queued to an executor by the listener itself.</li>
 *   <li><b>Exceptions are isolated</b>: a listener throwing never affects dispatch or
 *       the other listeners ({@code Exception} is swallowed; {@code Error} propagates).</li>
 *   <li><b>The event is valid only for the duration of {@code handleEvent}</b> — never
 *       retain it (it pins states, consumers, triggers, and payloads against garbage
 *       collection). For durable history keep {@link StateMachineEvent#toLog()} strings,
 *       as {@link StateMachineEventHistory} does.</li>
 * </ul>
 */
public interface StateMachineListener
        extends EventHandlerListener<StateMachineEvent> {
    // inherits: void handleEvent(StateMachineEvent event) — lambda-friendly
}
