package org.zoxweb.server.fsm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MonoStateMachine<T, P>
{

    protected Map<T, Consumer<P>> stateMap = new LinkedHashMap<>();


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
        stateMap.get(t).accept(param);
    }
}
