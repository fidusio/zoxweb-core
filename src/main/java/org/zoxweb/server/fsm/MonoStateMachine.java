package org.zoxweb.server.fsm;

import org.zoxweb.shared.util.RegistrarMap;

import java.util.LinkedHashMap;
import java.util.function.Consumer;

/**
 * A minimal standalone key-to-consumer dispatch map, for cases where the full
 * {@link StateMachine} framework is unnecessary: no states, no triggers, no config,
 * no current-state tracking — just {@code register(key, consumer)} and
 * {@link #publish(Object, Object)} invoking the registered consumer inline.
 * <p>
 * With {@code synchronous = true}, publishes are serialized on this instance's monitor;
 * otherwise the consumer runs unsynchronized on the calling thread. One consumer per key —
 * registering a key again replaces the previous consumer.
 * </p>
 * <p>
 * Reference usage: {@code org.zoxweb.server.net.ssl.CustomSSLStateMachine}.
 * </p>
 *
 * @param <K> the routing key type (e.g. an enum)
 * @param <V> the payload type passed to the consumer
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
