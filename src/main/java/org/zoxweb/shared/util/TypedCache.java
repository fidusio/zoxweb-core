package org.zoxweb.shared.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class TypedCache {
    private final Map<String, Map<String, Object>> map = new LinkedHashMap<>();

    public TypedCache() {
    }

    /**
     * Register a new cache type
     * 
     * @param cacheType to be resgistered
     * @return this
     */
    public synchronized TypedCache registerType(String cacheType) {
        Map<String, Object> find = map.get(cacheType);
        if (find == null) {
            map.put(cacheType, new LinkedHashMap<>());
        }

        return this;
    }

    /**
     * Register a new cache type
     * 
     * @param cacheType to be resgistered
     * @return this
     */
    public TypedCache registerType(Enum<?> cacheType) {
        return registerType(SharedUtil.enumName(cacheType));
    }

    /**
     * Lookup and an object
     * 
     * @param <C>              casting type
     * @param cacheType        type in the cache
     * @param cachedObjectName name of the cached object
     * @return matching object or null
     */
    public <C> C lookupObject(Enum<?> cacheType, String cachedObjectName) {
        return lookupObject(SharedUtil.enumName(cacheType), cachedObjectName);
    }

    public <C> C lookupObject(Enum<?> cacheType, Enum<?> cachedObjectName) {
        return lookupObject(SharedUtil.enumName(cacheType), SharedUtil.enumName(cachedObjectName));
    }

    public <C> C lookupObject(String cacheType, String cachedObjectName) {
        C ret = null;
        Map<String, Object> find = map.get(cacheType);
        if (find != null) {
            ret = (C) find.get(cachedObjectName);
        }

        return ret;
    }

    public TypedCache addObject(Enum<?> cacheType, GetName objectToCache) {
        return addObject(SharedUtil.enumName(cacheType), objectToCache.getName(), objectToCache);
    }

    public TypedCache addObject(String cacheType, GetName objectToCache) {
        return addObject(cacheType, objectToCache.getName(), objectToCache);
    }

    public TypedCache addObject(Enum<?> cacheType, GetName cachedObjectName, Object objectToCache) {
        return addObject(SharedUtil.enumName(cacheType), cachedObjectName.getName(), objectToCache);
    }

    public TypedCache addObject(Enum<?> cacheType, String name, Object toCache) {
        return addObject(SharedUtil.enumName(cacheType), name, toCache);
    }

    public TypedCache addObject(String cacheType, String name, Object toCache) {
        Map<String, Object> findType = map.get(cacheType);
        if (findType == null) {
            registerType(cacheType);
            findType = map.get(cacheType);
        }

        findType.put(name, toCache);

        return this;
    }

    public TypedCache removeObject(Enum<?> cacheType, String cachedObjectName) {
        return removeObject(SharedUtil.enumName(cacheType), cachedObjectName);
    }

    public TypedCache removeObject(Enum<?> cacheType, GetName cachedObjectName) {
        return removeObject(SharedUtil.enumName(cacheType), cachedObjectName.getName());
    }

    public TypedCache removeObject(String cacheType, GetName cachedObjectName) {
        return removeObject(cacheType, cachedObjectName.getName());
    }

    public TypedCache removeObject(String cacheType, String cachedObjectName) {
        Map<String, Object> findType = map.get(cacheType);
        if (findType != null)
            findType.remove(cachedObjectName);

        return this;
    }

    public String[] getTypes() {
        return map.keySet().toArray(new String[0]);
    }

    public <T> Collection<T> getValues(Enum<?> type) {
        return getValues(SharedUtil.enumName(type));
    }

    public <T> Collection<T> getValues(String type) {
        if (map.get(type) != null)
            return (Collection<T>) map.get(type).values();

        return null;
    }

    public TypedCache clearCache() {
        for (String ct : map.keySet())
            map.get(ct).clear();
        return this;
    }

    @Override
    public String toString() {
        return "TypedCache{" +
                "map=" + map +
                '}';
    }
}
