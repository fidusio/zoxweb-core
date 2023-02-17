package org.zoxweb.server.fsm;

import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.shared.util.GetName;

import java.util.concurrent.Executor;


public interface StateMachineInt<C>
extends GetName, AutoCloseable
{

    StateMachineInt register(StateInt state);

    StateMachineInt publish(TriggerInt trigger);
    public <D>StateMachineInt publish(StateInt state, String canID, D data);

    public <D>StateMachineInt publish(StateInt state, Enum<?> canID, D data);
    StateMachineInt publishSync(TriggerInt trigger);

    public <D>StateMachineInt publishSync(StateInt state, String canID, D data);

    public <D>StateMachineInt publishSync(StateInt state, Enum<?> canID, D data);
    StateMachineInt publishToCurrentState(TriggerInt trigger);



    <V extends C> V getConfig();
    StateMachineInt setConfig(C config);

    void start(boolean sync);

    void close();

    TaskSchedulerProcessor getScheduler();
    Executor getExecutor();
    boolean isScheduledTaskEnabled();
    StateInt lookupState(String name);
    StateInt lookupState(Enum<?> name);


    StateInt getCurrentState();

    void setCurrentState(StateInt state);

}
