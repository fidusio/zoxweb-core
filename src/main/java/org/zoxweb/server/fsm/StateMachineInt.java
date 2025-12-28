package org.zoxweb.server.fsm;

import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.shared.util.GetConfig;
import org.zoxweb.shared.util.GetName;

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
        extends GetName, AutoCloseable, GetConfig<C> {

    /**
     * Registers a state with this state machine.
     * <p>
     * Registration also indexes all TriggerConsumers associated with the state
     * for efficient lookup during trigger dispatch.
     * </p>
     *
     * @param state the state to register
     * @return this state machine for method chaining
     */
    StateMachineInt<C> register(StateInt<?> state);

    /**
     * Publishes a trigger asynchronously to all registered consumers.
     * <p>
     * The trigger is dispatched to all TriggerConsumers registered for the trigger's
     * canonical ID. Execution mode depends on configuration (TaskScheduler, Executor, or sync fallback).
     * </p>
     *
     * @param trigger the trigger to publish
     * @return this state machine for method chaining
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
     * Publishes a trigger only to the current state's consumer for that trigger.
     * <p>
     * Unlike {@link #publish(TriggerInt)} which broadcasts to all registered consumers,
     * this method targets only the consumer registered in the current state.
     * </p>
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
     * Closes this state machine and releases any resources.
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
    StateInt<?> lookupState(Enum<?> name);

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
