package org.zoxweb.shared.util;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class KVMapStoreDefault<K,V>
implements KVMapStore<K,V>
{

	private final Map<K, V> mapCache;
	protected final Set<K> exclusionFilter;
	protected final DataSizeReader<V> sizeReader;
	protected final AtomicLong dataSize = new AtomicLong();
	protected DataEncoder<K,K> keyFilter = null;


	public KVMapStoreDefault(Map<K,V> map)
	{
		this(map, null, null);
	}
	
	public KVMapStoreDefault(Map<K,V> map, Set<K> eFilter)
	{
		this(map, eFilter, null);
	}

	public KVMapStoreDefault(Map<K,V> map, Set<K> eFilter, DataSizeReader<V> sizeReader)
	{
		SUS.checkIfNulls("Values can't be null", map);
		this.mapCache = map;
		this.exclusionFilter = eFilter != null ? eFilter : new HashSet<K>();
		this.sizeReader = sizeReader;
	}

	@Override
	public synchronized boolean put(K key, V value)
	{
		if (key != null && value != null)
		{
			key = toKey(key);

			if (!exclusionFilter.contains(key))
			{
				V oldValue = mapCache.put(key, value);
				if(sizeReader != null) {
					long toSubtract = sizeReader.size(oldValue);
					long toAdd = sizeReader.size(value);
					dataSize.getAndAdd(toAdd - toSubtract);
				}
				return true;
			}
		}

		return false;
	}

	private K toKey(K key)
	{
		return keyFilter != null ? keyFilter.encode(key) : key;
	}
	
//	@Override
//	public  boolean map(K key, V value)
//	{
//		return put(key, value);
//	}

	@Override
	public synchronized V get(K key) {
		// TODO Auto-generated method stub
		return mapCache.get(toKey(key));
	}

	@Override
	public synchronized boolean remove(K key) {
		// TODO Auto-generated method stub
		V oldValue = mapCache.remove(toKey(key));
		if(sizeReader != null) {
			long toSubtract = sizeReader.size(oldValue);

			dataSize.getAndAdd(-toSubtract);
		}
		return (oldValue != null);
	}


	protected Map<K,V> getMapCache()
	{
		return mapCache;
	}


	public synchronized V removeGet(K key) {
		// TODO Auto-generated method stub
		V oldValue = mapCache.remove(toKey(key));
		if(sizeReader != null) {
			long toSubtract = sizeReader.size(oldValue);

			dataSize.getAndAdd(-toSubtract);
		}
		return oldValue;
	}

	@Override
	public synchronized void clear(boolean all) {
		// TODO Auto-generated method stub
		mapCache.clear();
		dataSize.set(0);
		if (all)
			exclusionFilter.clear();
		
	}

	@Override
	public synchronized Iterator<K> exclusions() 
	{
		// TODO Auto-generated method stub
		return exclusionFilter.iterator();
	}

	@Override
	public synchronized Iterator<V> values()
	{
		// TODO Auto-generated method stub
		return mapCache.values().iterator();
	}


    public synchronized  KVMapStore<K,V> map(V value, K ...keys)
    {
        SUS.checkIfNull("value null", value);
        for(K k : keys)
            mapCache.put(k, value);
        return this;
    }

	public synchronized Set<Map.Entry<K, V>> entrySet()
	{
		return mapCache.entrySet();
	}

	@Override
	public synchronized Iterator<K> keys()
	{
		// TODO Auto-generated method stub
		return mapCache.keySet().iterator();
	}

	@Override
	public synchronized void addExclusion(K exclusion)
	{
		exclusionFilter.add(toKey(exclusion));
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return mapCache.size();
	}

	@Override
	public long dataSize() {
		return dataSize.get();
	}

	@Override
	public long averageDataSize()
	{
		int size = size();
		if(size > 0) {
			return dataSize.get() / size;
		}

		return 0;
	}

	@Override
	public long defaultExpirationPeriod() {
		return 0;
	}



}
