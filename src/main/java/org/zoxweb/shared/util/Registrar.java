package org.zoxweb.shared.util;

import java.util.Map;
import java.util.function.Function;

/**
 * A registrar is a key to value registry, the registration methods return the registrar
 * itself to allow chaining of calls.
 *
 * @param <K> the key type used to look for a value
 * @param <V> the value type to be registered
 * @param <T> the type returned by the registration methods, typically the implementing class
 *            to allow method chaining
 */
public interface Registrar<K, V, T> {


    /**
     * Value decoder is with registerValue and unregisterValue
     * it is a helper to extract the key K from the value object
     *
     * @param vkDecoder the interface that extracts the key
     * @return T
     */
    default T setValueKeyDecoder(DataDecoder<V, K> vkDecoder) {
        throw new NotSupportedException("setValueToKeyDecoder not implemented");
    }

    /**
     * @return the decoder that extracts the key K from a value V, null if not set
     */
    DataDecoder<V, K> getValueKeyDecoder();

    /**
     * Set name description of the registrar
     *
     * @param nd to be set
     * @return T for ease of use
     */
    T setNamedDescription(NamedDescription nd);


    /**
     * @return the underlying map holding the registered key value pairs
     */
    Map<K, V> getCacheMap();


    /**
     * Register a key to value
     *
     * @param key   that will be used to look for
     * @param value to be associated with the key
     * @return T for ease of use
     */
    T register(K key, V value);

    /**
     * Remove the key value from the registrar
     *
     * @param key of the value to be removed
     * @return the removed registered value, null if it does not exist
     */
    V unregister(K key);

    /**
     * unregister a value based on the value key decoder
     *
     * @param v to be removed
     * @return the removed registered value, null if it does not exist
     */
    default V unregisterValue(V v) {
        return unregister(getValueKeyDecoder().decode(v));
    }


    /**
     * register a value based on the value key decoder
     *
     * @param v to be registered, its key is extracted by the value key decoder
     * @return T for ease of use
     */
    default T registerValue(V v) {
        return register(getValueKeyDecoder().decode(v), v);
    }

    /**
     * @param key   to look for
     * @param <VAL> the expected value type, a subtype of V
     * @return the found value, null if the key is not registered
     */
    <VAL extends V> VAL lookup(K key);

    /**
     * Look for a value and create it on demand, if the key is already registered the currently
     * registered value is returned and the mapping function is not invoked, otherwise the mapping
     * function is applied to the key and the result, if not null, is registered and returned.
     * The default implementation delegates to the cache map computeIfAbsent, the key is normalized
     * via toKey, implementations that wrap another registrar must override and delegate instead.
     *
     * @param key             to look for
     * @param mappingFunction applied to the key to create the value if it is not registered yet
     * @param <VAL>           the expected value type, a subtype of V
     * @return the registered value, null if the key was not registered and the mapping function
     * returned null
     */
    @SuppressWarnings("unchecked")
    default <VAL extends V> VAL lookup(K key, Function<? super K, ? extends V> mappingFunction) {
        return (VAL) getCacheMap().computeIfAbsent(toKey(key), mappingFunction);
    }

    /**
     * Normalize a key prior to using it against the cache map, the default implementation returns
     * the key as is, implementations that filter or convert their keys must override it to keep
     * the direct cache map access consistent with register and lookup
     *
     * @param key to be normalized
     * @return the normalized key
     */
    default K toKey(K key) {
        return key;
    }


    /**
     * This is a utility function to map a value to multiple keys
     *
     * @param value to be mapped
     * @param keys  to be associated
     * @return T for ease of use
     */
    T map(V value, K... keys);
}
