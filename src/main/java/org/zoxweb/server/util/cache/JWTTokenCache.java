package org.zoxweb.server.util.cache;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.task.TaskDefault;
import org.zoxweb.server.task.TaskEvent;
import org.zoxweb.server.task.TaskSchedulerProcessor;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.DateUtil;
import org.zoxweb.shared.security.JWT;
import org.zoxweb.shared.security.JWTToken;
import org.zoxweb.shared.util.Const.TimeInMillis;
import org.zoxweb.shared.util.DataEncoder;
import org.zoxweb.shared.util.KVMapStore;
import org.zoxweb.shared.util.KVMapStoreDefault;
import org.zoxweb.shared.util.SUS;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * One time use JWT cache that prevents token replay attacks, a token is registered by its
 * hash and stays cached for the duration of its validity window, registering the same token
 * twice within that window is a replay and is rejected with a SecurityException.
 * <p>Expired entries are evicted by a cleaner task queued on the task scheduler at
 * registration time, so the cache content self purges without any polling.
 */
public class JWTTokenCache
        implements KVMapStore<String, JWT> {

    /**
     * Scheduled per registered token, removes the token hash from the cache once its
     * validity window has elapsed
     */
    private class CacheCleanerTask extends TaskDefault {

        @Override
        protected void childExecuteTask(TaskEvent event) {
            String hash = (String) event.getTaskExecutorParameters()[0];
            cache.remove(hash);
            //log.info(Thread.currentThread() + " pending tokens: " + size());
        }

    }


    private static final LogWrapper log = new LogWrapper(JWTTokenCache.class.getName());
    private final Lock lock = new ReentrantLock();
    private KVMapStore<String, JWT> cache = new KVMapStoreDefault<String, JWT>(new HashMap<String, JWT>(), new HashSet<String>());
    private final long expirationPeriod;
    private volatile TaskSchedulerProcessor tsp;
    private volatile CacheCleanerTask cct;

    /**
     * Create a cache with a 5 minutes expiration period using the default task scheduler
     */
    public JWTTokenCache() {
        this(5 * TimeInMillis.MINUTE.MILLIS, TaskUtil.defaultTaskScheduler());
    }

    /**
     * Create a cache
     *
     * @param expirationPeriod the token validity window in millis, a token older than this
     *                         period is rejected, must be &gt; 0
     * @param tsp              the task scheduler used to queue the expired token eviction
     *                         tasks, can't be null
     */
    public JWTTokenCache(long expirationPeriod, TaskSchedulerProcessor tsp) {
        SUS.checkIfNulls("TaskScheduler null", tsp);

        if (expirationPeriod <= 0) {
            throw new IllegalArgumentException("invalid expiration period <= 0 " + expirationPeriod);
        }

        cct = new CacheCleanerTask();
        this.expirationPeriod = expirationPeriod;
        this.tsp = tsp;
        log.getLogger().info("ExpirationPeriod " + TimeInMillis.toString(expirationPeriod) + ", TaskScheduler:" + tsp);
    }


    /**
     * Register a token by its own hash
     *
     * @param jwtToken the token to register
     * @return true if the token was registered
     * @throws SecurityException if the token is expired or already registered, replay attack
     */
    public boolean map(JWTToken jwtToken) {
        return put(jwtToken.getJWT().getHash(), jwtToken.getJWT());
    }

    /**
     * Register a jwt by its own hash
     *
     * @param jwt the jwt to register
     * @return true if the jwt was registered
     * @throws SecurityException if the jwt is expired or already registered, replay attack
     */
    public boolean map(JWT jwt) {
        return put(jwt.getHash(), jwt);
    }


    /**
     * Register a jwt, the jwt issued at time must be within the expiration period of the
     * cache and the hash must not be already registered, on success an eviction task is
     * queued to remove the entry once the jwt validity window has elapsed
     *
     * @param jwtHash the jwt hash used as key
     * @param jwt     to be registered
     * @return true if the jwt was registered
     * @throws SecurityException if the jwt is expired or the hash is already registered,
     *                           replay attack
     */
    @Override
    public boolean put(String jwtHash, JWT jwt)
            throws SecurityException {

        long issuedAtInMillis = jwt.getPayload().getIssuedAt() * 1000;
        long delta = Math.abs(System.currentTimeMillis() - issuedAtInMillis);


        if (delta >= expirationPeriod) {
            throw new SecurityException("Expired token issued at " + DateUtil.DEFAULT_GMT_MILLIS.format(new Date(issuedAtInMillis)));
        }


        boolean ret;
        try {
            lock.lock();
            if (cache.get(jwtHash) != null) {
                // otp replay
                throw new SecurityException("Token already used, replay attack.");
            }

            // register the token
            ret = cache.put(jwtHash, jwt);

            tsp.queue(this, expirationPeriod + delta, cct, jwtHash);

        } finally {
            lock.unlock();
        }

        return ret;
    }

    @Override
    public JWT get(String jwtHash) {
        return cache.get(jwtHash);
    }

    @Override
    public boolean remove(String jwtHash) {
        return cache.remove(jwtHash);
    }

    @Override
    public boolean containsKey(String key) {
        return cache.containsKey(key);
    }

    @Override
    public JWT removeGet(String jwtHash) {
        return cache.removeGet(jwtHash);
    }

    @Override
    public void clear(boolean all) {
        cache.clear(all);
    }

    @Override
    public Iterator<String> exclusions() {
        return cache.exclusions();
    }

    @Override
    public Iterator<JWT> values() {
        return cache.values();
    }

    @Override
    public Iterator<String> keys() {
        return cache.keys();
    }

    @Override
    public Set<Map.Entry<String, JWT>> entrySet() {
        return cache.entrySet();
    }

    @Override
    public <VAL extends  KVMapStore<String, JWT>> VAL exclude(String exclusion) {
        cache.exclude(exclusion);
        return (VAL)this;
    }

    /**
     * Check in the key is in the exclusion set
     *
     * @param key to be checked
     * @return true if the key belongs to the exclusion set
     */
    @Override
    public boolean isExcluded(String key) {
        return cache.isExcluded(key);
    }

    /**
     * Remove a key, from the exclusion set.
     *
     * @param exclusionToRemove the key to be removed from the exclusion set, filtered by the key filter if set
     */
    @Override
    public <VAL extends  KVMapStore<String, JWT>> VAL include(String exclusionToRemove) {
        cache.include(exclusionToRemove);
        return (VAL)this;
    }

    /**
     * Set the key filter of the underlying store
     *
     * @param filter to be applied to the jwt hash keys, null to disable filtering
     * @param <VAL>  the implementing store type
     * @return the underlying store for ease of use
     */
    @Override
    public <VAL extends  KVMapStore<String, JWT>> VAL setKeyFilter(DataEncoder<String, String> filter) {
        return cache.setKeyFilter(filter);
    }

    /**
     * @return the current key filter, null if not set
     */
    @Override
    public DataEncoder<String, String> getKeyFilter() {
        return cache.getKeyFilter();
    }

    @Override
    public int size() {
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


    /**
     * @return the token validity window in millis set at construction
     */
    public long defaultExpirationPeriod() {
        return expirationPeriod;
    }

    /**
     * @return the cache description
     */
    @Override
    public String getDescription() {
        return "";
    }

    /**
     * @return the cache name, JWTCache
     */
    @Override
    public String getName() {
        return "JWTCache";
    }

}
