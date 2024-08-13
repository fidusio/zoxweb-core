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
package org.zoxweb.shared.data;


import org.zoxweb.shared.util.MetaToken;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.SharedUtil;

/**
 *
 *
 */
//@MappedSuperclass
@SuppressWarnings("serial")
public abstract class ReferenceIDDAO 
	extends NVEntity 
{

	public static final NVConfig NVC_REFERENCE_ID = NVConfigManager.createNVConfig(MetaToken.REFERENCE_ID.getName(), "The reference id of the Object","ReferenceID", true, false, true, true, true, String.class, null);
	public static final NVConfig NVC_ACCOUNT_ID = NVConfigManager.createNVConfig(MetaToken.ACCOUNT_ID.getName(), "The account id","AccountID", true, false, false, true, true, String.class, null);
	public static final NVConfig NVC_USER_ID = NVConfigManager.createNVConfig(MetaToken.USER_ID.getName(), "The user id","UserID", true, false, false, true, true, String.class, null);
	public static final NVConfig NVC_GLOBAL_ID = NVConfigManager.createNVConfig(MetaToken.GLOBAL_ID.getName(), "The global id of the Object","GlobalID", true, false, true, true, false, String.class, null);
	public static final NVConfigEntity NVC_REFERENCE_ID_DAO = new NVConfigEntityLocal("reference_id_dao", null , "ReferenceIDDAO", true, false, false, false, ReferenceIDDAO.class, SharedUtil.toNVConfigList(NVC_REFERENCE_ID, NVC_ACCOUNT_ID, NVC_USER_ID, NVC_GLOBAL_ID), null, false, null);

	protected ReferenceIDDAO(NVConfigEntity nvce)
	{
		super(nvce);
	}
	
	/**
	 * Returns the reference ID.
	 * @return reference id
	 */
	//@Id
	//@Column(name = "reference_id")
	@Override
	public String getReferenceID()
    {
		if(GLOBAL_ID_AS_REF_ID)
			return getGUID();

		return lookupValue(NVC_REFERENCE_ID);
	}

	/**
	 * Sets the reference ID.
	 * @param referenceID
	 */
	@Override
	public void setReferenceID(String referenceID)
	{
		if(GLOBAL_ID_AS_REF_ID)
			setGUID(referenceID);
		else
			setValue(NVC_REFERENCE_ID, referenceID);
	}
	
	/**
	 * Returns the account ID.
	 * @return account id
	 */
	//@Column(name = "account_id")
	@Override
	public String getAccountID()
	{
		return lookupValue(NVC_ACCOUNT_ID);
	}

	/**
	 * Sets the account ID.
	 * @param accountID
	 */
	@Override
	public void setAccountID(String accountID)
	{
		setValue(NVC_ACCOUNT_ID, accountID);
	}
	
	/**
	 * Returns the user ID.
	 * @return user id
	 */
	//@Column(name = "user_id")
	@Override
	public String getUserID()
	{
		return lookupValue(NVC_USER_ID);
	}

	/**
	 * Sets the user ID.
	 * @param userID
	 */
	@Override
	public void setUserID(String userID)
	{
		setValue(NVC_USER_ID, userID);
	}

	/**
	 * Returns the global ID.
	 * @return global id
	 */
	//@Column(name = "global_id")
	@Override
	public String getGUID() {
		return lookupValue(NVC_GLOBAL_ID);
	}

	/**
	 * Sets the global ID.
	 * @param globalID global uuid
	 */
	@Override
	public void setGUID(String globalID)
	{
		setValue(NVC_GLOBAL_ID, globalID);
	}

}