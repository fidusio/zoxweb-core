package org.zoxweb.server.util.cache;

import org.zoxweb.server.logging.LogWrapper;

import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.event.*;
import java.util.concurrent.atomic.AtomicInteger;




public class JCacheListener<K, V>
implements CacheEntryCreatedListener<K, V>,
		   CacheEntryUpdatedListener<K,V>,
		   CacheEntryRemovedListener<K,V>,
		   CacheEntryExpiredListener<K,V>,
		   CacheEntryEventFilter<K, V> 
{
	public static final LogWrapper log = new LogWrapper(JCacheListener.class);
	private final AtomicInteger counter = new AtomicInteger(0);

	@Override
	public void onCreated(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
			throws CacheEntryListenerException
	{
		// TODO Auto-generated method stub
		counter.addAndGet((int)events.spliterator().estimateSize());
		if(log.isEnabled()) log.getLogger().info(""+counter.get());
		
	}

	@Override
	public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
			throws CacheEntryListenerException
	{
		// TODO Auto-generated method stub
		if(log.isEnabled()) log.getLogger().info(""+counter.get());
	}

	@Override
	public void onExpired(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
			throws CacheEntryListenerException {
		// TODO Auto-generated method stub
		counter.addAndGet(-(int)events.spliterator().estimateSize());
		if(log.isEnabled()) log.getLogger().info(""+counter.get());
	}

	@Override
	public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
			throws CacheEntryListenerException 
	{
		// TODO Auto-generated method stub
		counter.addAndGet(-(int)events.spliterator().estimateSize());
		if(log.isEnabled()) log.getLogger().info(""+counter.get());
	}
	
	public int size() 
	{
		return counter.get();
	}
	
	@Override
	public boolean evaluate(CacheEntryEvent<? extends K, ? extends V> event) throws CacheEntryListenerException {
		// TODO Auto-generated method stub
		return true;
	}
	
	public static <K,V> CacheEntryListenerConfiguration<K, V> toConfiguration() {
		return toConfiguration(new JCacheListener<K,V>());
	  }
	
	
	public static <K,V> CacheEntryListenerConfiguration<K, V> toConfiguration(JCacheListener<K,V> listener) {
		return new MutableCacheEntryListenerConfiguration<K,V>(
				new FactoryBuilder.SingletonFactory<CacheEntryListener<K, V>>(listener),
				new FactoryBuilder.SingletonFactory<CacheEntryEventFilter<K, V>>(listener),
				false,
				true);
		

	  
	  }


}
