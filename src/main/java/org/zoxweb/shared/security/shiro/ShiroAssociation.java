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
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;

import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public abstract class ShiroAssociation
    extends SetNameDAO
    implements ShiroBase
{

	private static final NVConfig NVC_ASSOCIATION_TYPE = NVConfigManager.createNVConfig("association_type", "The Association Type","AssociationType", true, false, ShiroAssociationType.class);
	private static final NVConfig NVC_ASSOCIATION_PARAM = NVConfigManager.createNVConfigEntity("association", "The Association","Association", true, false, ShiroDomain.class, ArrayType.NOT_ARRAY);
	private static final NVConfig NVC_ASSOCIATED_TO = NVConfigManager.createNVConfigEntity("associated_to", "The Associated to","AssociatedTo", true, false, ShiroDomain.class, ArrayType.NOT_ARRAY);
	
	
	private static final NVConfigEntity NVC_ASSOCIATION = new NVConfigEntityPortable("shiro_association_dao", null , "ShiroAssociationDAO", true, false, false, false, ShiroAssociation.class, SharedUtil.toNVConfigList(NVC_ASSOCIATION_TYPE, NVC_ASSOCIATION_PARAM, NVC_ASSOCIATED_TO), null, false, SetNameDAO.NVC_NAME_DAO);
	
	protected ShiroAssociation(ShiroAssociationType type, ShiroDomain associatedTo, ShiroDomain association)
	{
		super(NVC_ASSOCIATION);
		setAssociationType(type);
		setAssociatedTo(associatedTo);
		setAssociation(association);
	}

	public ShiroAssociationType getAssociationType()
    {
		return lookupValue(NVC_ASSOCIATION_TYPE);
	}

	public void setAssociationType(ShiroAssociationType associationType)
    {
		setValue(NVC_ASSOCIATION_TYPE, associationType);
		//this.associationType = associationType;
	}

	public ShiroDomain getAssociation()
    {
		return lookupValue(NVC_ASSOCIATION);
		//return association;
	}

	public void setAssociation(ShiroDomain association) {
		setValue( NVC_ASSOCIATION, association);
		//this.association = association;
	}

	public ShiroDomain getAssociatedTo()
    {
		return lookupValue(NVC_ASSOCIATED_TO);
		//return associatedTo;
	}

	public void setAssociatedTo(ShiroDomain associatedTo)
    {
		setValue( NVC_ASSOCIATED_TO, associatedTo);
		//this.associatedTo = associatedTo;
	}

    @Override
	public String toString()
	{
		return toCanonicalID();
	}
	
	@Override
	public String toCanonicalID()
    {
		return SharedUtil.toCanonicalID(CAN_ID_SEP, getAssociatedTo().getDomainID(), getAssociatedTo().getName(), getAssociation().getName());
	}
	
	
//	public void setReferenceID( String id)
//	{
//		
//	}
	
//	public String getReferenceID()
//	{
//		return null;
//	}
	
	
//	public void setDomainID( String id)
//	{
//		
//	}
//	
//	public String getDomainID()
//	{
//		return null;
//	}
	
//	public void setUserID( String id)
//	{
//		
//	}
//	
//	public String getUserID()
//	{
//		return null;
//	}
//	
//	public String getAccountID()
//	{
//		return null;
//	}
//	
//	public void setAccountID( String account)
//	{
//		
//	}

}
