package org.zoxweb.server.fsm;

import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.shared.util.GetConfig;
import org.zoxweb.shared.util.GetName;

import java.util.concurrent.Executor;


public interface StateMachineInt<C>
extends GetName, AutoCloseable, GetConfig<C>
{

    StateMachineInt<C> register(StateInt<?> state);

    StateMachineInt<C> publish(TriggerInt<?> trigger);
    <D>StateMachineInt<C> publish(StateInt<?> state, String canID, D data);

    <D>StateMachineInt<C> publish(StateInt<?> state, Enum<?> canID, D data);
    StateMachineInt<C> publishSync(TriggerInt<?> trigger);

     <D>StateMachineInt<C> publishSync(StateInt<?> state, String canID, D data);

     <D>StateMachineInt<C> publishSync(StateInt<?> state, Enum<?> canID, D data);
    StateMachineInt<C> publishToCurrentState(TriggerInt<?> trigger);



    StateMachineInt<C> setConfig(C config);

    void start(boolean sync);

    void close();

    TaskSchedulerProcessor getScheduler();
    Executor getExecutor();
    boolean isScheduledTaskEnabled();
    StateInt<?> lookupState(String name);
    StateInt<?> lookupState(Enum<?> name);


    StateInt<?> getCurrentState();

    void setCurrentState(StateInt<?> state);

}
