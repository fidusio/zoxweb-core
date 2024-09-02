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
	//public static final NVConfig NVC_ACCOUNT_ID = NVConfigManager.createNVConfig(MetaToken.ACCOUNT_ID.getName(), "The account id","AccountID", true, false, false, true, true, String.class, null);
	public static final NVConfig NVC_SUBJECT_GUID = NVConfigManager.createNVConfig(MetaToken.SUBJECT_GUID.getName(), "The user id or Subject GUID","SubjectGUID", true, false, false, true, false, String.class, null);
	public static final NVConfig NVC_GUID = NVConfigManager.createNVConfig(MetaToken.GUID.getName(), "The global id of the Object","GlobalUID", true, false, true, true, false, String.class, null);
	public static final NVConfigEntity NVC_REFERENCE_ID_DAO = new NVConfigEntityLocal("reference_id_dao", null , "ReferenceIDDAO", true, false, false, false, ReferenceIDDAO.class, SharedUtil.toNVConfigList(NVC_REFERENCE_ID, NVC_SUBJECT_GUID, NVC_GUID), null, false, null);

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
		return lookupValue(NVC_REFERENCE_ID);
	}

	/**
	 * Sets the reference ID.
	 * @param referenceID
	 */
	@Override
	public void setReferenceID(String referenceID)
	{
		setValue(NVC_REFERENCE_ID, referenceID);
	}
	
	/**
	 * Returns the account ID.
	 * @return account id
	 */
	//@Column(name = "account_id")
//	@Override
//	public String getAccountID()
//	{
//		return lookupValue(NVC_ACCOUNT_ID);
//	}
//
//	/**
//	 * Sets the account ID.
//	 * @param accountID
//	 */
//	@Override
//	public void setAccountID(String accountID)
//	{
//		setValue(NVC_ACCOUNT_ID, accountID);
//	}
	
	/**
	 * Returns the user ID.
	 * @return user id
	 */
	//@Column(name = "user_id")
//	@Override
//	public String getUserID()
//	{
//		return getSubjectGUID();
//	}
//
//	/**
//	 * Sets the user ID.
//	 * @param userID
//	 */
//	@Override
//	public void setUserID(String userID)
//	{
//		setSubjectGUID(userID);
//	}

	/**
	 * Returns the global ID.
	 * @return global id
	 */
	//@Column(name = "global_id")
	@Override
	public String getGUID() {
		return lookupValue(NVC_GUID);
	}

	/**
	 * Sets the global ID.
	 * @param globalID global uuid
	 */
	@Override
	public void setGUID(String globalID)
	{
		setValue(NVC_GUID, globalID);
	}

	/**
	 * Returns the global ID.
	 * @return global id
	 */
	//@Column(name = "global_id")
	@Override
	public String getSubjectGUID() {
		return lookupValue(NVC_SUBJECT_GUID);
	}

	/**
	 * Sets the global ID.
	 * @param subjectGUID global uuid
	 */
	@Override
	public void setSubjectGUID(String subjectGUID)
	{
		setValue(NVC_SUBJECT_GUID, subjectGUID);
	}

}