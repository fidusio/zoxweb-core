package org.zoxweb.server.fsm;

import org.zoxweb.shared.util.RegistrarMap;

import java.util.function.Consumer;

/**
 * This is a very simple state machine class
 * @param <K>
 * @param <V>
 */
public class MonoStateMachine<K, V>
    extends RegistrarMap<K, Consumer<V>>
{

    private final boolean notSync;

    public MonoStateMachine(boolean sync)
    {
        this.notSync = !sync;
    }
    public void publish(K key, V param)
    {
        if (notSync)
            lookup(key).accept(param);
        else
            synchronized (this)
            {
                lookup(key).accept(param);
            }

    }

}
