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


import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;


@SuppressWarnings("serial")
public class ShiroSubject
	extends ShiroDomain
{
	
//	private static final NVConfig NVC_AUTHENTICATION_REALM =  NVConfigManager.createNVConfig("authentication_realm", null,"AuthenticationRealm",true, false, String.class);
//	private static final NVConfig NVC_PASSWORD =  NVConfigManager.createNVConfig("password", null,"Password",true, false, String.class);
	
	public static final NVConfigEntity NVC_SHIRO_SUBJECT = new NVConfigEntityLocal("shiro_subject", "Shiro subject dao object" , "ShiroSubject", false, true, false, false, ShiroSubject.class, null, null, false, ShiroDomain.NVC_APP_ID_DAO);
	
	public ShiroSubject()
	{
		super(NVC_SHIRO_SUBJECT);
	}
	
	public ShiroSubject(String domainID, String appID, String username)
	{
		this();
		// MN do not change sequence
		setName(username);
		setDomainAppID(domainID, appID);
	}
	
//	public String getAuthenticationRealm()
//	{
//		return lookupValue(NVC_AUTHENTICATION_REALM);
//	}
//
//	public void setAuthenticationRealm(String authenticationRealm) 
//	{
//		setValue(NVC_AUTHENTICATION_REALM, authenticationRealm);
//	}

//	public String getPassword()
//	{
//		return lookupValue(NVC_PASSWORD);
//	}
//
//	public void setPassword(String password) 
//	{		
//		setValue(NVC_PASSWORD, password);
//	}
	
//	public boolean equals(Object o)
//	{
//		if ( this == o)
//		{
//			return true;
//		}
//			
//		if ( o != null && o instanceof ShiroSubjectDAO)
//		{
//			ShiroSubjectDAO to = (ShiroSubjectDAO) o;
//			if (getReferenceID() != null && to.getReferenceID() != null)
//				return getReferenceID().equals(to.getReferenceID());
//		}
//		return false;
//	}
	
	
	
}