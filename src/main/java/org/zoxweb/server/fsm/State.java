package org.zoxweb.server.fsm;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.util.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;


public class State<P>
    implements StateInt<P>
{


    public final static LogWrapper log = new LogWrapper(State.class).setEnabled(false);
    private final String name;
    private final NVGenericMap data = new NVGenericMap();
    private volatile StateMachineInt<?> stateMachine;

    private final  Map<String, Consumer<?>> triggerConsumers = new LinkedHashMap<String, Consumer<?>> ();
    public State(String name, NVBase<?> ...props)
    {
        this.name = name;
        if(props != null)
        {
            for (NVBase<?> nvb : props) {
                data.add(nvb);
            }
        }
    }
    public State(Enum<?> name, NVBase<?> ...props)
    {
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

    public Consumer<?> lookupConsumer(String canonicalID)
    {
        return triggerConsumers.get(canonicalID);
    }


    @Override
    public TriggerConsumerInt<?> lookupTriggerConsumer(GetName canonicalID) {
        return lookupTriggerConsumer(canonicalID.getName());
    }


    @Override
    public TriggerConsumerInt<?> lookupTriggerConsumer(Enum<?> canonicalID) {
        return lookupTriggerConsumer(canonicalID.name());
    }

    @Override
    public String getName() {
        return name;
    }

    public synchronized StateInt<?> register(TriggerConsumerInt<?> tc)
    {
        for(String canID : tc.canonicalIDs())
            triggerConsumers.put(canID, tc);
        tc.setSate(this);
        return this;
    }

    public synchronized StateInt<?> register(Consumer<?> consumer, String ...canIDs)
    {
        Consumer<?> tch = consumer instanceof TriggerConsumerHolder ? consumer : new TriggerConsumerHolder<>(consumer);
        for(String canID : canIDs)
            triggerConsumers.put(canID, tch);
        return this;
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

    public NVGenericMap getProperties()
    {
        return data;
    }

    public String toString()
    {
        return getName();
    }
}
