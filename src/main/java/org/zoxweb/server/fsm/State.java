package org.zoxweb.server.fsm;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.util.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


/**
 * Default {@link StateInt} implementation: a named identifier with a mutable property bag
 * that acts as a registry of TriggerConsumers.
 * <p>
 * Typed consumers register via {@link #register(TriggerConsumerInt)}; raw
 * {@link java.util.function.Consumer}s (e.g. lambdas) register via
 * {@link #register(java.util.function.Consumer, String...)} and are adapted into an anonymous
 * {@link TriggerConsumer} bound to the given canonical IDs, so both kinds are indexed and
 * dispatched identically.
 * </p>
 * <p>
 * If this state is already attached to a {@link StateMachine}, consumers registered afterwards
 * are forwarded to the machine immediately and become visible to subsequent publishes.
 * </p>
 *
 * @param <P> the type of properties associated with this state
 */
public class State<P>
        extends NVGMProperties
        implements StateInt<P> {

    public final static LogWrapper log = new LogWrapper(State.class).setEnabled(false);
    private final String name;
    //    private final NVGenericMap data = new NVGenericMap();
    private volatile StateMachineInt<?> stateMachine;

    private final Map<String, Consumer<?>> triggerConsumers = new ConcurrentHashMap<String, Consumer<?>>();

    public State(String name, NVBase<?>... props) {
        super(true, "state-properties");
        this.name = name;
        if (props != null) {
            for (NVBase<?> nvb : props) {
                getProperties().add(nvb);
            }
        }
    }

    public State(Enum<?> name, NVBase<?>... props) {
        this(SUS.enumName(name), props);
    }

    @Override
    public synchronized TriggerConsumerInt<?>[] triggers() {
        return triggerConsumers.values().toArray(new TriggerConsumerInt<?>[triggerConsumers.size()]);
    }

    @Override
    public TriggerConsumerInt<?> lookupTriggerConsumer(String canonicalID) {

        Consumer<?> ret = lookupConsumer(canonicalID);
        if (ret instanceof TriggerConsumerInt)
            return (TriggerConsumerInt<?>) ret;
        return null;
    }

    public Consumer<?> lookupConsumer(String canonicalID) {
        return triggerConsumers.get(canonicalID);
    }


    @Override
    public TriggerConsumerInt<?> lookupTriggerConsumer(GetName canonicalID) {
        return lookupTriggerConsumer(canonicalID.getName());
    }


    @Override
    public TriggerConsumerInt<?> lookupTriggerConsumer(Enum<?> canonicalID) {
        return lookupTriggerConsumer(SUS.enumName(canonicalID));
    }

    @Override
    public String getName() {
        return name;
    }

    public synchronized StateInt<?> register(TriggerConsumerInt<?> tc) {
        for (String canID : tc.canonicalIDs())
            triggerConsumers.put(canID, tc);
        tc.setState(this);
        if (getStateMachine() != null) {
            ((StateMachine<?>) getStateMachine()).mapTriggerConsumer(tc);
        }
        return this;
    }

    public synchronized StateInt<?> register(Consumer<?> consumer, String... canIDs) {
//        Consumer<Object> tch = consumer instanceof TriggerConsumerHolder ? (Consumer<Object>) consumer : new TriggerConsumerHolder<>(consumer);
//
//        TriggerConsumer<Object> tc = new TriggerConsumer<Object>(canIDs) {
//            @Override
//            public void accept(Object o) {
//                tch.accept(o);
//            }
//        };
//       return register(tc);
        SUS.checkIfNull("consumer null", consumer);
        Consumer<Object> target = (Consumer<Object>) consumer;
        TriggerConsumer<Object> tc = new TriggerConsumer<Object>(canIDs) {
            @Override
            public void accept(Object o) {
                target.accept(o);
            }
        };
        return register(tc);

    }

    /**
     * @param consumer
     * @param canIDs
     * @return
     */
    @Override
    public StateInt<?> register(Consumer<?> consumer, Enum<?>... canIDs) {

        return register(consumer, SUS.enumNames(canIDs));
    }


    @Override
    public StateMachineInt<?> getStateMachine() {
        return stateMachine;
    }

    @Override
    public void setStateMachine(StateMachineInt<?> smi) {
        stateMachine = smi;
    }


    public String toString() {
        return getName();
    }

}
