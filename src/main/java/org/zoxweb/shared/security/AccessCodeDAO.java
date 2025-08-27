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
package org.zoxweb.shared.security;

import org.zoxweb.shared.data.DataConst.DataParam;
import org.zoxweb.shared.data.DataContentDAO.Param;
import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SetCanonicalID;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

/**
 * @author mnael
 *
 */
@SuppressWarnings("serial")
public class AccessCodeDAO 
    extends SetNameDescriptionDAO
    implements SetCanonicalID
{
	/**
	 * Contains login token parameters.
	 */
	public enum Params
		implements GetNVConfig
	{
		ACCESS_CODE(NVConfigManager.createNVConfig("access_code", "The Access Code", "AccessCode", false, true, false, String.class, FilterType.ENCRYPT)),
		ACCESS_COUNT(NVConfigManager.createNVConfig("access_count", "Number of times this access code was used", "AccessCount",false, false, Long.class)),
		ACCESS_QUOTA(NVConfigManager.createNVConfig("access_quota", "Maximum times the access code can be used 0 for ever", "AccessQuota",true, true, Long.class)),
		CASE_SENSITIVE(NVConfigManager.createNVConfig("case_sensitive", "True the access code is a exact match", "CaseSensitive", false, false, Boolean.class)),
		CANONICAL_ID(DataParam.CANONICAL_ID.getNVConfig()),
		DESCRIPTION(NVConfigManager.createNVConfig("description", null, "Description", false, true, false, true, false, String.class, null)),
		SESSION_DURATION(NVConfigManager.createNVConfig("session_duration", "The session duration in millis", "SessionDuration", false, true, Long.class)),
		
		;
		
		private final NVConfig nvc;
		
		Params(NVConfig nvc)
		{
            this.nvc = nvc;
		}
		
		public NVConfig getNVConfig() 
		{
			return nvc;
		}
	}
	
	public static final NVConfigEntity NVC_ACCESS_CODE_DAO = new NVConfigEntityPortable(
																					"access_code_dao", 
																					null , 
																					"AccessCodeDAO", 
																					true, 
																					false, 
																					false, 
																					false, 
																					AccessCodeDAO.class, 
																					SharedUtil.extractNVConfigs(Params.values()), 
																					null, 
																					false, 
																					SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
																				);
	
	public AccessCodeDAO()
	{
		super(NVC_ACCESS_CODE_DAO);
	}
	
	public String getAccessCode()
	{
		return lookupValue(Params.ACCESS_CODE);
	}

	public synchronized void setAccessCode(String code)
	{
		setValue(Params.ACCESS_CODE, SharedStringUtil.trimOrNull(code));
		setAccessCount(0);
	}
	
	public boolean isCaseSensitive()
	{
		return lookupValue(Params.CASE_SENSITIVE);
	}
	
	public synchronized void setCaseSensitive(boolean stat)
	{
		setValue(Params.CASE_SENSITIVE, stat);
	}
	
	public synchronized void setAccessQuota(long limit)
	{
		setValue(Params.ACCESS_QUOTA, limit);
	}
	
	public long getAccessQuota()
	{
		return lookupValue(Params.ACCESS_QUOTA);
	}
	
	
	public long getAccessCount()
	{
		return lookupValue(Params.ACCESS_COUNT);
	}
	
	private synchronized void setAccessCount(long count)
	{
		setValue(Params.ACCESS_COUNT, count);
	}
	
	public synchronized long validateAccessCode(String toValidate)
		throws AccessException
	{
		String currentAccessCode = getAccessCode();

		if(currentAccessCode != null)
		{
			toValidate = SharedStringUtil.trimOrNull(toValidate);

			if (toValidate == null)
			{
				throw new AccessException("Invalid access code");
			}
			
			if (!isCaseSensitive())
			{
				currentAccessCode = currentAccessCode.toLowerCase();
				toValidate = toValidate.toLowerCase();
			}
			
			if (!currentAccessCode.equals(toValidate))
			{
				throw new AccessException("Access Code Denied.");				
			}
		}
		
		
		setAccessCount(getAccessCount() + 1);

		if (getAccessQuota() > 0 && getAccessCount() > getAccessQuota())
		{
			throw new AccessException("Access Code Denied validatation quota reached.");
		}
		
		return getAccessCount();
		
	}

	/* (non-Javadoc)
	 * @see org.zoxweb.shared.util.CanonicalID#toCanonicalID()
	 */
	@Override
	public String toCanonicalID()
    {
		return getCanonicalID();
	}

	/* (non-Javadoc)
	 * @see org.zoxweb.shared.util.SetCanonicalID#getCanonicalID()
	 */
	@Override
	public String getCanonicalID()
	{
		return lookupValue(Param.CANONICAL_ID);
	}

	/* (non-Javadoc)
	 * @see org.zoxweb.shared.util.SetCanonicalID#setCanonicalID(java.lang.String)
	 */
	@Override
	public void setCanonicalID(String canonicalID) 
	{
		setValue(Params.CANONICAL_ID, canonicalID);
	}
	
	/**
	 * Get the session duration in milli second -1 for ever, 0 it the caller default session duration
	 * @return the session duration in millis
	 */
	public long getSessionDuration()
	{
		return lookupValue(Params.SESSION_DURATION);
	}

	/**
	 * 
	 * @param duration
	 */
	public void setSessionDuration(long duration)
	{
		setValue(Params.SESSION_DURATION, duration);
	}

}