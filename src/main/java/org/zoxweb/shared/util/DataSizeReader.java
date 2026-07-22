package org.zoxweb.shared.util;

/**
 * Reads the data size in bytes of a value, used by KVMapStore implementations to maintain
 * the data size accounting of the store.
 *
 * @param <V> the value type to be sized
 */
public interface DataSizeReader<V> {
    /**
     * Return the size in bytes of a value
     *
     * @param v the value to be sized, can be null
     * @return the size in bytes, implementations MUST return 0 if v is null
     */
    long size(V v);
}
