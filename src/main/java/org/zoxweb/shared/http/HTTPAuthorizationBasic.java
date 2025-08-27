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
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

/**
 * @author mnael
 *
 */
@SuppressWarnings("serial")
public class HTTPAuthorizationBasic
extends HTTPAuthorization
{

	public static final NVConfig NVC_USER = NVConfigManager.createNVConfig("user", null, "User", false, true, String.class);
	public static final NVConfig NVC_PASSWORD = NVConfigManager.createNVConfig("password", null, "Password", false, true, String.class);
	
	public static final NVConfigEntity NVC_HTTP_AUTHORIZATION_BASIC = new NVConfigEntityPortable("http_authorization_basic", null , null, true, false, false, false, HTTPAuthorizationBasic.class, SharedUtil.toNVConfigList(NVC_USER, NVC_PASSWORD), null, false, HTTPAuthorization.NVC_HTTP_AUTHORIZATION);
	

	public HTTPAuthorizationBasic()
	{
		super(NVC_HTTP_AUTHORIZATION_BASIC, HTTPAuthScheme.BASIC);
	}
	
	

	
	
	
	public HTTPAuthorizationBasic(String user, String password)
	{
		this();
		setUser(user);
		setPassword(password);
	}
	

	/**
	 * @return the user
	 */
	public String getUser()
	{
		return lookupValue(NVC_USER);
	}


	/**
	 * @param user the user to set
	 */
	public void setUser(String user)
	{
		setValue(NVC_USER, user);
	}


	/**
	 * @return the password
	 */
	public String getPassword()
	{
		return  lookupValue(NVC_PASSWORD);
	}


	/**
	 * @param password the password to set
	 */
	public void setPassword(String password)
	{
		setValue(NVC_PASSWORD, password);
	}
	
	
	
	public String toString()
	{
		return SharedUtil.toCanonicalID(' ', getAuthScheme(), getUser(), getPassword());
	}

	public GetNameValue<String> toHTTPHeader()
	{
		return getAuthScheme().toHTTPHeader(getUser(), getPassword());
	}
	
}
