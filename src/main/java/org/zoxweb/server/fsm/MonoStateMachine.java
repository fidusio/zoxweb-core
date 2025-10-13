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
        extends RegistrarMap<K, Consumer<V>, MonoStateMachine<K, V>> {

    private final boolean synchronous;

    public MonoStateMachine(boolean synchronous) {
        super(new LinkedHashMap<>());
        this.synchronous = synchronous;
    }

    public void publish(K key, V param) {

        Consumer<V> c = lookup(key);
        if (c != null) {
            if (synchronous)
                synchronized (this) {
                    c.accept(param);
                }
            else
                c.accept(param);

        }
    }

}
