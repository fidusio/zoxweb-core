package org.zoxweb.server.fsm;

import org.zoxweb.shared.util.GetNVProperties;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.NVGenericMap;

import java.util.function.Consumer;


public interface StateInt<P>
    extends GetName, GetNVProperties
{
    enum States
        implements GetName
    {
        INIT("init"),
        FINAL("final"),
        ;

        private final String name;
        States(String name)
        {
            this.name = name;
        }
        @Override
        public String getName() {
            return name;
        }
    }


    TriggerConsumerInt<?>[] triggers();

    TriggerConsumerInt<?> lookupTriggerConsumer(String canonicalID);
    TriggerConsumerInt<?> lookupTriggerConsumer(GetName canonicalID);
    TriggerConsumerInt<?> lookupTriggerConsumer(Enum<?> canonicalID);

    Consumer<?> lookupConsumer(String canonicalID);

    StateInt<?> register(TriggerConsumerInt<?> tc);


    StateInt<?> register(Consumer<?> consumer, String ...canIDs);
    StateInt<?> register(Consumer<?> consumer, Enum<?> ...canIDs);

    StateMachineInt<?> getStateMachine();

    void setStateMachine(StateMachineInt<?> smi);

    NVGenericMap getProperties();

}
