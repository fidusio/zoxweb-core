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
package org.zoxweb.shared.security;

import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

/**
 * The request to authentication a user_id or account_id, the user_id can be a user, a MAC address, an IP Address etc
 * The following parameters must be set:
 * <ol>
 * <li> user_id: or account_id as email, MAC address or ip
 * <li> access code: can be the system password or user password
 * <li> the system id: where the authentication will take place 
 * </ol>
 * 
 * @author mnael
 *
 */
@SuppressWarnings("serial")
public class AuthenticationRequest
	extends AuthenticationBase
	
{
	public enum Params
		implements GetNVConfig
	{
		
		ACCESS_CODE(NVConfigManager.createNVConfig("access_code", "The Access Code", "AccessCode", false, true, false, String.class, FilterType.ENCRYPT)),
		//SYSTEM_ID(NVConfigManager.createNVConfig("system_id", "The Acces Code", "SystemID", false, true, false, String.class, null)),	
		;
		
		private final NVConfig cType;
		
		Params(NVConfig c)
		{
			cType = c;
		}
		
		public NVConfig getNVConfig() 
		{
			return cType;
		}
	}
	
	public static final NVConfigEntity NVC_AUTHENTICATION_REQUEST = new NVConfigEntityPortable("authentication_request",
																						   null, 
																						   "AuthenticationRequest", 
																						   true, 
																						   false, 
																						   false, 
																						   false, 
																						   AuthenticationRequest.class, 
																						   SharedUtil.extractNVConfigs(Params.values()), 
																						   null, 
																						   false, 
																						   AuthenticationBase.NVC_AUTHENTICATION_BASE);
	
	
	

	
	public AuthenticationRequest()
	{
		super(NVC_AUTHENTICATION_REQUEST);
	}

	public String getAccessCode()
	{
		return lookupValue(Params.ACCESS_CODE);
	}
	
	public void setAccessCode(String code)
	{
		setValue(Params.ACCESS_CODE, code);
	}

}