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
package org.zoxweb.shared.http;

import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.SharedUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of HTTP request methods as defined in RFC 7231 (HTTP/1.1 Semantics).
 * Includes standard methods (GET, POST, PUT, DELETE, etc.) and WebDAV extensions.
 * <p>
 * Each method has a name (the HTTP method string) and a value (the corresponding
 * servlet method name like "doGet", "doPost").
 * </p>
 *
 * @author javaconsigliere
 */
public enum HTTPMethod
implements GetNameValue<String>
{
	
	//  "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE"
	
	
	GET("GET", "doGet"),
	POST("POST", "doPost"),
	HEAD("HEAD", "doHead"),
	OPTIONS("OPTIONS", "doOptions"),
	PUT("PUT", "doPut"),
	DELETE("DELETE", "doDelete"),
	TRACE("TRACE", "doTrace"),
	CONNECT("CONNECT", "doConnect"),
	// This is a new method crucial for update support
	PATCH("PATCH", "doPatch"),
	COPY("COPY", "doCopy"),
	LINK("LINK", "doLink"),
	UNLINK("UNLINK", "doUnlink"),
	PURGE("PURGE", "doPurge"),
	LOCK("LOCK", "doLock"),
	UNLOCK("UNLOCK", "doUnlock"),
	PROPFIND("PROPFIND", "doPropFind"),
	VIEW("VIEW", "doView")
	;

	private final String name;
	private final String value;
	private static final Map<String, HTTPMethod> fastLookup = new HashMap<>();
	
	HTTPMethod(String name, String value)
	{
		this.name= name;
		this.value = value;

	}
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}
	@Override
	public String getValue() 
	{
		// TODO Auto-generated method stub
		return value;
	}
	
	/**
	 * Looks up an HTTP method by name or servlet method name.
	 *
	 * @param lookFor the method name to look up
	 * @return the matching HTTPMethod, or null if not found
	 */
	public static HTTPMethod lookup(String lookFor)
	{
		return SharedUtil.lookupEnum(lookFor, values());
	}

	/**
	 * Fast lookup of HTTP method by name using a cached map.
	 *
	 * @param lookFor the method name to look up
	 * @return the matching HTTPMethod, or null if not found
	 */
	public static HTTPMethod lookupHTTPMethod(String lookFor)
	{
		if (fastLookup.isEmpty())
		{
			synchronized (fastLookup)
			{
				// prevent double penetration
				if (fastLookup.isEmpty())
				{
					for(HTTPMethod httpMethod : HTTPMethod.values())
						fastLookup.put(httpMethod.getName().toLowerCase(), httpMethod);
				}
			}
		}
		return fastLookup.get(lookFor.toLowerCase());
	}
	
	/**
	 * Returns an array of all HTTP method names.
	 *
	 * @return array of method name strings
	 */
	public static String[] toMethodNames()
	{

		HTTPMethod[] all = values();
		String[] ret= new String[all.length];
		for (int i=0; i< all.length; i++)
		{
			ret[i] = all[i].getName();
		}

		return ret;
	}
}
