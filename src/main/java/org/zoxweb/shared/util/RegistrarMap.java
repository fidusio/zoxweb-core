
package org.zoxweb.shared.util;

import java.util.Map;

public abstract  class RegistrarMap<K, V, T>
        extends KVMapStoreDefault<K, V>
        implements Registrar<K, V, T> {


    public RegistrarMap(Map<K, V> map) {
        super(map);
    }

    @Override
    public T register(K key, V value) {
        put(key, value);
        return (T) this;
    }

    /**
     *
     * @param dd
     * @return
     */
    @Override
    public T setValueKeyDecoder(DataDecoder<V, K> dd) {
        valueToKey = dd;
        return (T) this;
    }

    @Override
    public T setNamedDescription(NamedDescription nd) {
        namedDescription = nd;
        return (T) this;
    }

    /**
     *
     * @return
     */
    @Override
    public DataDecoder<V, K> getValueKeyDecoder() {
        return valueToKey;
    }


    @Override
    public V unregister(K key) {
        return removeGet(key);
    }

    @Override
    public <VAL extends V> VAL lookup(K key) {
        return (VAL) get(key);
    }


    public T map(V value, K ...keys) {
        SUS.checkIfNull("value null", value);
        for (K k : keys)
            register(k, value);
        return (T)this;
    }
}
