package org.zoxweb.server.fsm;

import org.zoxweb.shared.util.GetNVProperties;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.NVGenericMap;

import java.util.function.Consumer;

/**
 * StateInt represents a named identifier with mutable properties in the state machine.
 * <p>
 * <b>Important:</b> Unlike traditional FSM states that encapsulate behavior, a StateInt is
 * simply an identifier that holds mutable properties. The actual processing logic resides
 * in {@link TriggerConsumerInt} implementations.
 * </p>
 *
 * <h2>Design Philosophy</h2>
 * <ul>
 *   <li>State is just an ID - a named container for context/properties</li>
 *   <li>Properties are mutable and can be modified by TriggerConsumers during processing</li>
 *   <li>States register TriggerConsumers that handle specific trigger types</li>
 *   <li>Multiple consumers can be registered for different trigger canonical IDs</li>
 * </ul>
 *
 * <h2>Relationship to TriggerConsumer</h2>
 * A State acts as a registry for TriggerConsumers. When a trigger arrives, the state machine
 * looks up the appropriate consumer by canonical ID and invokes it. The consumer then:
 * <ol>
 *   <li>Processes the trigger data</li>
 *   <li>Handles any errors that occur</li>
 *   <li>Publishes the next trigger to continue the flow</li>
 * </ol>
 *
 * <h2>Built-in States</h2>
 * The {@link States} enum provides standard state identifiers:
 * <ul>
 *   <li>{@code INIT} - Initial state, triggered when state machine starts</li>
 *   <li>{@code FINAL} - Terminal state for completion</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * State<MyProps> state = new State<>("processing", myProps);
 * state.register(new MyTriggerConsumer("TRIGGER_A"));
 * state.register(data -> handleData(data), "TRIGGER_B", "TRIGGER_C");
 * stateMachine.register(state);
 * }</pre>
 *
 * @param <P> the type of properties associated with this state
 * @see TriggerConsumerInt
 * @see StateMachineInt
 */
public interface StateInt<P>
        extends GetName, GetNVProperties {

    /**
     * Standard state identifiers for common FSM lifecycle points.
     */
    enum States
            implements GetName {
        /**
         * Initial state - triggered when the state machine starts via {@link StateMachineInt#start(boolean)}.
         */
        INIT("init"),
        /**
         * Final/terminal state - indicates processing is complete.
         */
        FINAL("final"),
        ;

        private final String name;

        States(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * Returns all TriggerConsumers registered with this state.
     *
     * @return array of all associated TriggerConsumers
     */
    TriggerConsumerInt<?>[] triggers();

    /**
     * Looks up a TriggerConsumer by its registered canonical ID.
     *
     * @param canonicalID the trigger canonical ID
     * @return the TriggerConsumer registered for that ID, or null if not found
     */
    TriggerConsumerInt<?> lookupTriggerConsumer(String canonicalID);

    /**
     * Looks up a TriggerConsumer by its registered canonical ID using a GetName instance.
     *
     * @param canonicalID the trigger canonical ID as a GetName
     * @return the TriggerConsumer registered for that ID, or null if not found
     */
    TriggerConsumerInt<?> lookupTriggerConsumer(GetName canonicalID);

    /**
     * Looks up a TriggerConsumer by its registered canonical ID using an enum.
     *
     * @param canonicalID the trigger canonical ID as an enum
     * @return the TriggerConsumer registered for that ID, or null if not found
     */
    TriggerConsumerInt<?> lookupTriggerConsumer(Enum<?> canonicalID);

    /**
     * Looks up a raw Consumer by its registered canonical ID.
     * <p>
     * Use this when the consumer was registered via {@link #register(Consumer, String...)}
     * rather than as a TriggerConsumerInt.
     * </p>
     *
     * @param canonicalID the trigger canonical ID
     * @return the Consumer registered for that ID, or null if not found
     */
    Consumer<?> lookupConsumer(String canonicalID);

    /**
     * Registers a TriggerConsumer with this state.
     * <p>
     * The consumer will be invoked when a trigger matching any of its
     * {@link TriggerConsumerInt#canonicalIDs()} is published.
     * </p>
     *
     * @param tc the TriggerConsumer to register
     * @return this state for method chaining
     */
    StateInt<?> register(TriggerConsumerInt<?> tc);

    /**
     * Registers a raw Consumer for one or more canonical IDs.
     * <p>
     * This is a convenience method for simple handlers that don't need the full
     * TriggerConsumer functionality. The consumer will be invoked when a trigger
     * matching any of the specified canonical IDs is published.
     * </p>
     *
     * @param consumer the Consumer to register
     * @param canIDs   one or more canonical IDs this consumer handles
     * @return this state for method chaining
     */
    StateInt<?> register(Consumer<?> consumer, String... canIDs);

    /**
     * Registers a raw Consumer for one or more canonical IDs specified as enums.
     *
     * @param consumer the Consumer to register
     * @param canIDs   one or more canonical IDs as enums
     * @return this state for method chaining
     */
    StateInt<?> register(Consumer<?> consumer, Enum<?>... canIDs);

    /**
     * Returns the state machine this state is registered with.
     *
     * @return the owning StateMachine, or null if not yet registered
     */
    StateMachineInt<?> getStateMachine();

    /**
     * Associates this state with a state machine.
     * <p>
     * Typically called by the framework during {@link StateMachineInt#register(StateInt)}.
     * </p>
     *
     * @param smi the owning state machine
     */
    void setStateMachine(StateMachineInt<?> smi);

    /**
     * Returns the mutable properties map for this state.
     * <p>
     * TriggerConsumers can use this to store and retrieve shared context
     * during processing. Properties are mutable and can be modified at any time.
     * </p>
     *
     * @return the properties map
     */
    NVGenericMap getProperties();

}
