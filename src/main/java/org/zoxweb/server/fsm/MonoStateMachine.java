package org.zoxweb.server.fsm;

import org.zoxweb.shared.util.RegistrarMap;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

/**
 * This is a very simple state machine class
 * @param <K>
 * @param <V>
 */
public class MonoStateMachine<K, V>
    extends RegistrarMap<K, Consumer<V>, MonoStateMachine<K,V>>
{

    private final boolean notSync;

    public MonoStateMachine(boolean sync)
    {
        super(new LinkedHashMap<>());
        this.notSync = !sync;
    }
    public void publish(K key, V param)
    {

        Consumer<V> c = lookup(key);
        if (c != null) {
            if (notSync)
                c.accept(param);
            else
                synchronized (this)
                {
                    c.accept(param);
                }
        }
    }

}
