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

import org.zoxweb.shared.data.SetNameDAO;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.SystemID;

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
public class AuthenticationBase
	extends SetNameDAO
	implements SystemID<String>
{
	
	public enum Params
		implements GetNVConfig
	{
		CALLER_ID(NVConfigManager.createNVConfig("caller_id", "The caller id", "CallerID", false, true, false, String.class, null)),
		SYSTEM_ID(NVConfigManager.createNVConfig("system_id", "The System Identifier", "SystemID", false, true, false, String.class, null)),
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
	
	public static final NVConfigEntity NVC_AUTHENTICATION_BASE = new NVConfigEntityPortable("authentication_base",
																						   null, 
																						   "AuthenticationBase", 
																						   true, 
																						   false, 
																						   false, 
																						   false, 
																						   AuthenticationBase.class, 
																						   SharedUtil.extractNVConfigs(Params.values()), 
																						   null, 
																						   false, 
																						   SetNameDAO.NVC_NAME_DAO);
	
	
	

	/**
	 */
	public AuthenticationBase()
	{
		super(NVC_AUTHENTICATION_BASE);
	}
	
	
	protected AuthenticationBase(NVConfigEntity nvce)
	{
		super(nvce);
	}




	/* (non-Javadoc)
	 * @see org.zoxweb.shared.util.SystemID#getSystemID()
	 */
	@Override
	public String getSystemID()
	{
		// TODO Auto-generated method stub
		return lookupValue(Params.SYSTEM_ID);
	}




	/* (non-Javadoc)
	 * @see org.zoxweb.shared.util.SystemID#setSystemID(java.lang.Object)
	 */
	@Override
	public void setSystemID(String systemID)
	{
		setValue(Params.SYSTEM_ID, systemID);
	}
	
	public String getCallerID()
	{
		return lookupValue(Params.CALLER_ID);
	}
	
	
	public void setCallerID(String callerID)
	{
		setValue(Params.CALLER_ID, callerID);
	}

}
