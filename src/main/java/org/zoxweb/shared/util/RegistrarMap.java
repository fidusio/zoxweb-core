
package org.zoxweb.shared.util;

import java.util.Map;
import java.util.function.Function;

public abstract class RegistrarMap<K, V, T>
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

    /**
     * Same contract as the Registrar lookup, it does not access the cache map directly to keep
     * the exclusion filter and the data size accounting of the store in sync, a value created by
     * the mapping function for an excluded key is not registered and null is returned
     *
     * @param key             to look for
     * @param mappingFunction applied to the key to create the value if it is not registered yet
     * @param <VAL>           the expected value type, a subtype of V
     * @return the registered value, null if the key was not registered and the mapping function
     * returned null or the key is excluded
     */
    @Override
    @SuppressWarnings("unchecked")
    public  <VAL extends V> VAL lookup(K key, Function<? super K, ? extends V> mappingFunction) {
        V ret = get(key);
        if (ret == null) {
            synchronized (this) {
                ret = get(key);
                if(ret == null) {
                    ret = mappingFunction.apply(toKey(key));
                    if (ret != null && !put(key, ret))
                        ret = null;
                }
            }
        }
        return (VAL) ret;
    }


    public T map(V value, K... keys) {
        SUS.checkIfNull("value null", value);
        for (K k : keys)
            register(k, value);
        return (T) this;
    }
}
