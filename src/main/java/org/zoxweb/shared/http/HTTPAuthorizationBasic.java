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
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

/**
 * HTTP Basic Authentication credentials container as defined in RFC 7617.
 * Stores username and password for HTTP Basic authentication scheme.
 * <p>
 * The credentials are Base64-encoded when converted to an HTTP Authorization header
 * in the format: "Basic base64(username:password)".
 * </p>
 *
 * @author mnael
 * @see HTTPAuthorization
 * @see HTTPAuthScheme#BASIC
 */
@SuppressWarnings("serial")
public class HTTPAuthorizationBasic
extends HTTPAuthorization
{

	public static final NVConfig NVC_USER = NVConfigManager.createNVConfig("user", null, "User", false, true, String.class);
	public static final NVConfig NVC_PASSWORD = NVConfigManager.createNVConfig("password", null, "Password", false, true, String.class);
	
	public static final NVConfigEntity NVC_HTTP_AUTHORIZATION_BASIC = new NVConfigEntityPortable("http_authorization_basic", null , null, true, false, false, false, HTTPAuthorizationBasic.class, SharedUtil.toNVConfigList(NVC_USER, NVC_PASSWORD), null, false, HTTPAuthorization.NVC_HTTP_AUTHORIZATION);
	

	/**
	 * Default constructor creating an empty Basic authorization.
	 */
	public HTTPAuthorizationBasic()
	{
		super(NVC_HTTP_AUTHORIZATION_BASIC, HTTPAuthScheme.BASIC);
	}
	
	

	
	
	
	/**
	 * Constructs an HTTPAuthorizationBasic with the specified username and password.
	 *
	 * @param user     the username for authentication
	 * @param password the password for authentication
	 */
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
	
	
	
	/**
	 * Returns a string representation of this Basic authorization.
	 *
	 * @return string containing the scheme, username, and password
	 */
	public String toString()
	{
		return SharedUtil.toCanonicalID(' ', getAuthScheme(), getUser(), getPassword());
	}

	/**
	 * Converts this Basic authorization to an HTTP Authorization header.
	 * The result is in the format: "Basic base64(username:password)".
	 *
	 * @return the Authorization header as a name-value pair
	 */
	public GetNameValue<String> toHTTPHeader()
	{
		return getAuthScheme().toHTTPHeader(getUser(), getPassword());
	}
	
}
