package org.zoxweb.shared.util;

public interface Registrar<K, V>
{
    void register(K key,V value);
    V unregister(K key);
    V lookup(K key);
}
