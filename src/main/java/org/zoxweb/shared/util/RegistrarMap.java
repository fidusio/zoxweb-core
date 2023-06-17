
package org.zoxweb.shared.util;

import java.util.HashMap;
import java.util.Map;

public class RegistrarMap<K, V, T>
    implements Registrar<K, V, T>
{

    private final Map<K,V> map = new HashMap<>();

//    @Override
//    public RegistrarMap register(K key, V value)
//    {
//        map.put(key, value);
//        return this;
//
//    }

    @Override
    public T register(K key, V value)
    {
        map.put(key, value);
        return (T) this;
    }

    @Override
    public V unregister(K key)
    {
        return map.remove(key);
    }

    @Override
    public V lookup(K key)
    {
        return map.get(key);
    }
}
