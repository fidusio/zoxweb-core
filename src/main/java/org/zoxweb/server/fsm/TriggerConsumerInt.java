package org.zoxweb.server.fsm;


import org.zoxweb.shared.util.SetNVProperties;

import java.util.function.Consumer;
import java.util.function.Function;

public interface TriggerConsumerInt<T>
        extends Consumer<T>, SetNVProperties {
    String[] canonicalIDs();

    StateInt<?> getState();

    void setSate(StateInt<?> state);

    <R> TriggerConsumerInt<?> setFunction(Function<?, R> function);

    <R> Function<T, R> getFunction();


    void publish(TriggerInt<?> triggerInt);

    <D> void publish(String canID, D data);

    <D> void publish(Enum<?> canID, D data);

    void publishSync(TriggerInt<?> triggerInt);

    <D> void publishSync(String canID, D data);

    <D> void publishSync(Enum<?> canID, D data);

    StateMachineInt<?> getStateMachine();

}
