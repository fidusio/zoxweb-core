package org.zoxweb.shared.util;

public interface Registrar<K, V, T> {


    default T setValueToKeyDecoder(DataDecoder<V, K> dd) {
        throw new NotSupportedException("setValueToKeyDecoder not implemented");
    }

    DataDecoder<V, K> getValueToKeyDecoder();

    T setNamedDescription(NamedDescription nd);

    T register(K key, V value);

    V unregister(K key);

    default V unregisterValue(V v) {
        return unregister(getValueToKeyDecoder().decode(v));
    }

    default T registerValue(V v) {
        return register(getValueToKeyDecoder().decode(v), v);
    }

    <VAL extends V> VAL lookup(K key);


    T map(V value, K ...keys);
}
