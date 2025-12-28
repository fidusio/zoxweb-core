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

import org.zoxweb.shared.data.TimeStampDAO;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.Const.Status;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.util.Date;

/**
 *
 */
@SuppressWarnings("serial")
public class ShiroAssociationRule
	extends	TimeStampDAO 
{
	
	public enum Param
		implements GetNVConfig
	{

		ASSOCIATE(NVConfigManager.createNVConfig("associate", "The object to associate", "Associate", true, false, false, true, true, String.class, null)),
		REFERENCE_TYPE(NVConfigManager.createNVConfig("reference_type", "The type of the object associate", "ReferenceType", false, false, false, true, false, String.class, null)),
		ASSOCIATION_CRUD(NVConfigManager.createNVConfig("crud", "crud", "CRUD", true, true, CRUD.class)),
		ASSOCIATED_TO(NVConfigManager.createNVConfig("associated_to", "The object associated with", "AssociatedTo", true, false, false, true, false, String.class, null)),
		ASSOCIATION_STATUS(NVConfigManager.createNVConfig("association_status", "Association status", "AssociationStatus", true, true, Status.class)),
		ASSOCIATION_TYPE(NVConfigManager.createNVConfig("association_type", "Association type", "AssociationType", true, true, ShiroAssociationType.class)),
		EXPIRATION(NVConfigManager.createNVConfig("expiration", "Expiration date if set", "Expiration", true, true, Date.class)),
		PATTERN(NVConfigManager.createNVConfig("pattern", "Shiro compatible pattern", "Pattern", true, true, String.class)),
		ASSOCIATION(NVConfigManager.createNVConfigEntity("association", "The shiro association permission or role", "Association", false, false,  ShiroDomain.class, ArrayType.NOT_ARRAY)),
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
	
	/**
	 * This NVConfigEntity type constant is set to an instantiation of a NVConfigEntityLocal object based on UserIDDAO.
	 */
	public static final NVConfigEntity NVC_SHIRO_ASSOCIATION_RULE = new NVConfigEntityPortable("shiro_association_rule", null , "ShiroAssociationRule", true, false, false, false, ShiroAssociationRule.class, SharedUtil.extractNVConfigs(Param.values()), null, false, TimeStampDAO.NVC_TIME_STAMP_DAO);
	
	
	private String dynamicPattern;
	
	public ShiroAssociationRule()
	{
		super(NVC_SHIRO_ASSOCIATION_RULE);
		setExpiration(null);
	}
	
	public ShiroAssociationRule(String name, NVEntity associate, NVEntity associatedTo, Status status, ShiroAssociationType at, Date expiration, CRUD crud)
	{
		this(name, associate != null ? associate.getReferenceID() : null, associate != null ? associate.getClass().getName() : null, associatedTo != null ? associatedTo.getReferenceID() : null, status, at, expiration, crud);
	}
	
	
	public ShiroAssociationRule(String name, String associate, String associateRefType, String associatedTo, Status status, ShiroAssociationType at, Date expiration, CRUD crud)
	{
		this();
		SUS.checkIfNulls("Null parameters", name, associate, associatedTo, at, crud);
		setName(name);
		setAssociate(associate);
		setAssociatedTo(associatedTo);
		setAssociationType(at);
		setExpiration(expiration);
		setReferenceType(associateRefType);
		setCRUD(crud);
		if (status == null)
		{
			status = Status.ACTIVE;
		}
		setAssociationStatus(status);
	}
	
	
	public ShiroAssociationRule(String name, ShiroDomain associate, ShiroAssociationType at, NVEntity associateTo)
	{
		this();
		SUS.checkIfNulls("Null parameters", name, associate, at, associateTo);
		if(associate == associateTo || associate.equals(associateTo))
		{
			throw new IllegalArgumentException("Invalid association rule");
		}
		setName(name);
		setAssociatedTo(associateTo.getReferenceID());
		setAssociationType(at);
		setExpiration(null);
		setAssociationStatus(Status.ACTIVE);
		setAssociation(associate);
	}
	
	
	public ShiroAssociationType getAssociationType()
	{
		return lookupValue(Param.ASSOCIATION_TYPE);
	}

    @Override
	public void setName(String name)
	{
		if (name != null)
		{
			name = name.toLowerCase();
		}
		
		super.setName(name);
	}
	
	
	public void setAssociationType(ShiroAssociationType assType)
	{
		setValue(Param.ASSOCIATION_TYPE, assType);
	}
	
	
	public String getAssociate()
	{
		return lookupValue(Param.ASSOCIATE);
	}
	
	public void setAssociate(String associate)
	{
		setValue(Param.ASSOCIATE, associate);
	}
	
	
	public String getReferenceType()
	{
		return lookupValue(Param.REFERENCE_TYPE);
	}
	
	public void setReferenceType(String associateRefType)
	{
		setValue(Param.REFERENCE_TYPE, associateRefType);
	}
	
	
	public String getAssociatedTo()
	{
		return lookupValue(Param.ASSOCIATED_TO);
	}
	
	public void setAssociatedTo(String associatedTo)
	{
		setValue(Param.ASSOCIATED_TO, associatedTo);
	}
	
	
	public String getPattern()
	{
		String ret = lookupValue(Param.PATTERN);

		if (ret == null)
		{
			return getDynamicPattern();
		}
		
		return ret;
	}
	
	public void setPattern(String pattern)
	{
		setValue(Param.PATTERN, pattern);
	}

	public CRUD getCRUD()
	{
		return lookupValue(Param.ASSOCIATION_CRUD);
	}
	
	public void setCRUD(CRUD crud)
	{
		setValue(Param.ASSOCIATION_CRUD, crud);
	}
	
	
	public Date getExpiration()
	{
		Long val = lookupValue(Param.EXPIRATION);

		if (val == null || val == -1)
		{
			return null;
		}
		return new Date(val);
	}
	
	
	public void setExpiration(Date date)
	{
		if (date == null)
		{
			setValue(Param.EXPIRATION, (long)-1);
		}
		else
		{
			setValue(Param.EXPIRATION, date.getTime());
		}
		
	}
	
	public Status getAssociationStatus()
	{
		return lookupValue(Param.ASSOCIATION_STATUS);
	}
	
	public void setAssociationStatus(Status status)
	{
		setValue(Param.ASSOCIATION_STATUS, status);
	}

	private synchronized String getDynamicPattern()
	{
		if (dynamicPattern == null)
		{
			switch(getAssociationType())
			{
			case PERMISSION_TO_ROLE:
				break;
			case PERMISSION_TO_SUBJECT:
				dynamicPattern = SharedUtil.toCanonicalID(':', getName(), getCRUD(), getAssociate()).toLowerCase();
			case ROLEGROUP_TO_SUBJECT:
				break;
			case ROLE_TO_ROLEGROUP:
				break;
			case ROLE_TO_SUBJECT:
				break;
			case PERMISSION_TO_RESOURCE:
				break;
			case ROLE_TO_RESOURCE:
				break;
			
			}
		}
		
		return dynamicPattern;
	}
	
	public <V extends ShiroDomain> V getAssociation()
	{
		return lookupValue(Param.ASSOCIATION);
	}
	
	public <V extends ShiroDomain> void setAssociation(V association)
	{
		setValue(Param.ASSOCIATION, association);
		setAssociate(association.getReferenceID());
	}
}
