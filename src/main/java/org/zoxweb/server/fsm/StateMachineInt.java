package org.zoxweb.server.fsm;

import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.shared.util.GetConfig;
import org.zoxweb.shared.util.GetNVProperties;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.SUS;

import java.util.concurrent.Executor;

/**
 * StateMachineInt defines the contract for a trigger-driven event processing system.
 * <p>
 * <b>Important:</b> This is not a traditional finite state machine. It intentionally breaks
 * some classical FSM paradigms to support real-world scenarios where events can arrive
 * randomly from multiple threads.
 * </p>
 *
 * <h2>Design Philosophy</h2>
 * <ul>
 *   <li>Events (triggers) can arrive at any time from any source</li>
 *   <li>No transition validation - the system accepts any trigger regardless of current state</li>
 *   <li>States are simple identifiers with mutable properties, not behavior containers</li>
 *   <li>TriggerConsumers own the processing logic and are responsible for issuing subsequent triggers</li>
 *   <li>Supports concurrent event generation in multi-threaded environments</li>
 * </ul>
 *
 * <h2>Execution Model</h2>
 * The state machine supports three execution modes:
 * <ol>
 *   <li><b>TaskScheduler mode</b> - Events queued to a task scheduler for managed execution</li>
 *   <li><b>Executor mode</b> - Events dispatched to a thread pool for async execution</li>
 *   <li><b>Synchronous mode</b> - Events processed immediately in the calling thread</li>
 * </ol>
 *
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * StateMachine<MyConfig> sm = new StateMachine<>("my-fsm");
 * sm.register(initState);
 * sm.register(processingState);
 * sm.setConfig(myConfig);
 * sm.start(true); // Publishes INIT trigger
 * }</pre>
 *
 * @param <C> the configuration type associated with this state machine
 * @see StateInt
 * @see TriggerInt
 * @see TriggerConsumerInt
 */
public interface StateMachineInt<C>
        extends GetName, AutoCloseable, GetConfig<C>, GetNVProperties {

    /**
     * Registers a state with this state machine.
     * <p>
     * Registration also indexes all TriggerConsumers associated with the state
     * for efficient lookup during trigger dispatch.
     * </p>
     * <p>
     * States may be registered at any time, including while the machine is operational.
     * A publish that starts after registration completes is guaranteed to reach the new
     * state's consumers; triggers already snapshotted before registration will not include
     * them. Consumers added to a state after it has been registered are forwarded to the
     * machine automatically.
     * </p>
     *
     * @param state the state to register
     * @return this state machine for method chaining
     * @see #deregister(StateInt)
     */
    StateMachineInt<C> register(StateInt<?> state);

    StateMachineInt<C> setEventLogEnabled(boolean enabled);
    boolean isEventLogEnabled();

    /**
     * Deregisters a state from this state machine (the inverse of {@link #register(StateInt)}).
     * <p>
     * The state's consumers are removed from the machine's routing index; consumers from
     * other states sharing the same canonical IDs are unaffected. Deregistration is
     * effective for publishes that start after it completes — triggers already snapshotted
     * or queued still deliver (same policy as {@link #close()}: gate the future, never
     * cancel in-flight work).
     * </p>
     * <p>
     * The state object itself is kept intact (suspend/resume model): it retains its
     * consumers and properties, and a later {@link #register(StateInt)} fully restores it.
     * If the deregistered state is the current state, the current-state marker is cleared
     * (the machine reverts to pre-init delivery semantics). Registering the same consumer
     * instance in multiple states is unsupported; deregistration removes instances from
     * shared canonical-ID sets.
     * </p>
     *
     * @param state the state to deregister
     * @return true if the state was registered with this machine and has been removed,
     *         false if it was null or not registered
     * @throws IllegalArgumentException if the state is the INIT state — the machine's
     *                                  bootstrap anchor cannot be deregistered
     */
    boolean deregister(StateInt<?> state);

    /**
     * Deregisters a registered state by name.
     *
     * @param name the state name
     * @return true if a state with that name was registered and has been removed
     * @throws IllegalArgumentException if the name resolves to the INIT state
     * @see #deregister(StateInt)
     */
    default boolean deregister(String name){
        return deregister(lookupState(name));
    }

    /**
     * Deregisters a registered state by enum name.
     *
     * @param name the state name as an enum
     * @return true if a state with that name was registered and has been removed
     * @throws IllegalArgumentException if the name resolves to the INIT state
     * @see #deregister(StateInt)
     */
    default boolean deregister(Enum<?> name){
        return deregister(lookupState(name));
    }

    /**
     * Adds an observability listener to this machine.
     * <p>
     * Listeners are fully optional — a machine without listeners allocates no events and
     * behaves exactly as before. Callbacks run inline on the thread where the event
     * occurred and are exception-isolated; the delivered {@link StateMachineEvent} is
     * valid only for the duration of the callback and must not be retained
     * (see {@link StateMachineListener} for the full contract).
     * </p>
     *
     * @param listener the listener to add; null is a no-op; adding the same instance
     *                 twice registers it once
     * @return this state machine for method chaining
     */
    StateMachineInt<C> addListener(StateMachineListener listener);

    /**
     * Removes a previously added observability listener.
     *
     * @param listener the listener to remove
     * @return true if the listener was registered and has been removed
     */
    boolean removeListener(StateMachineListener listener);

    /**
     * Publishes a trigger asynchronously to all registered consumers.
     * <p>
     * The trigger is dispatched to all TriggerConsumers registered for the trigger's
     * canonical ID. Execution mode depends on configuration (TaskScheduler, Executor, or sync fallback).
     * </p>
     * <p>
     * The consumer set is snapshotted at publish time: consumers registered afterwards do not
     * receive this trigger. Publishing a canonical ID with no registered consumer is a silent
     * no-op.
     * </p>
     *
     * @param trigger the trigger to publish
     * @return this state machine for method chaining
     * @throws IllegalStateException if the machine is closed
     */
    StateMachineInt<C> publish(TriggerInt<?> trigger);

    /**
     * Convenience method to create and publish a trigger asynchronously.
     *
     * @param state the originating state
     * @param canID the canonical ID identifying the target consumer(s)
     * @param data  the data payload for the trigger
     * @param <D>   the type of trigger data
     * @return this state machine for method chaining
     */
    <D> StateMachineInt<C> publish(StateInt<?> state, String canID, D data);

    /**
     * Convenience method to create and publish a trigger asynchronously using an enum canonical ID.
     *
     * @param state the originating state
     * @param canID the canonical ID as an enum
     * @param data  the data payload for the trigger
     * @param <D>   the type of trigger data
     * @return this state machine for method chaining
     */
    <D> StateMachineInt<C> publish(StateInt<?> state, Enum<?> canID, D data);

    /**
     * Publishes a trigger synchronously, blocking until all consumers complete.
     * <p>
     * Use this when the caller needs to wait for processing to complete before continuing.
     * </p>
     *
     * @param trigger the trigger to publish
     * @return this state machine for method chaining
     */
    StateMachineInt<C> publishSync(TriggerInt<?> trigger);

    <D> StateMachineInt<C> publishSync(String canID, D data);
    <D> StateMachineInt<C> publishSync(Enum<?> canID, D data);

    /**
     * Convenience method to create and publish a trigger synchronously.
     *
     * @param state the originating state
     * @param canID the canonical ID identifying the target consumer(s)
     * @param data  the data payload for the trigger
     * @param <D>   the type of trigger data
     * @return this state machine for method chaining
     */
    <D> StateMachineInt<C> publishSync(StateInt<?> state, String canID, D data);

    /**
     * Convenience method to create and publish a trigger synchronously using an enum canonical ID.
     *
     * @param state the originating state
     * @param canID the canonical ID as an enum
     * @param data  the data payload for the trigger
     * @param <D>   the type of trigger data
     * @return this state machine for method chaining
     */
    <D> StateMachineInt<C> publishSync(StateInt<?> state, Enum<?> canID, D data);

    /**
     * Publishes a trigger only if it is relevant to the machine's current position.
     * <p>
     * This is a relevance-gated {@link #publish(TriggerInt)}:
     * </p>
     * <ul>
     *   <li>If the machine is not yet initialized (no current state — INIT has not fired),
     *       the trigger is delivered via normal broadcast so early events are not lost.</li>
     *   <li>If the current state has a consumer registered for the trigger's canonical ID,
     *       the trigger is broadcast — reaching <b>all</b> consumers registered for that ID,
     *       in any state, per the shared-ID design.</li>
     *   <li>If the current state has no consumer for the ID, the trigger is dropped.</li>
     * </ul>
     *
     * @param trigger the trigger to publish
     * @return this state machine for method chaining
     */
    StateMachineInt<C> publishToCurrentState(TriggerInt<?> trigger);

    /**
     * Sets the configuration object for this state machine.
     * <p>
     * The configuration is accessible to all TriggerConsumers via {@link #getConfig()}.
     * </p>
     *
     * @param config the configuration object
     * @return this state machine for method chaining
     */
    StateMachineInt<C> setConfig(C config);

    /**
     * Starts the state machine by publishing an INIT trigger.
     * <p>
     * This triggers the initial state's consumer to begin processing.
     * </p>
     *
     * @param sync if true, publishes INIT synchronously; if false, asynchronously
     */
    void start(boolean sync);

    /**
     * Closes this state machine: all subsequent publish calls throw {@link IllegalStateException}.
     * <p>
     * Closing is a gate on future publishes only — triggers already queued to the scheduler
     * or executor still execute. Owners tearing down shared resources should guard their
     * consumers accordingly.
     * </p>
     */
    void close();

    /**
     * @return the task scheduler processor if configured, null otherwise
     */
    TaskSchedulerProcessor getScheduler();

    /**
     * @return the executor for async trigger dispatch if configured, null otherwise
     */
    Executor getExecutor();

    /**
     * @return true if a TaskSchedulerProcessor is configured and enabled
     */
    boolean isScheduledTaskEnabled();

    /**
     * Looks up a registered state by name.
     *
     * @param name the state name
     * @return the state, or null if not found
     */
    StateInt<?> lookupState(String name);

    /**
     * Looks up a registered state by enum name.
     *
     * @param name the state name as an enum
     * @return the state, or null if not found
     */
    default StateInt<?> lookupState(Enum<?> name){
        return lookupState(SUS.enumName(name));
    }

    /**
     * @return the current state of this state machine
     */
    StateInt<?> getCurrentState();

    /**
     * Sets the current state.
     * <p>
     * Typically called by the framework (TriggerConsumerHolder) during trigger processing.
     * </p>
     *
     * @param state the new current state
     */
    void setCurrentState(StateInt<?> state);

}
