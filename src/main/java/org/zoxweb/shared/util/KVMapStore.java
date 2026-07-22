/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.shared.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A key to value store contract, it behaves like a map with 3 additional features:
 * <ol>
 * <li>an exclusion filter, an excluded key can never be stored</li>
 * <li>an optional key filter that normalizes every key before it is used, ie: case
 * insensitive keys</li>
 * <li>optional data size accounting in bytes of the stored values</li>
 * </ol>
 *
 * @param <K> the key type used to store and look for a value
 * @param <V> the value type to be stored
 */
public interface KVMapStore<K, V>
extends GetName,GetDescription{


    /**
     * Associate a value with a key
     *
     * @param key   that will be used to look for, filtered by the key filter if set
     * @param value to be associated with the key
     * @return true if the value was stored, false if the key or value is null or the key
     * is excluded
     */
    boolean put(K key, V value);

    /**
     * Look for the value associated with a key
     *
     * @param key to look for
     * @return the associated value, null if the key is not stored
     */
    V get(K key);

    /**
     * Remove a key value association
     *
     * @param key of the value to be removed
     * @return true if a value was actually removed
     */
    boolean remove(K key);

    /**
     * Remove a key value association and return the removed value
     *
     * @param key of the value to be removed
     * @return the removed value, null if the key was not stored
     */
    V removeGet(K key);

    /**
     * Remove all the stored key value associations and reset the data size accounting
     *
     * @param all if true the exclusion filter is cleared too
     */
    void clear(boolean all);

    /**
     * @return iterator over the currently excluded keys
     */
    Iterator<K> exclusions();

    /**
     * @return iterator over the currently stored values
     */
    Iterator<V> values();

    /**
     * @return iterator over the currently stored keys
     */
    Iterator<K> keys();

    /**
     * @return the entry set of the underlying map
     */
    Set<Map.Entry<K, V>> entrySet();

    /**
     * Exclude a key, an excluded key is rejected by any subsequent put, the values already
     * stored are not affected
     *
     * @param exclusion the key to be excluded, filtered by the key filter if set
     */
    void addExclusion(K exclusion);

    /**
     * Check if a key is currently stored
     *
     * @param key to look for
     * @return true if the key has an associated value
     */
    boolean containsKey(K key);

    /**
     * Set the key filter, once set every key is converted by the filter prior to any usage,
     * ie: a lower case encoder makes the store case insensitive
     *
     * @param filter to be applied to the keys, null to disable filtering
     * @param <VAL>  the implementing store type
     * @return the store itself for ease of use
     */
    <VAL extends KVMapStore<K, V>> VAL setKeyFilter(DataEncoder<K,K> filter);

    /**
     * @return the current key filter, null if not set
     */
    DataEncoder<K, K> getKeyFilter();




    /**
     * Return the count of objects stored by the key value store
     *
     * @return the number of stored key value associations
     */
    int size();

    /**
     * If enabled, return the amount in bytes of the stored values
     *
     * @return the total data size in bytes, 0 if data size accounting is not enabled
     */
    long dataSize();

    /**
     * @return the average value size in bytes, dataSize() divided by size(), 0 if the store
     * is empty or data size accounting is not enabled
     */
    long averageDataSize();

    /**
     * @return the default expiration period in millis of a stored value, 0 if the store
     * does not expire its content
     */
    long defaultExpirationPeriod();

}