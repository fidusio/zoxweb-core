package org.zoxweb.server.util.cache;

import org.zoxweb.server.task.TaskDefault;
import org.zoxweb.server.task.TaskEvent;
import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.DateUtil;
import org.zoxweb.shared.security.JWT;
import org.zoxweb.shared.security.JWTToken;
import org.zoxweb.shared.util.Const.TimeInMillis;
import org.zoxweb.shared.util.KVMapStore;
import org.zoxweb.shared.util.KVMapStoreDefault;
import org.zoxweb.shared.util.SUS;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class JWTTokenCache
implements KVMapStore<String, JWT>
{
	
	
	
	private static final Logger log = Logger.getLogger(JWTTokenCache.class.getName());
	private Lock lock = new ReentrantLock();
	private class CacheCleanerTask extends TaskDefault
	{

		@Override
		protected void childExecuteTask(TaskEvent event) 
		{
			// TODO Auto-generated method stub
			String hash = (String) event.getTaskExecutorParameters()[0];
			cache.remove(hash);
			//log.info(Thread.currentThread() + " pending tokens: " + size());
		}
		
	}
	
	
	private KVMapStore<String, JWT> cache = new KVMapStoreDefault<String, JWT>(new HashMap<String, JWT>(), new HashSet<String>());
	private long expirationPeriod;
	private volatile TaskSchedulerProcessor tsp;
	private volatile CacheCleanerTask cct;
	
	public JWTTokenCache()
	{
		this(5*TimeInMillis.MINUTE.MILLIS, TaskUtil.defaultTaskScheduler());
	}
	
	public JWTTokenCache(long expirationPeriod, TaskSchedulerProcessor tsp)
	{
		SUS.checkIfNulls("TaskScheduler null", tsp);
		
		if (expirationPeriod <= 0)
		{
			throw new IllegalArgumentException("invalid expiration period <= 0 " + expirationPeriod);
		}
		
		cct = new CacheCleanerTask();
		this.expirationPeriod = expirationPeriod;
		this.tsp = tsp;
		log.info("ExpirationPeriod " + TimeInMillis.toString(expirationPeriod) + ", TaskScheduler:" + tsp);
	}

	
	
	public boolean map(JWTToken jwtToken)
	{
		return put(jwtToken.getJWT().getHash(), jwtToken.getJWT());
	}
	
	public boolean map(JWT jwt)
	{
		return put(jwt.getHash(), jwt);
	}




	@Override
	public boolean put(String jwtHash, JWT jwt)
		throws SecurityException
	{
		
	    long issuedAtInMillis = jwt.getPayload().getIssuedAt() * 1000;
		long delta = Math.abs(System.currentTimeMillis() - issuedAtInMillis);
		
		
		if (delta >= expirationPeriod)
		{
			throw new SecurityException("Expired token issued at " + DateUtil.DEFAULT_GMT_MILLIS.format(new Date(issuedAtInMillis)));
		}
		
		
		boolean ret;
		try
		{
		    lock.lock();
			if (cache.get(jwtHash) != null)
			{
				// otp replay
				throw new SecurityException("Token already used, replay attack.");
			}
			
			// register the token
			ret = cache.put(jwtHash, jwt);
			
			tsp.queue(this, expirationPeriod + delta, cct, jwtHash);
			
		}
		finally
		{
		  lock.unlock();
		}
		
		// TODO Auto-generated method stub
		return ret;
	}

	@Override
	public JWT get(String jwtHash) {
		// TODO Auto-generated method stub
		return cache.get(jwtHash);
	}

	@Override
	public boolean remove(String jwtHash) {
		// TODO Auto-generated method stub
		return cache.remove(jwtHash);
	}

	@Override
	public JWT removeGet(String jwtHash) {
		// TODO Auto-generated method stub
		return cache.removeGet(jwtHash);
	}

	@Override
	public void clear(boolean all) {
		// TODO Auto-generated method stub
		cache.clear(all);
	}

	@Override
	public Iterator<String> exclusions() {
		// TODO Auto-generated method stub
		return cache.exclusions();
	}

	@Override
	public Iterator<JWT> values() {
		// TODO Auto-generated method stub
		return cache.values();
	}

	@Override
	public Iterator<String> keys() {
		// TODO Auto-generated method stub
		return cache.keys();
	}

	@Override
	public Set<Map.Entry<String, JWT>> entrySet() {
		return cache.entrySet();
	}

	@Override
	public void addExclusion(String exclusion) {
		// TODO Auto-generated method stub
		cache.addExclusion(exclusion);;
	}

    /**
     * @param value
     * @param keys
     * @return
     */
    @Override
    public KVMapStore<String, JWT> map(JWT value, String... keys) {
        return cache.map(value, keys);
    }

    @Override
	public int size() {
		// TODO Auto-generated method stub
		return cache.size();
	}

	@Override
	public long dataSize() {
		return cache.dataSize();
	}

	@Override
	public long averageDataSize() {
		return cache.averageDataSize();
	}


	public long defaultExpirationPeriod()
	{
		return expirationPeriod;
	}
	
}
