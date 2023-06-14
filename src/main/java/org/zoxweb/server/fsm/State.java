package org.zoxweb.server.fsm;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.NVBase;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SharedUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;


public class State<P>
    implements StateInt<P>
{


    public final static LogWrapper log = new LogWrapper(State.class).setEnabled(false);
    private final String name;
    private final NVGenericMap data = new NVGenericMap();
    private volatile StateMachineInt stateMachine;

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
        this(SharedUtil.enumName(name), props);
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
    public String getName() {
        return name;
    }

    public synchronized StateInt register(TriggerConsumerInt<?> tc)
    {
        for(String canID : tc.canonicalIDs())
            triggerConsumers.put(canID, tc);
        tc.setSate(this);
        return this;
    }

    public synchronized StateInt register(Consumer<?> consumer, String ...canIDs)
    {
        TriggerConsumerHolder<?> tch = new TriggerConsumerHolder<>(consumer);
        for(String canID : canIDs)
            triggerConsumers.put(canID, tch);
        return this;
    }



    @Override
    public StateMachineInt getStateMachine() {
        return stateMachine;
    }

    @Override
    public void setStateMachine(StateMachineInt smi) {
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
