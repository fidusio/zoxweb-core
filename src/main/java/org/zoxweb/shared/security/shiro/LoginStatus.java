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
package org.zoxweb.shared.security.shiro;

import org.zoxweb.shared.data.DomainInfoDAO;
import org.zoxweb.shared.util.*;

@SuppressWarnings("serial")
public class LoginStatus
	extends DomainInfoDAO
{

	public enum Param
		implements GetNVConfig
	{
		
		//NVC_DOMAIN_ID(NVConfigManager.createNVConfig("domain_id", "The domain id","DomainID", true, false, String.class)),
		NVC_APPLICATION_ID(NVConfigManager.createNVConfig("application_id", "The application id trying to login to","ApplicationID", false, true, String.class)),
		NVC_REALM(NVConfigManager.createNVConfig("realm", "The realm","realm", false, false, String.class)),
		NVC_PRINCIPAL(NVConfigManager.createNVConfig("principal", "The user","Principal", true, false, String.class)),
		NVC_LOGIN_STATUS(NVConfigManager.createNVConfig("login_status", "The login status", "LoginStatus", true, false, boolean.class)),
		NVC_SESSION_TIMEOUT_MILLIS(NVConfigManager.createNVConfig("session_timeout_millis", "The session timeout in millis", "SessionTimeoutMillis", true, false, long.class)),
//		NVC_ROLE_LIST(NVConfigManager.createNVConfig("roles", "The user roles","UserRoles", true, false, String[].class)),
//		NVC_PERMISSION_LIST(NVConfigManager.createNVConfig("permissions", "The user permissions","UserPermissions", true, false, String[].class)),
//		NVC_EXTRA_INFO(NVConfigManager.createNVConfig("extra_info", "login extra information","ExtraInfo", true, false, String[].class)),
		
		;

		private final NVConfig nvc;

        Param(NVConfig nvc)
		{
			this.nvc = nvc;
		}
		
		public NVConfig getNVConfig() 
		{
			return nvc;
		}
	}
	
	
	
	
	
	
	private static final NVConfigEntity NVC_LOGIN_STATUS = new NVConfigEntityLocal("login_status", "The login status", "LoginStatus", true, false, false, false, LoginStatus.class, SharedUtil.extractNVConfigs(Param.values()), null, false, DomainInfoDAO.NVC_DOMAIN_INFO_DAO);
	
	
	
	
	
	public LoginStatus()
	{
		super(NVC_LOGIN_STATUS);
	}
	
	public LoginStatus(final String domainID, final String appID, final String userID, final boolean loggedIn, long sessionTimeoutMillis)
	{
		this();
		setDomainID(domainID);
		setApplicationID( appID);
		setUserID(userID);
		setLoggedIn(loggedIn);
		setSessionTimeoutInMillis(sessionTimeoutMillis);
	}

	
	public String getPrincipal()
	{
		return lookupValue(Param.NVC_PRINCIPAL);
	}
	
	public void setPrincipal(String principal)
	{
		setValue(Param.NVC_PRINCIPAL, principal);
	}
	

	public String getRealm()
	{
		return lookupValue(Param.NVC_REALM);
	}
	
	public void setRealm(String realm)
	{
		setValue(Param.NVC_REALM, realm);
	}
	
	
	public long getSessionTimeoutInMillis()
	{
		return lookupValue(Param.NVC_SESSION_TIMEOUT_MILLIS);
	}
	
	public void setSessionTimeoutInMillis(long timeout)
	{
		setValue(Param.NVC_SESSION_TIMEOUT_MILLIS, timeout);
	}
	
	
	public boolean isLoggedIn()
	{
		return lookupValue(Param.NVC_LOGIN_STATUS);
	}
	
	public void setLoggedIn(boolean status)
	{
		setValue(Param.NVC_LOGIN_STATUS, status);
	}
	
	
//	public List<NVPair> getUserRoles()
//	{
//		return lookupValue(Param.NVC_ROLE_LIST);
//	}
//
//	public void setUserRoles( List<NVPair> roles)
//	{
//		setValue(Param.NVC_ROLE_LIST, roles);
//	}
//
//
//	public List<NVPair> getUserPermissions()
//	{
//		return lookupValue(Param.NVC_PERMISSION_LIST);
//	}
//
//	public void setUserPermissions(List<NVPair> permissions)
//	{
//		setValue(Param.NVC_PERMISSION_LIST, permissions);
//	}
//
//
//	public List<NVPair> getExtaInformations()
//	{
//		return lookupValue(Param.NVC_EXTRA_INFO);
//	}
//
//	public void setExtraInformations(List<NVPair> extraInfo)
//	{
//		setValue(Param.NVC_EXTRA_INFO, extraInfo);
//	}
	

	public String getApplicationID()
	{
		return lookupValue(Param.NVC_APPLICATION_ID);
	}
	
	public void setApplicationID(String appID)
	{
		setValue(Param.NVC_APPLICATION_ID, appID);
	}

}
