package org.zoxweb.shared.util;

public interface Registrar<K, V, T> {


    /**
     * Value decoder is with registerValue and unregisterValue
     * it is a helper to extract the key K from the value object
     * @param vkDecoder the interface that extracts the key
     * @return T
     */
    default T setValueKeyDecoder(DataDecoder<V, K> vkDecoder) {
        throw new NotSupportedException("setValueToKeyDecoder not implemented");
    }

    DataDecoder<V, K> getValueKeyDecoder();

    /**
     * Set name description of the registrar
     * @param nd to be set
     * @return T for ease of use
     */
    T setNamedDescription(NamedDescription nd);

    /**
     * Register a key to value
     * @param key that will be used to look for
     * @param value to be associated with the key
     * @return T for ease of use
     */
    T register(K key, V value);

    /**
     * Remove the key value from the registrar
     * @param key of the value to be removed
     * @return the removed registered value, null if it does not exist
     */
    V unregister(K key);

    /**
     * unregister a value based on the value key decoder
     * @param v to be removed
     * @return the removed registered value, null if it does not exist
     */
    default V unregisterValue(V v) {
        return unregister(getValueKeyDecoder().decode(v));
    }


    /**
     * register a value based on the value key decoder
     * @param v to be removed
     * @return the removed registered value, null if it does not exist
     */
    default T registerValue(V v) {
        return register(getValueKeyDecoder().decode(v), v);
    }

    /**
     * @param key to look for
     * @return the found value
     * @param <VAL>
     */
    <VAL extends V> VAL lookup(K key);


    /**
     * This is a utility function to map a value to multiple keys
     * @param value to be mapped
     * @param keys to be associated
     * @return T for ease of use
     */
    T map(V value, K ...keys);
}
