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

import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.util.List;

@SuppressWarnings("serial")
public class ShiroRoleGroup
	extends ShiroDomain
{
	
	public enum Param
		implements GetNVConfig
	{
		
		ROLES(NVConfigManager.createNVConfigEntity("roles", "The roles associated with the role group.", "Roles", false, true, ShiroRole.class, ArrayType.REFERENCE_ID_MAP)),
		
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
	
	public static final NVConfigEntity NVC_SHIRO_ROLE_GROUP = new NVConfigEntityLocal("shiro_role_group", "Shiro rolegroup dao object" ,
			"ShiroRoleGroup", false, true, false, false, ShiroRoleGroup.class, SharedUtil.extractNVConfigs(Param.values()),
			null, false, ShiroDomain.NVC_APP_ID_DAO);
	
	public ShiroRoleGroup()
	{
		super(NVC_SHIRO_ROLE_GROUP);
	}
	
	public ShiroRoleGroup(String domainID, String appID, String name, String description)
	{
		this();
		// MN do not change sequence
		setName(name);
		setDescription(description);
		setDomainAppID(domainID, appID);
	}
	
	
	
	
//	public boolean equals(Object o)
//	{
//		if (this == o)
//		{
//			return true;
//		}
//			
//		if (o != null && o instanceof ShiroRoleGroupDAO)
//		{
//			ShiroRoleGroupDAO to = (ShiroRoleGroupDAO) o;
//			if (SharedUtil.referenceIDToLong(this) == 0 && SharedUtil.referenceIDToLong(to) == 0)
//			{
//				return toCanonicalID().equals(to);
//			}
//			if ( getReferenceID() == to.getReferenceID())
//			{
//				return true;
//			}
//			
//		}
//		
//		return false;
//	}
	
	@SuppressWarnings("unchecked")
	public ArrayValues<NVEntity> getRoles()
	{
		return (ArrayValues<NVEntity>) lookup(Param.ROLES);
	}
	
	public void setRoles(ArrayValues<NVEntity> values)
	{
		getRoles().add(values.values(), true);
	}
	
	public void setRoles(List<NVEntity> values)
	{
		getRoles().add(values.toArray(new NVEntity[0]), true);
	}
	
}