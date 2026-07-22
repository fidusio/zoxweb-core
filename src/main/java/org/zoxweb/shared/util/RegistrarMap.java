
package org.zoxweb.shared.util;

import java.util.Map;
import java.util.function.Function;

/**
 * Registrar implementation backed by a KVMapStoreDefault, it exposes the store as a
 * chainable registry: register/unregister/lookup delegate to put/removeGet/get so the key
 * filter, the exclusion filter and the data size accounting of the store apply to every
 * registration.
 *
 * @param <K> the key type used to look for a value
 * @param <V> the value type to be registered
 * @param <T> the type returned by the registration methods, typically the concrete subclass
 *            to allow method chaining
 */
public abstract class RegistrarMap<K, V, T>
        extends KVMapStoreDefault<K, V>
        implements Registrar<K, V, T> {


    /**
     * Create a registrar backed by the given map, the map choice defines the behavior of
     * the registrar: HashMap, LinkedHashMap, TreeMap...
     *
     * @param map the backing map, can't be null
     */
    public RegistrarMap(Map<K, V> map) {
        super(map);
    }

    /**
     * Register a key to value via the store put, an excluded key is silently ignored
     *
     * @param key   that will be used to look for
     * @param value to be associated with the key
     * @return the registrar itself for ease of use
     */
    @Override
    public T register(K key, V value) {
        put(key, value);
        return (T) this;
    }

    /**
     * Set the decoder that extracts the key K from a value V, used by registerValue and
     * unregisterValue
     *
     * @param dd the interface that extracts the key
     * @return the registrar itself for ease of use
     */
    @Override
    public T setValueKeyDecoder(DataDecoder<V, K> dd) {
        valueToKey = dd;
        return (T) this;
    }

    /**
     * Set the name and description of the registrar, exposed via getName and getDescription
     *
     * @param nd to be set
     * @return the registrar itself for ease of use
     */
    @Override
    public T setNamedDescription(NamedDescription nd) {
        namedDescription = nd;
        return (T) this;
    }

    /**
     * @return the decoder that extracts the key K from a value V, null if not set
     */
    @Override
    public DataDecoder<V, K> getValueKeyDecoder() {
        return valueToKey;
    }


    /**
     * Remove the key value from the registrar
     *
     * @param key of the value to be removed
     * @return the removed registered value, null if it does not exist
     */
    @Override
    public V unregister(K key) {
        return removeGet(key);
    }

    /**
     * Look for a registered value
     *
     * @param key   to look for
     * @param <VAL> the expected value type, a subtype of V
     * @return the found value, null if the key is not registered
     */
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


    /**
     * Map a single value to multiple keys
     *
     * @param value to be mapped, can't be null
     * @param keys  to be associated with the value
     * @return the registrar itself for ease of use
     */
    public T map(V value, K... keys) {
        SUS.checkIfNull("value null", value);
        for (K k : keys)
            register(k, value);
        return (T) this;
    }
}
