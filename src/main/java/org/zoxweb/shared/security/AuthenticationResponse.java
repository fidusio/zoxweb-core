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

import org.zoxweb.shared.util.GetNVConfig;
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
public class AuthenticationResponse
	extends AuthenticationBase
{
	
	
	
	public enum Params
		implements GetNVConfig
	{
		//SYSTEM_ID(NVConfigManager.createNVConfig("system_id", "The Acces Code", "SystemID", false, true, false, String.class, null)),
		SESSION_DURATION(NVConfigManager.createNVConfig("session_duration", "The session duration is millis", "SessionDuration", false, true, Long.class)),
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
	
	public static final NVConfigEntity NVC_AUTHENTICATION_REQUEST = new NVConfigEntityPortable("authentication_response",
																						   null, 
																						   "AuthenticationResponse", 
																						   true, 
																						   false, 
																						   false, 
																						   false, 
																						   AuthenticationResponse.class, 
																						   SharedUtil.extractNVConfigs(Params.values()), 
																						   null, 
																						   false, 
																						   AuthenticationBase.NVC_AUTHENTICATION_BASE
																						   );
	
	

	/**
	 * Default constructor
	 */
	public AuthenticationResponse()
	{
		super(NVC_AUTHENTICATION_REQUEST);
	}

	
	
	
	public AuthenticationResponse(String callerID, String systemID, long sessionDuration)
	{
		this();
		setCallerID(callerID);
		setSystemID(systemID);
		setSessionDuration(sessionDuration);
	}

	/**
	 * Get the session duration in milli second -1 for ever, 0 it the caller default session duration
	 * @return the session inactivity duration
	 */
	public long getSessionDuration()
	{
		// TODO Auto-generated method stub
		return lookupValue(Params.SESSION_DURATION);
	}

	/**
	 * 
	 * @param duration
	 */
	public void setSessionDuration(long duration)
	{
		setValue(Params.SESSION_DURATION, duration);
	}
	

}
