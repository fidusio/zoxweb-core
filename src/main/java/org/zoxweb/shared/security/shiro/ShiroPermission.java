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

import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class ShiroPermission
	extends ShiroDomain
{
	
	//public static final NVConfig NVC_EMBED_APP_ID =  NVConfigManager.createNVConfig("embed_app_id", null,"EmbedAppID",true, false, Boolean.class);
	public static final NVConfig NVC_PATTERN =  NVConfigManager.createNVConfig("pattern", null,"Pattern",true, false, String.class);
	
	public static final NVConfigEntity  NVC_SHIRO_PERMISSION = new NVConfigEntityPortable("shiro_permission", "Shiro permission dao object" ,
			"ShiroPermission", false, true, false, false, ShiroPermission.class, SharedUtil.toNVConfigList(NVC_PATTERN),
			null, false, ShiroDomain.NVC_SHIRO_DOMAIN);
	
	public ShiroPermission()
	{
		super(NVC_SHIRO_PERMISSION);
	}
	
	public ShiroPermission(String domainID, String appID, String name, String description, String pattern)
	{
		this();
		// MN do not change sequence
		setName(name);
		setDescription(description);
		setDomainAppID(domainID, appID);
		setPermissionPattern(pattern);
	}
	
//	public boolean equals(Object o)
//	{
//		if (this == o)
//		{
//			return true;
//		}
//			
//		if (o != null && o instanceof ShiroPermissionDAO)
//		{
//			ShiroPermissionDAO to = (ShiroPermissionDAO) o;
//			
//			if (SharedUtil.referenceIDToLong(this) == 0 && SharedUtil.referenceIDToLong(to) == 0)
//			{
//				return toCanonicalID().equals(to);
//			}
//			if (getReferenceID() == to.getReferenceID())
//			{
//				return true;
//			}
//		}
//		
//		return false;
//	}
	
	public String getPermissionPattern() 
	{
		return lookupValue(NVC_PATTERN);	
	}
	
	public synchronized void setPermissionPattern(String pattern)
	{
		setValue(NVC_PATTERN, SharedStringUtil.trimOrEmpty(SharedStringUtil.toLowerCase(pattern)));
	}
	

	
}