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

import org.zoxweb.shared.data.DataConst.DataParam;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.SetCanonicalID;
import org.zoxweb.shared.util.SharedUtil;

/**
 * 
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class CanonicalIDDAO 
	extends TimeStampDAO
	implements SetCanonicalID
{
	
	public static final NVConfigEntity NVC_CANONICAL_ID_DAO = new NVConfigEntityPortable
																	(
																		"canonical_id_dao", 
																		null, 
																		"CanonicalIDDAO", 
																		true, 
																		false, 
																		false, 
																		false, 
																		CanonicalIDDAO.class, 
																		SharedUtil.toNVConfigList(DataParam.CANONICAL_ID.getNVConfig()),
																		null, 
																		false, 
																		TimeStampDAO.NVC_TIME_STAMP_DAO
																	);
	
	
	public CanonicalIDDAO()
	{
		super(NVC_CANONICAL_ID_DAO);
	}
	
	protected CanonicalIDDAO(NVConfigEntity nvce)
	{
		super(nvce);
	}
	
	@Override
	public String toCanonicalID()
	{
		return getCanonicalID();
	}
	
	/**
	 * Returns canonical ID.
	 */
	@Override
	public String getCanonicalID() 
	{
		return lookupValue(DataParam.CANONICAL_ID);
	}
	
	/**
	 * Sets canonical ID.
	 * @param id
	 */
	@Override
	public void setCanonicalID(String id)
	{
		setValue(DataParam.CANONICAL_ID, id);		
	}
	
}