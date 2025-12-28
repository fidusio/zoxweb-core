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
package org.zoxweb.shared.data;

import java.util.Date;
import java.util.List;

import org.zoxweb.shared.util.CRUD;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

/**
 *
 */
@SuppressWarnings("serial")
public class NVEntityAccessInfo
	extends TimeStampDAO 
{

	public enum Param
		implements GetNVConfig
	{
		ASSOCIATED_NVE(NVConfigManager.createNVConfig("associated_nve", "Referenced ID of associated NVEntity.", "AssociatedNVEntity", true, false, false, false, true, String.class, null)),
		CRUDS(NVConfigManager.createNVConfig("cruds", "CRUDs (permissions) supported", "CRUDs", true, true, CRUD[].class)),
		EXPIRATION_DATE(NVConfigManager.createNVConfig("expiration_date", "Expiration date.", "Expiration Date", false, false, Date.class)),
		
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
	
	public static final NVConfigEntity NVC_NVENTITY_ACCESS_INFO = new NVConfigEntityPortable(
            "nventity_access_info",
            null,
            "NVEntityAccessInfo",
            true,
            false,
            false,
            false,
            NVEntityAccessInfo.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            TimeStampDAO.NVC_TIME_STAMP_DAO
    );
	
	/**
	 * The default constructor. 
	 */
	public NVEntityAccessInfo()
	{
		super(NVC_NVENTITY_ACCESS_INFO);
	}
	
	/**
	 * This constructor instantiates NVEntityAccessInfo based on given NVConfigEntity parameter.
	 * @param nvc the NVConfigEntity
	 */
	protected NVEntityAccessInfo(NVConfigEntity nvc)
	{
		super(nvc);
	}
	
	/**
	 * Gets associated NVEntity.
	 * @return ref id of the associated nventity
	 */
	public String getAssociatedNVEntityRefID() 
	{
		return lookupValue(Param.ASSOCIATED_NVE);
	}
	
	/**
	 * Sets associated NVEntity.
	 * @param refID
	 */
	public void setAssociatedNVEntityRefID(String refID) 
	{
		setValue(Param.ASSOCIATED_NVE, refID);
	}
	
	/**
	 * Gets permissions.
	 * @return list of cruds
	 */
	public List<CRUD> getCRUDs() 
	{
		return lookupValue(Param.CRUDS);
	}
	
	/**
	 * Sets permissions.
	 * @param cruds
	 */
	public void setCRUDs(List<CRUD> cruds) 
	{
		setValue(Param.CRUDS, cruds);
	}
	
	/**
	 * Gets expiration date.
	 * @return expiration date in millis
	 */
	public long getExpirationDate()
	{
		return lookupValue(Param.EXPIRATION_DATE);
	}
	
	/**
	 * Checks if given CRUD is permitted.
	 * @param crud
	 * @return true if permitted
	 */
	public boolean isPermitted(CRUD crud)
	{
		if (getCRUDs() != null)
		{
			for (CRUD c : getCRUDs())
			{
				if (crud == c)
				{
					return true;
				}
			}
		}
		return false;	
	}
	
	
	/**
	 * Sets expiration date.
	 * @param date
	 */
	public void setExpirationDate(long date)
	{
		setValue(Param.EXPIRATION_DATE, date);
	}

}