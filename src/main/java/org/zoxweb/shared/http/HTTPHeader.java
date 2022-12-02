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

import org.zoxweb.shared.security.SecurityConsts.OAuthParam;
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
	AUTHORIZATION(OAuthParam.AUTHORIZATION.getNVConfig().getDisplayName()),
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
	LOCATION("Location"),
	PROXY_AGENT("Proxy-Agent"),
	PROXY_CONNECTION("Proxy-Connection"),
	PROXY_AUTHENTICATE("Proxy-Authenticate"),
	PROXY_AUTHORIZATION("Proxy-Authorization"),
	SET_COOKIE("Set-Cookie"),
	TRANSFER_ENCODING("Transfer-Encoding"),
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
	
	public String toString()
	{
		return name;
	}
	
	
	public static GetNameValue<String> toHTTPHeader(GetName gn, GetValue<String> ...values)
	{
		return toHTTPHeader(gn.getName(), values);
	}
	
	
	public static GetNameValue<String> toHTTPHeader(GetName gn, String ...values)
	{
		return toHTTPHeader(gn.getName(), values);
	}
	
	
	public static GetNameValue<String> toHTTPHeader(String name, GetValue<String> ...values)
	{
		StringBuilder headerValue = new StringBuilder();

		if(values != null)
		{
			for(int i=0; i<values.length; i++)
			{
				if (headerValue.length() > 0)
					headerValue.append("; ");

				if (values[i] != null && !SharedStringUtil.isEmpty(values[i].getValue()))
					headerValue.append(values[i].getValue());

			}
		}

		return new NVPair(name, headerValue.toString());
	}
	
	
	public static GetNameValue<String> toHTTPHeader(String name, String ...values)
	{
		StringBuilder headerValue = new StringBuilder();

		if(values != null)
		{
			for(int i=0; i<values.length; i++)
			{
				if (headerValue.length() > 0)
					headerValue.append("; ");

				if (!SharedStringUtil.isEmpty(values[i]))
					headerValue.append(values[i]);

			}
		}

		return new NVPair(name, headerValue.toString());
	}

}
