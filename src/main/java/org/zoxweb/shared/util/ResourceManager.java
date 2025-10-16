package org.zoxweb.shared.util;

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


    private final RegistrarMapDefault<Object, Object> resources = new RegistrarMapDefault<>(ResourceManager::keyMap, null);
    public static final ResourceManager SINGLETON = new ResourceManager();

    //private final Map<Object, Object> resources = new LinkedHashMap<>();

    private ResourceManager() {
        register(Resource.SYSTEM_INFO, new NVGenericMap(Resource.SYSTEM_INFO.getName()));
    }

    public synchronized Object[] resources() {
        return resources.entrySet().toArray();
    }


    private static Object keyMap(Object key) {
        if (key instanceof GetName)
            return ((GetName) key).getName();
        return key;
    }

    /**
     *
     * @return
     */
    @Override
    public DataDecoder<Object, Object> getValueToKeyDecoder() {
        return resources.getValueToKeyDecoder();
    }

    /**
     *
     * @param nd
     * @return
     */
    @Override
    public ResourceManager setNamedDescription(NamedDescription nd) {
        resources.setNamedDescription(nd);
        return this;
    }

    @Override
    public synchronized ResourceManager register(Object key, Object value) {
        resources.register(key, value);
        return this;
    }

    @Override
    public synchronized Object unregister(Object key) {
        return resources.remove(key);
    }

    @Override
    public <V> V lookup(Object key) {
        return resources.lookup(key);
    }

    /**
     *
     * @param value
     * @param keys
     * @return
     */
    @Override
    public ResourceManager map(Object value, Object... keys) {
        resources.map(value, keys);
        return this;
    }


    @SuppressWarnings("unchecked")
    public static <V> V lookupResource(Object k) {
        return (V) SINGLETON.lookup(k);
    }


}
