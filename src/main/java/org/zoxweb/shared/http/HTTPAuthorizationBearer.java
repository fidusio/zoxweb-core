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

import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

/**
 * @author mnael
 *
 */
@SuppressWarnings("serial")
public class HTTPAuthorizationBearer
extends HTTPAuthorization
{
	
	
	public static final NVConfig NVC_TOKEN = NVConfigManager.createNVConfig("token", null,"Token", false, true, String.class);

	public static final NVConfigEntity NVC_HTTP_AUTHORIZATION_BEARER = new NVConfigEntityLocal("http_authorization_bearer", null , null, true, false, false, false, HTTPAuthorizationBearer.class, SharedUtil.toNVConfigList(NVC_TOKEN), null, false, HTTPAuthorizationBearer.NVC_HTTP_AUTHORIZATION);
	
	
	
	//private String token;
	public HTTPAuthorizationBearer()
	{
		super(NVC_HTTP_AUTHORIZATION_BEARER, HTTPAuthScheme.BEARER);
		setName(HTTPAuthScheme.BEARER.getName());
	}
	
	public HTTPAuthorizationBearer(String token)
	{
		this();
		setToken(token);
	}
	/**
	 * @return the token
	 */
	public String getToken() 
	{
		return lookupValue(NVC_TOKEN);
	}
	/**
	 * @param token the token to set
	 */
	public void setToken(String token) 
	{
		setValue(NVC_TOKEN, token);
	}


	
	public String toString()
	{
		return SharedUtil.toCanonicalID(' ', getAuthScheme(), getToken());
	}
	
	public GetNameValue<String> toHTTPHeader()
	{
//		if (getAuthSchemeOverride() != null)
//			return  getAuthScheme().toHTTPHeader(getAuthSchemeOverride(), getToken());

		return getAuthScheme().toHTTPHeader(getToken());
	}
	
}
