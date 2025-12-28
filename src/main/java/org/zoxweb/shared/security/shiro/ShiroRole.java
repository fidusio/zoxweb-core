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

import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.util.List;

@SuppressWarnings("serial")
public class ShiroRole
	extends ShiroDomain
{
	
	public enum Param
		implements GetNVConfig
	{
		
		PERMISSIONS(NVConfigManager.createNVConfigEntity("permissions", "The permissions associated with the role.", "Permissions", false, true, ShiroPermission.class, ArrayType.GET_NAME_MAP)),
	
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
	
	public static final NVConfigEntity NVC_SHIRO_ROLE = new NVConfigEntityPortable("shiro_role", "Shiro role dao object" , "ShiroRole",
			false, true, false, false, ShiroRole.class, SharedUtil.extractNVConfigs(Param.values()), null, false, ShiroDomain.NVC_SHIRO_DOMAIN);
	
	public ShiroRole()
	{
		super(NVC_SHIRO_ROLE);
	}
	
	
	public ShiroRole(String domainID, String appID, String roleName)
	{
		this(domainID, appID, roleName, null);
	}
	
	public ShiroRole(String domainID, String appID, String roleName, String description)
	{
		this();
		// MN do not change sequence
		setName(roleName);
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
//		if (o != null && o instanceof ShiroRoleDAO)
//		{
//			ShiroRoleDAO to = (ShiroRoleDAO) o;
////			if (SharedUtil.referenceIDToLong(this) == 0 && SharedUtil.referenceIDToLong(to) == 0)
////			{
////				return toCanonicalID().equals(to);
////			}
////			if (getReferenceID() == to.getReferenceID())
////			{
////				return true;
////			}
//			
//			if (to.getReferenceID() != null && getReferenceID() != null)
//			{
//				return getReferenceID().equals(to.getReferenceID());
//			}
//			
//		}
//		
//		return false;
//	}
	
	
	
	@SuppressWarnings("unchecked")
	public ArrayValues<NVEntity> getPermissions()
	{
		return (ArrayValues<NVEntity>) lookup(Param.PERMISSIONS);
	}
	
	
	public synchronized void addPermissions(ShiroPermission...permissions)
	{
		for (ShiroPermission p : permissions)
		{
			getPermissions().add(p);
		}
	}
	public synchronized void setPermissions(ArrayValues<NVEntity> values)
	{
		getPermissions().add(values.values(), true);
	}
	
	public synchronized void setPermissions(List<NVEntity> values)
	{
		getPermissions().add(values.toArray(new NVEntity[0]), true);
	}


	
}