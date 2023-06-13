package org.zoxweb.server.fsm;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MonoStateMachine<T, P>
{

    protected final Map<T, Consumer<P>> stateMap = new HashMap<>();

    private final boolean notSync;

    public MonoStateMachine(boolean sync)
    {
        this.notSync = !sync;
    }
    public void map(T type, Consumer<P> consumer)
    {
        stateMap.put(type, consumer);
    }

    public void unmap(T type)
    {
        stateMap.remove(type);
    }

    public void dispatch(T t, P param)
    {
        if (notSync)
            stateMap.get(t).accept(param);
        else
            synchronized (this)
            {
                stateMap.get(t).accept(param);
            }

    }

}
