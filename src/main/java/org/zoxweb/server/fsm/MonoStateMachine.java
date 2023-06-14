package org.zoxweb.server.fsm;

import org.zoxweb.shared.util.Registrar;
import org.zoxweb.shared.util.RegistrarMap;

import java.util.function.Consumer;

/**
 * This is a very simple state machine class
 * @param <K>
 * @param <V>
 */
public class MonoStateMachine<K, V>
    implements Registrar<K, Consumer<V>>
{

    protected final RegistrarMap<K, Consumer<V>> registrarMap = new RegistrarMap<>();

    private final boolean notSync;

    public MonoStateMachine(boolean sync)
    {
        this.notSync = !sync;
    }
    public void register(K type, Consumer<V> consumer)
    {
        registrarMap.register(type, consumer);
    }

    public Consumer<V> unregister(K type)
    {
        return registrarMap.unregister(type);
    }

    public Consumer<V> lookup(K key)
    {
        return registrarMap.lookup(key);
    }

    public void dispatch(K key, V param)
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
