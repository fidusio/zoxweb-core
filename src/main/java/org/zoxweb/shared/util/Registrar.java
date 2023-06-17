package org.zoxweb.shared.util;

public interface Registrar<K, V, T>
{
    T register(K key,V value);
    V unregister(K key);
    V lookup(K key);
}
