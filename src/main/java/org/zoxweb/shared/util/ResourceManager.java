package org.zoxweb.shared.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResourceManager
        implements Registrar<Object, Object, ResourceManager> {


    public enum Resource
            implements GetName {
        API_SECURITY_MANAGER("APISecurityManager"),
        DATA_STORE("DataStore"),
        REALM_STORE("RealmStore"),
        API_APP_MANAGER("APIAppManager"),
        JWT_CACHE("JWTCache"),
        AUTH_TOKEN("AuthToken"),
        HTTP_SERVER("HttpServer"),
        PROXY_SERVER("ProxyServer"),
        SYSTEM_INFO("SystemInfo"),
        FILE_SYSTEM("FileSystem"),

        ;

        Resource(String name) {
            this.name = name;
        }

        private final String name;

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return name;
        }

    }


    public static final ResourceManager SINGLETON = new ResourceManager();

    private final Map<Object, Object> resources = new LinkedHashMap<>();

    private ResourceManager() {
        register(Resource.SYSTEM_INFO, new NVGenericMap(Resource.SYSTEM_INFO.getName()));
    }

    public synchronized Object[] resources() {
        return resources.values().toArray();
    }


    private Object keyMap(Object key) {
        if (key instanceof GetName)
            return ((GetName) key).getName();
        return key;
    }

    @Override
    public synchronized ResourceManager register(Object key, Object value) {
        resources.put(keyMap(key), value);
        return this;
    }

    @Override
    public synchronized Object unregister(Object key) {
        return resources.remove(keyMap(key));
    }

    @Override
    public <V> V lookup(Object key) {
        return (V) resources.get(keyMap(key));
    }


    @SuppressWarnings("unchecked")
    public static <V> V lookupResource(Object k) {
        return (V) SINGLETON.lookup(k);
    }


}
