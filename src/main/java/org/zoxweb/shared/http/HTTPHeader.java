/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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

import org.zoxweb.shared.util.*;

public enum HTTPHeader
    implements GetName
{
	ACCEPT("Accept"),
	ACCEPT_CHARSET("Accept-Charset"),
	ACCEPT_LANGUAGE("Accept-Language"),
	ACCEPT_DATETIME("Accept-Datetime"),
	ACCEPT_ENCODING("Accept-Encoding"),
	ACCESS_CONTROL_ALLOW_CREDENTIALS("Access-Control-Allow-Credentials"),
	ACCESS_CONTROL_ALLOW_ORIGIN("Access-Control-Allow-Origin"),
	AUTHORIZATION("Authorization"),
	CACHE_CONTROL("Cache-Control"),
	CONNECTION("Connection"),
	CONTENT_DISPOSITION("Content-Disposition"),
	CONTENT_ENCODING("Content-Encoding"),
	CONTENT_LENGTH("Content-Length"),
	CONTENT_LOCATION("Content-Location"),
	CONTENT_TRANSFER_ENCODING("Content-Transfer-Encoding"),
	CONTENT_TYPE("Content-Type"),
	COOKIE("Cookie"),
	DATE("Date"),
	ETAG("Etag"),
	EXPIRES("Expires"),
	FROM("From"),
	HOST("Host"),
	KEEP_ALIVE("Keep-Alive"),
	LAST_MODIFIED("Last-Modified"),
	LOCATION("Location"),

	PROXY_AGENT("Proxy-Agent"),
	PROXY_CONNECTION("Proxy-Connection"),
	PROXY_AUTHENTICATE("Proxy-Authenticate"),
	PROXY_AUTHORIZATION("Proxy-Authorization"),
	SEC_WEBSOCKET_ACCEPT("Sec-WebSocket-Accept"),
	SEC_WEBSOCKET_EXTENSIONS("Sec-WebSocket-Extensions"),
	SEC_WEBSOCKET_KEY("Sec-WebSocket-Key"),
	SEC_WEBSOCKET_PROTOCOL("Sec-WebSocket-Protocol"),
	SEC_WEBSOCKET_VERSION("Sec-WebSocket-Version"),
	SERVER("Server"),
	SET_COOKIE("Set-Cookie"),
	STRICT_TRANSPORT_SECURITY("Strict-Transport-Security"),
	TRANSFER_ENCODING("Transfer-Encoding"),
	UPGRADE("Upgrade"),
	USER_AGENT("User-Agent"),
	WWW_AUTHENTICATE("WWW-Authenticate"),
	X_ACCEPT_ENCODING("X-Accept-Encoding"),
	;

	
	
	private final String name;
	
	
	HTTPHeader(String n)
	{
		name = n;
	}
	
	
	@Override
	public String getName() 
	{
		// TODO Auto-generated method stub
		return name;
	}

	public GetNameValue<String> toHTTPHeader(String ...values)
	{
		return HTTPConst.toHTTPHeader(name, values);
	}

	public GetNameValue<String> toHTTPHeader(GetValue<String> ...values)
	{
		return HTTPConst.toHTTPHeader(name, values);
	}
	
	public String toString()
	{
		return name;
	}



}
