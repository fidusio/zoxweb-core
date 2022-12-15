package org.zoxweb.shared.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResourceManager
{
	
	public enum Resource
		implements GetName
	{
		API_SECURITY_MANAGER("APISecurityManager"),
		DATA_STORE("DataStore"),
		REALM_STORE("RealmStore"),
		API_APP_MANAGER("APIAppManager"),
		JWT_CACHE("JWTCache"),
		AUTH_TOKEN("AuthToken"),
		HTTP_SERVER("HttpServer"),
		PROXY_SERVER("ProxyServer")
		
		;

		Resource(String name)
		{
			this.name= name;
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
	
	private ResourceManager()
	{
		
	}

	
	@SuppressWarnings("unchecked")
	public <V> V lookup(Object k)
    {
        return (V) resources.get(keyMap(k));
    }

	public synchronized <V> ResourceManager map(Object k, V res)
	{
		resources.put(keyMap(k), res);
		return this;
	}

	public synchronized Object [] resources()
	{
		return resources.values().toArray();
	}
	
	@SuppressWarnings("unchecked")
    public synchronized <V> V remove(Object key)
	{
		return (V) resources.remove(keyMap(key));
	}

	private Object keyMap(Object key)
	{
		if (key instanceof GetName)
			return ((GetName) key).getName();
		return key;
	}
}
