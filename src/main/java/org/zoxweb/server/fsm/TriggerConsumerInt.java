package org.zoxweb.server.fsm;


import org.zoxweb.shared.util.SetNVProperties;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * TriggerConsumerInt is the core processing unit in the state machine.
 * <p>
 * <b>Important:</b> This is where the actual work happens. Unlike traditional FSM designs
 * where states contain behavior, here the TriggerConsumer is responsible for all processing
 * logic, error handling, and flow control.
 * </p>
 *
 * <h2>Contract</h2>
 * When a TriggerConsumer's {@link #accept(Object)} method is invoked, it <b>must</b>:
 * <ol>
 *   <li><b>Process the trigger data</b> - Perform the required business logic</li>
 *   <li><b>Handle any errors</b> - Catch and process exceptions appropriately</li>
 *   <li><b>Issue the next trigger</b> - Publish subsequent trigger(s) to continue the flow, or terminate</li>
 * </ol>
 *
 * <h2>Design Philosophy</h2>
 * <ul>
 *   <li>Keep it simple - each consumer handles one type of event</li>
 *   <li>Consumer owns error handling - no global error state in the FSM</li>
 *   <li>Consumer drives the flow - decides what trigger to publish next</li>
 *   <li>Stateless processing preferred - use State properties for shared context</li>
 * </ul>
 *
 * <h2>Canonical IDs</h2>
 * Each consumer registers for one or more canonical IDs. When a trigger with a matching
 * canonical ID is published, this consumer's {@link #accept(Object)} method is invoked.
 * Multiple consumers can register for the same canonical ID if needed.
 *
 * <h2>Publishing Next Triggers</h2>
 * Convenience methods are provided to publish subsequent triggers:
 * <ul>
 *   <li>{@link #publish(TriggerInt)} - Async publish of a trigger object</li>
 *   <li>{@link #publish(String, Object)} - Async publish with canonical ID and data</li>
 *   <li>{@link #publishSync(TriggerInt)} - Sync publish, blocks until processed</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * public class ProcessDataConsumer extends TriggerConsumer<InputData> {
 *     public ProcessDataConsumer() {
 *         super("PROCESS_DATA");
 *     }
 *
 *     @Override
 *     public void accept(InputData data) {
 *         try {
 *             Result result = process(data);
 *             publish("DATA_PROCESSED", result);  // Issue next trigger
 *         } catch (Exception e) {
 *             publish("PROCESS_ERROR", e);        // Handle error by publishing error trigger
 *         }
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of trigger data this consumer accepts
 * @see TriggerInt
 * @see StateInt
 * @see StateMachineInt
 */
public interface TriggerConsumerInt<T>
        extends Consumer<T>, SetNVProperties {

    /**
     * @return the canonical IDs this consumer is registered to handle
     */
    String[] canonicalIDs();

    /**
     * @return the state this consumer is registered with
     */
    StateInt<?> getState();

    /**
     * Associates this consumer with a state.
     *
     * @param state the owning state
     */
    void setSate(StateInt<?> state);

    /**
     * Sets an optional transformation function to apply during processing.
     *
     * @param function the transformation function
     * @param <R>      the function return type
     * @return this consumer for method chaining
     */
    <R> TriggerConsumerInt<?> setFunction(Function<?, R> function);

    /**
     * @param <R> the function return type
     * @return the transformation function, or null if not set
     */
    <R> Function<T, R> getFunction();

    /**
     * Publishes a trigger asynchronously to continue the processing flow.
     * <p>
     * This is the primary mechanism for a consumer to issue the next trigger
     * after completing its processing.
     * </p>
     *
     * @param triggerInt the trigger to publish
     */
    void publish(TriggerInt<?> triggerInt);

    /**
     * Convenience method to create and publish a trigger asynchronously.
     *
     * @param canID the canonical ID identifying the target consumer(s)
     * @param data  the data payload for the trigger
     * @param <D>   the type of trigger data
     */
    <D> void publish(String canID, D data);

    /**
     * Convenience method to create and publish a trigger asynchronously using an enum canonical ID.
     *
     * @param canID the canonical ID as an enum
     * @param data  the data payload for the trigger
     * @param <D>   the type of trigger data
     */
    <D> void publish(Enum<?> canID, D data);

    /**
     * Publishes a trigger synchronously, blocking until processing completes.
     *
     * @param triggerInt the trigger to publish
     */
    void publishSync(TriggerInt<?> triggerInt);

    /**
     * Convenience method to create and publish a trigger synchronously.
     *
     * @param canID the canonical ID identifying the target consumer(s)
     * @param data  the data payload for the trigger
     * @param <D>   the type of trigger data
     */
    <D> void publishSync(String canID, D data);

    /**
     * Convenience method to create and publish a trigger synchronously using an enum canonical ID.
     *
     * @param canID the canonical ID as an enum
     * @param data  the data payload for the trigger
     * @param <D>   the type of trigger data
     */
    <D> void publishSync(Enum<?> canID, D data);

    /**
     * @return the state machine this consumer belongs to (via its owning state)
     */
    StateMachineInt<?> getStateMachine();

}
