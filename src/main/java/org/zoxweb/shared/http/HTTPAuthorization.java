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

import org.zoxweb.shared.data.SetNameDAO;
import org.zoxweb.shared.util.*;

/**
 * @author mnael
 *
 */
@SuppressWarnings("serial")
public class HTTPAuthorization
extends SetNameDAO
{


	public static final NVConfig NVC_AUTH_SCHEME = NVConfigManager.createNVConfig("auth_scheme", null,"HTTPAuthScheme", false, true, HTTPAuthScheme.class);
	public static final NVConfig NVC_TOKEN = NVConfigManager.createNVConfig("token", null,"Token", false, true, NamedValue.class);
	//public static final NVConfig NVC_AUTH_SCHEME_OVERRIDE = NVConfigManager.createNVConfig("auth_scheme_override", null,"Token type override", false, true, String.class);
	public static final NVConfigEntity NVC_HTTP_AUTHORIZATION = new NVConfigEntityLocal("http_authorization", null , null, true, false, false, false, HTTPAuthorization.class, SharedUtil.toNVConfigList(NVC_AUTH_SCHEME, NVC_TOKEN), null, false, SetNameDAO.NVC_NAME_DAO);



	public HTTPAuthorization()
	{
		super(NVC_HTTP_AUTHORIZATION);
	}

	public HTTPAuthorization(HTTPAuthScheme type)
	{
		this();
		setAuthScheme(type);
	}


	public HTTPAuthorization(String type, String token)
	{
		this(HTTPAuthScheme.GENERIC);
		setName(type);
		setToken(token);
	}


	public HTTPAuthorization(String typePlusToken)
	{
		this(HTTPAuthScheme.GENERIC);


		typePlusToken = typePlusToken.trim();
		String scheme;
		int index = typePlusToken.indexOf(" ");
		if(index == -1)
			throw new IllegalArgumentException("Invalid token: " + typePlusToken);

		scheme = typePlusToken.substring(0, index);

		String tokenVal = typePlusToken.substring(index).trim();
		if(SUS.isEmpty(tokenVal))
			throw new IllegalArgumentException("Empty token value: " + tokenVal);


		setName(scheme);
		setToken(tokenVal);

		String[] pairs = tokenVal.split(",\\s*");
		NamedValue<?> token = lookup(NVC_TOKEN);
		for (String pair : pairs) {
			if (pair.isEmpty()) continue;
			String[] nv = pair.split("=", 2);
			String key = nv[0].trim();
			if(nv.length > 1)
			{
				String val = nv[1].trim();
				if (val.startsWith("\"") && val.endsWith("\"")) {
					val = val.substring(1, val.length() - 1);
				}
				token.getProperties().build(key, val);
			}
		}

	}



	public HTTPAuthorization(HTTPAuthScheme type, String token)
	{
		this(type);
		setToken(token);
	}

//	protected HTTPAuthorization(NVConfigEntity nvce)
//	{
//		super(nvce);
//	}

	protected HTTPAuthorization(NVConfigEntity nvce, HTTPAuthScheme type)
	{
		super(nvce);
		setAuthScheme(type);
	}


	/**
	 * @return the token
	 */
	public String getToken()
	{
		return (String) lookup(NVC_TOKEN).getValue();
	}
	/**
	 * @param token the token to set
	 */
	public void setToken(String token)
	{
		NVBase<String> nvb = lookup(NVC_TOKEN);
		nvb.setValue(token);
	}




	
	public HTTPAuthScheme getAuthScheme()
	{
		return lookupValue(NVC_AUTH_SCHEME);
	}

	public GetNameValue<String> toHTTPHeader()
	{
		return getAuthScheme().toHTTPHeader(getName(), getToken());
	}


	protected void setAuthScheme(HTTPAuthScheme type)
	{
		setValue(NVC_AUTH_SCHEME, type);
		if(type != HTTPAuthScheme.GENERIC)
			setName(type.getName());
	}

	public String toString()
	{
		return toHTTPHeader().getValue();
	}

//	public static HTTPAuthorization createBearer()
//	{
//		return new HTTPAuthorization(HTTPAuthScheme.BEARER);
//	}
//
//
//	public static HTTPAuthorization createBearer(String token)
//	{
//		return new HTTPAuthorization(HTTPAuthScheme.BEARER, token);
//	}

	
}
