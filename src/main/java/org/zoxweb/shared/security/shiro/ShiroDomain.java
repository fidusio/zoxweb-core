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
package org.zoxweb.shared.security.shiro;

import org.zoxweb.shared.data.AppIDDAO;
import org.zoxweb.shared.data.DataConst.DataParam;
import org.zoxweb.shared.util.*;


@SuppressWarnings("serial")
public abstract class ShiroDomain
	extends AppIDDAO
	implements ShiroBase
{
	
	//private static final NVConfig NVC_DOMAIN_ID =  NVConfigManager.createNVConfig("domain_id", null,"DomainID",true, false, String.class);
	protected static final NVConfig DOMAIN_APP_ID_LOCAL = NVConfigManager.createNVConfig("domain_app_id", "Domain APP ID", "DomainAppID", true, false, false,String.class, null);
	
	protected static final NVConfigEntity NVC_SHIRO_DOMAIN = new NVConfigEntityPortable("shiro_domain", null , "ShiroDomain", false, true, false, false, ShiroDomain.class, SharedUtil.toNVConfigList(DOMAIN_APP_ID_LOCAL), null, false, AppIDDAO.NVC_APP_ID_DAO);
	//,SharedUtil.extractNVConfigs( new Params[]{Params.REFERENCE_ID, Params.NAME, Params.LENGTH}));
	
	
	protected ShiroDomain(NVConfigEntity nvce)
	{
		super(nvce);
	}
	
	
	public void setName(String name)
	{
		setValue(DataParam.NAME, SUS.trimOrEmpty(SharedStringUtil.toLowerCase(name)));
	}

	@Override
	public boolean equals(Object o)
	{
		if ( this == o)
		{
			return true;
		}
			
		if ( o != null && o instanceof ShiroDomain)
		{
			ShiroDomain to = (ShiroDomain) o;
			
			if (getGUID() != null && to.getGUID() != null)
			{
				return getGUID().equals(to.getGUID());
			}
			
			if (getDomainID() != null && to.getDomainID() != null)
			{
				return getDomainID().equals(to.getDomainID());
			}
		}
		return false;
	}	
	
	public String toCanonicalID()
	{
		return SharedUtil.toCanonicalID(ShiroBase.CAN_ID_SEP, getDomainID(), getAppID(), getName());
	}
	
	
}