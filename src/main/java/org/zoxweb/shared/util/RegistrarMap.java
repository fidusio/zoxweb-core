
package org.zoxweb.shared.util;

import java.util.Map;

public class RegistrarMap<K, V, T>
    extends KVMapStoreDefault<K,V>
    implements Registrar<K, V, T>
{

    public RegistrarMap(Map<K,V> map)
    {
        super(map);
    }

    @Override
    public T register(K key, V value)
    {
        put(key, value);
        return (T) this;
    }

    @Override
    public V unregister(K key)
    {
        return removeGet(key);
    }

    @Override
    public<VAL extends V> VAL lookup(K key)
    {
        return (VAL)get(key);
    }
}
