package org.zoxweb.shared.util;

public interface Registrar<K, V, T>
{
    T register(K key,V value);
    V unregister(K key);
    <VAL extends V> VAL lookup(K key);
}
