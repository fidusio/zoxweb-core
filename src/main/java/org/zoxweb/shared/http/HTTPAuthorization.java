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
public abstract class HTTPAuthorization
extends SetNameDAO
{

	public static final NVConfig NVC_AUTH_SCHEME = NVConfigManager.createNVConfig("authorization", null,"HTTPAuthScheme", false, true, HTTPAuthScheme.class);
	public static final NVConfig NVC_AUTH_SCHEME_OVERRIDE = NVConfigManager.createNVConfig("auth_scheme_override", null,"Token type override", false, true, String.class);
	public static final NVConfigEntity NVC_HTTP_AUTHORIZATION = new NVConfigEntityLocal(null, null , null, true, false, false, false, HTTPAuthorization.class, SharedUtil.toNVConfigList(NVC_AUTH_SCHEME, NVC_AUTH_SCHEME_OVERRIDE), null, false, SetNameDAO.NVC_NAME_DAO);
	
	protected HTTPAuthorization(NVConfigEntity nvce, HTTPAuthScheme type)
	{
		super(nvce);
		setValue(NVC_AUTH_SCHEME, type);
	}


	
	public HTTPAuthScheme getAuthScheme()
	{
		return lookupValue(NVC_AUTH_SCHEME);
	}
	public String getAuthSchemeOverride()
	{
		return lookupValue(NVC_AUTH_SCHEME_OVERRIDE);
	}

	public void setAuthSchemeOverride(String tokenType)
	{
		setValue(NVC_AUTH_SCHEME_OVERRIDE, tokenType);
	}
	
	abstract public GetNameValue<String> toHTTPHeader();
	
	
}
