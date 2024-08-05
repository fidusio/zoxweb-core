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

import org.zoxweb.shared.data.SetNameDAO;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;

import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class ShiroLoginToken
	extends SetNameDAO
{

	private static final NVConfig NVC_DOMAIN_ID = NVConfigManager.createNVConfig("domain_id", "The domain id","DomainID", true, true, String.class);
	private static final NVConfig NVC_APPLICATION_ID = NVConfigManager.createNVConfig("application_id", "The application id trying to login to","ApplicationID", false, true, String.class);
	private static final NVConfig NVC_REALM = NVConfigManager.createNVConfig("realm", "The realm","realm", false, true, String.class);
	private static final NVConfig NVC_USER_ID = NVConfigManager.createNVConfig("user_id", "The user","UserID", true, true, String.class);
	private static final NVConfig NVC_PASSWORD = NVConfigManager.createNVConfig("password", "The password","Password", true, true, String.class);

	private static final NVConfigEntity NVC_LOGIN_TOKEN = new NVConfigEntityLocal("shiro_login_token", "The login token", "ShiroLoginToken", true, false, false, false, ShiroLoginToken.class, SharedUtil.toNVConfigList( NVC_DOMAIN_ID, NVC_APPLICATION_ID, NVC_REALM, NVC_USER_ID, NVC_PASSWORD), null, false, SetNameDAO.NVC_NAME_DAO);

	public ShiroLoginToken()
	{
		super(NVC_LOGIN_TOKEN);
	}

	public ShiroLoginToken(String domainID, String applicationID, String realm, String userID, String password)
	{
		this();
		setUserID( userID);
		
		setPassword( password);
		setDomainID( domainID);
		setRealm( realm);
	}
	
	@Override
	public String getUserID()
	{
		return lookupValue( NVC_USER_ID);
	}

    @Override
	public void setUserID( String user)
	{
		setValue( NVC_USER_ID, user);
	}
	
	public String getApplicationID()
	{
		return lookupValue( NVC_APPLICATION_ID);
	}
	
	public void setApplicationID( String appID)
	{
		setValue( NVC_APPLICATION_ID, appID);
	}
	
	public String getPassword()
	{
		return lookupValue( NVC_PASSWORD);
	}
	
	public void setPassword( String password)
	{
		setValue( NVC_PASSWORD, password);
	}
	
	
	public String getDomainID()
	{
		return lookupValue( NVC_DOMAIN_ID);
	}
	
	public void setDomainID( String domain)
	{
		setValue( NVC_DOMAIN_ID, domain);
	}

	public String getRealm()
	{
		return lookupValue( NVC_REALM);
	}
	
	public void setRealm( String realm)
	{
		setValue( NVC_REALM, realm);
	}


	
}
