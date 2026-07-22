package org.zoxweb.shared.util;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default KVMapStore implementation backed by a caller supplied map, the map choice defines
 * the behavior of the store: HashMap for a plain cache, LinkedHashMap for insertion order or
 * a bounded LRU cache, TreeMap for sorted keys.
 * <p>All the mutating and iterating methods are synchronized on the store instance, the data
 * size accounting is enabled by supplying a DataSizeReader at construction.
 *
 * @param <K> the key type used to store and look for a value
 * @param <V> the value type to be stored
 */
public class KVMapStoreDefault<K, V>
        implements KVMapStore<K, V> {

    /** The backing map supplied at construction, holds the key value associations */
    private final Map<K, V> mapCache;
    /** The excluded keys, a key present in this set is rejected by put */
    protected final Set<K> exclusionFilter;
    /** Reads the size in bytes of a value, null disables data size accounting */
    protected final DataSizeReader<V> sizeReader;
    /** The total size in bytes of the stored values, maintained by put, remove and clear */
    protected final AtomicLong dataSize = new AtomicLong();


    /** Converts every key prior to any usage, null disables key filtering */
    protected DataEncoder<K, K> keyFilter = null;
    /** Extracts the key from a value, used by Registrar based subclasses, null if not set */
    protected DataDecoder<V, K> valueToKey = null;
    /** The name and description of the store, exposed via getName and getDescription */
    protected NamedDescription namedDescription;


    /**
     * Create a store backed by the given map, no exclusions, no data size accounting
     *
     * @param map the backing map, can't be null
     */
    public KVMapStoreDefault(Map<K, V> map) {
        this(map, null, null);
    }

    /**
     * Create a store backed by the given map with an exclusion set, no data size accounting
     *
     * @param map     the backing map, can't be null
     * @param eFilter the exclusion set, if null an empty HashSet is used
     */
    public KVMapStoreDefault(Map<K, V> map, Set<K> eFilter) {
        this(map, eFilter, null);
    }

    /**
     * Create a store backed by the given map with an exclusion set and data size accounting
     *
     * @param map        the backing map, can't be null
     * @param eFilter    the exclusion set, if null an empty HashSet is used
     * @param sizeReader reads the size in bytes of a value to maintain the data size of the
     *                   store, null disables data size accounting
     */
    public KVMapStoreDefault(Map<K, V> map, Set<K> eFilter, DataSizeReader<V> sizeReader) {
        SUS.checkIfNulls("map can't be null", map);
        this.mapCache = map;
        this.exclusionFilter = eFilter != null ? eFilter : new HashSet<K>();
        this.sizeReader = sizeReader;
    }



    /**
     * Convert the key using the key filter if not null
     *
     * @param key to be converted
     * @return converted key, the key as is if no key filter is set
     */
    public K toKey(K key) {
        return keyFilter != null ? keyFilter.encode(key) : key;
    }


    /**
     * @return the backing map, direct usage bypasses the key filter, the exclusion filter
     * and the data size accounting
     */
    public Map<K, V> getCacheMap() {
        return mapCache;
    }

    /**
     * Check if a key is currently stored
     *
     * @param key to look for, filtered by the key filter if set
     * @return true if the key has an associated value
     */
    @Override
    public synchronized boolean containsKey(K key) {
        return mapCache.containsKey(toKey(key));
    }



    /**
     * Associate a value with a key and update the data size accounting if enabled
     *
     * @param key   that will be used to look for, filtered by the key filter if set
     * @param value to be associated with the key
     * @return true if the value was stored, false if the key or value is null or the key
     * is excluded
     */
    public final synchronized boolean put(K key, V value) {
        if (key != null && value != null) {
            key = toKey(key);

            if (!exclusionFilter.contains(key)) {
                V oldValue = mapCache.put(key, value);
                if (sizeReader != null) {
                    long toSubtract = sizeReader.size(oldValue);
                    long toAdd = sizeReader.size(value);
                    dataSize.getAndAdd(toAdd - toSubtract);
                }
                return true;
            }
        }

        return false;
    }


    /**
     * Look for the value associated with a key
     *
     * @param key to look for, filtered by the key filter if set
     * @return the associated value, null if the key is not stored
     */
    @Override
    public final synchronized V get(K key) {
        return mapCache.get(toKey(key));
    }

    /**
     * Remove a key value association and update the data size accounting if enabled
     *
     * @param key of the value to be removed, filtered by the key filter if set
     * @return true if a value was actually removed
     */
    @Override
    public synchronized boolean remove(K key) {
        V oldValue = mapCache.remove(toKey(key));
        if (sizeReader != null) {
            long toSubtract = sizeReader.size(oldValue);
            dataSize.getAndAdd(-toSubtract);
        }
        return (oldValue != null);
    }

    /**
     * Remove a key value association, update the data size accounting if enabled and
     * return the removed value
     *
     * @param key of the value to be removed, filtered by the key filter if set
     * @return the removed value, null if the key was not stored
     */
    public final synchronized V removeGet(K key) {
        V oldValue = mapCache.remove(toKey(key));
        if (sizeReader != null) {
            long toSubtract = sizeReader.size(oldValue);

            dataSize.getAndAdd(-toSubtract);
        }
        return oldValue;
    }

    /**
     * Remove all the stored key value associations and reset the data size accounting
     *
     * @param all if true the exclusion filter is cleared too
     */
    @Override
    public synchronized void clear(boolean all) {
        mapCache.clear();
        dataSize.set(0);
        if (all)
            exclusionFilter.clear();

    }

    /**
     * @return iterator over the currently excluded keys, iteration is not thread safe
     * versus concurrent modifications
     */
    @Override
    public synchronized Iterator<K> exclusions() {
        return exclusionFilter.iterator();
    }

    /**
     * @return iterator over the currently stored values, iteration is not thread safe
     * versus concurrent modifications
     */
    @Override
    public synchronized Iterator<V> values() {
        return mapCache.values().iterator();
    }


    /**
     * Set the key filter, once set every key is converted by the filter prior to any usage
     *
     * @param filter to be applied to the keys, null to disable filtering
     * @param <VAL>  the implementing store type
     * @return the store itself for ease of use
     */
    @Override
    @SuppressWarnings("unchecked")
    public <VAL extends KVMapStore<K, V>> VAL setKeyFilter(DataEncoder<K, K> filter) {
        this.keyFilter = filter;
        return (VAL)this;
    }



    /**
     * @return the current key filter, null if not set
     */
    @Override
    public DataEncoder<K, K> getKeyFilter() {
        return keyFilter;
    }

    /**
     * @return the entry set of the backing map, direct usage bypasses the key filter, the
     * exclusion filter and the data size accounting
     */
    public synchronized Set<Map.Entry<K, V>> entrySet() {
        return mapCache.entrySet();
    }

    /**
     * @return iterator over the currently stored keys, iteration is not thread safe
     * versus concurrent modifications
     */
    @Override
    public synchronized Iterator<K> keys() {
        return mapCache.keySet().iterator();
    }

    /**
     * Exclude a key, an excluded key is rejected by any subsequent put, the values already
     * stored are not affected
     *
     * @param exclusion the key to be excluded, filtered by the key filter if set
     */
    @Override
    public synchronized void addExclusion(K exclusion) {
        exclusionFilter.add(toKey(exclusion));
    }

    /**
     * Return the count of objects stored by the key value store, unsynchronized by design,
     * a fast monitoring read that can return a slightly stale count
     *
     * @return the number of stored key value associations
     */
    @Override
    public int size() {
        return mapCache.size();
    }

    /**
     * @return the total data size in bytes of the stored values, 0 if data size accounting
     * is not enabled
     */
    @Override
    public long dataSize() {
        return dataSize.get();
    }

    /**
     * @return the average value size in bytes, dataSize() divided by size(), 0 if the store
     * is empty or data size accounting is not enabled
     */
    @Override
    public long averageDataSize() {
        int size = size();
        if (size > 0) {
            return dataSize.get() / size;
        }

        return 0;
    }

    /**
     * @return 0, this store does not expire its content
     */
    @Override
    public long defaultExpirationPeriod() {
        return 0;
    }


    /**
     * @return the store description from the named description, null if not set
     */
    @Override
    public String getDescription() {
        return namedDescription != null ? namedDescription.getDescription() : null;
    }

    /**
     * @return the store name from the named description, null if not set
     */
    @Override
    public String getName() {
        return namedDescription != null ? namedDescription.getName() : null;
    }
}
