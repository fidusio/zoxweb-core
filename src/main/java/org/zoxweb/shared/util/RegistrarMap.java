
package org.zoxweb.shared.util;

import java.util.HashMap;
import java.util.Map;

public class RegistrarMap<K, V>
    implements Registrar<K, V>
{

    private final Map<K,V> map = new HashMap<>();

    @Override
    public void register(K key, V value)
    {
        map.put(key, value);
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
