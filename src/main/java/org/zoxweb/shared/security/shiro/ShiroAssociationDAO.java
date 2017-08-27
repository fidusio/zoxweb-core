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
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;

import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public abstract class ShiroAssociationDAO
    extends SetNameDAO
    implements ShiroDAO
{

	private static final NVConfig NVC_ASSOCIATON_TYPE = NVConfigManager.createNVConfig("association_type", "The Association Type","AssociationType", true, false, ShiroAssociationType.class);
	private static final NVConfig NVC_ASSOCIATION = NVConfigManager.createNVConfigEntity("association", "The Association","Association", true, false, ShiroDomainDAO.class, ArrayType.NOT_ARRAY);
	private static final NVConfig NVC_ASSOCIATED_TO = NVConfigManager.createNVConfigEntity("associated_to", "The Associated to","AssociatedTo", true, false, ShiroDomainDAO.class, ArrayType.NOT_ARRAY);
	
	
	private static final NVConfigEntity NVC_ASSOCIATON_DAO = new NVConfigEntityLocal("shiro_association_dao", null , "ShiroAssociationDAO", true, false, false, false, ShiroAssociationDAO.class, SharedUtil.toNVConfigList(NVC_ASSOCIATON_TYPE, NVC_ASSOCIATION, NVC_ASSOCIATED_TO), null, false, SetNameDAO.NVC_NAME_DAO);
	
	protected ShiroAssociationDAO(ShiroAssociationType type,  ShiroDomainDAO associatedTo, ShiroDomainDAO association)
	{
		super(NVC_ASSOCIATON_DAO);
		setAssociationType(type);
		setAssociatedTo(associatedTo);
		setAssociation(association);
	}

	public ShiroAssociationType getAssociationType()
    {
		return lookupValue(NVC_ASSOCIATON_TYPE);
	}

	public void setAssociationType(ShiroAssociationType associationType)
    {
		setValue(NVC_ASSOCIATON_TYPE, associationType);
		//this.associationType = associationType;
	}

	public ShiroDomainDAO getAssociation()
    {
		return lookupValue(NVC_ASSOCIATION);
		//return association;
	}

	public void setAssociation(ShiroDomainDAO association) {
		setValue( NVC_ASSOCIATION, association);
		//this.association = association;
	}

	public ShiroDomainDAO getAssociatedTo()
    {
		return lookupValue(NVC_ASSOCIATED_TO);
		//return associatedTo;
	}

	public void setAssociatedTo(ShiroDomainDAO associatedTo)
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
