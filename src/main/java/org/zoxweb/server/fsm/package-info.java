/**
 * Trigger-driven event processing framework (FSM package).
 * <p>
 * <b>This is intentionally not a classical finite state machine.</b> There is no transition
 * table and no transition validation. The package implements a canonical-ID routed
 * publish/subscribe engine with a current-state marker, designed for real-world systems
 * (e.g. the TLS handshake engine in {@code org.zoxweb.server.net.ssl}) where events arrive
 * at any time, from any thread, in any order.
 * </p>
 *
 * <h2>Components</h2>
 * <ul>
 *   <li>{@link org.zoxweb.server.fsm.StateMachineInt} / {@link org.zoxweb.server.fsm.StateMachine}
 *       — the engine: registry of states, single dispatch funnel for all triggers, owner of the
 *       execution mode, the shared config object, and the current-state marker.</li>
 *   <li>{@link org.zoxweb.server.fsm.StateInt} / {@link org.zoxweb.server.fsm.State}
 *       — a named identifier with a mutable property bag; acts as a registry of
 *       TriggerConsumers. States hold context, not behavior.</li>
 *   <li>{@link org.zoxweb.server.fsm.TriggerInt} / {@link org.zoxweb.server.fsm.Trigger}
 *       — an immutable event: canonical ID (routing key), typed data payload, issuing state,
 *       JVM-global unique ID, and creation timestamp.</li>
 *   <li>{@link org.zoxweb.server.fsm.TriggerConsumerInt} / {@link org.zoxweb.server.fsm.TriggerConsumer}
 *       — the processing unit: bound to one or more canonical IDs, owns business logic, error
 *       handling, and flow control (publishes the next trigger or terminates the flow).</li>
 *   <li>{@link org.zoxweb.server.fsm.MonoStateMachine}
 *       — a minimal standalone key-to-consumer dispatch map for cases where the full framework
 *       is unnecessary.</li>
 * </ul>
 *
 * <h2>Dispatch rules</h2>
 * <ul>
 *   <li><b>Single funnel:</b> every trigger is delivered through
 *       {@link org.zoxweb.server.fsm.StateMachineInt#publish(TriggerInt)} or
 *       {@link org.zoxweb.server.fsm.StateMachineInt#publishSync(TriggerInt)}; all other
 *       publishing methods (consumer helpers, convenience overloads, {@code start()}) delegate
 *       to these two.</li>
 *   <li><b>Broadcast by design:</b> multiple consumers — in the same or different states — may
 *       register the same canonical ID, and all of them receive every trigger published under
 *       that ID.</li>
 *   <li><b>Unknown IDs are a silent no-op:</b> publishing a canonical ID with no registered
 *       consumer delivers nothing and throws nothing.</li>
 *   <li><b>Snapshot semantics:</b> each publish captures the consumer set for its canonical ID
 *       at publish time. Consumers registered later receive only triggers published after their
 *       registration completes — late joiners see the future, not the past.</li>
 * </ul>
 *
 * <h2>Runtime registration</h2>
 * States may be registered while the machine is operational. Consumers added to an
 * already-registered {@link org.zoxweb.server.fsm.State} are forwarded to the machine
 * automatically and become visible to subsequent publishes. Registration (writers) and
 * dispatch lookup (readers) synchronize on the machine, so every publish sees a consistent
 * before-or-after view of any concurrent registration.
 *
 * <h2>Execution modes</h2>
 * Chosen at construction of {@link org.zoxweb.server.fsm.StateMachine}:
 * <ol>
 *   <li><b>TaskScheduler mode</b> — triggers are queued to a
 *       {@link org.zoxweb.server.task.TaskSchedulerProcessor}.</li>
 *   <li><b>Executor mode</b> — triggers are handed to an {@link java.util.concurrent.Executor}.</li>
 *   <li><b>Synchronous mode</b> — with a null executor, publishing runs consumers inline in the
 *       calling thread; {@code publishSync} always runs inline regardless of mode.</li>
 * </ol>
 *
 * @see org.zoxweb.server.fsm.StateMachineInt
 * @see org.zoxweb.server.net.ssl.SSLStateMachine
 */
package org.zoxweb.server.fsm;
