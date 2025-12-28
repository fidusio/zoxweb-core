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

import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.TimeStampInterface;

/**
 *
 */
@SuppressWarnings("serial")
public abstract class TimeStampDAO
	extends SetNameDescriptionDAO
	implements TimeStampInterface
{
	
	public enum Param
		implements GetNVConfig
	{
		CREATION_TS(NVConfigManager.createNVConfig("creation_ts", "Creation timestamp (in millis).", "CreationTS", true, false, false, true, Date.class, null)),
		LAST_UPDATE_TS(NVConfigManager.createNVConfig("last_update_ts", "Last update timestamp (in millis).", "LastUpdateTS", true, false, false, true, Date.class, null)),
		LAST_READ_TS(NVConfigManager.createNVConfig("last_read_ts", "Last read timestamp (in millis).", "LastReadTS", true, false, false, true, Date.class, null)),
		
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
	
	public static final NVConfigEntity NVC_TIME_STAMP_DAO = new NVConfigEntityPortable(
	        "time_stamp_dao",
            null,
            TimeStampDAO.class.getSimpleName(),
            true,
            false,
            false,
            false,
            TimeStampDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );

	/**
	 * This constructor instantiates TimeStampDAO with given NVConfigEntity.
	 * @param nvce
	 */
	protected TimeStampDAO(NVConfigEntity nvce)
	{
		super(nvce);
	}
	
	/**
	 * Returns the time (in milliseconds) when the file was created or uploaded into a system or domain.
	 * @return creation time
	 */
	public long getCreationTime() 
	{
		return lookupValue(Param.CREATION_TS);
	}
	
	/**
	 * Sets the time (in milliseconds) when the file was created or uploaded into a system or domain.
	 * @param ts
	 */
	public void setCreationTime(long ts) 
	{
		setValue(Param.CREATION_TS, ts);
	}
	
	/**
	 * Returns the last time (in milliseconds) the file was updated.
	 * @return last time updated 
	 */
	public long getLastTimeUpdated() 
	{
		return lookupValue(Param.LAST_UPDATE_TS);
	}
	
	/**
	 * Sets the last time (in milliseconds) the file was updated.
	 * @param ts
	 */
	public void setLastTimeUpdated(long ts) 
	{
		setValue(Param.LAST_UPDATE_TS, ts);
	}
	
	/**
	 * Returns the last time (in milliseconds) the file was read.
	 * @return last time read 
	 */
	public long getLastTimeRead() 
	{
		return lookupValue(Param.LAST_READ_TS);
	}
	
	/**
	 * Sets the last time (in milliseconds) the file was read.
	 * @param ts
	 */
	public void setLastTimeRead(long ts) 
	{
		setValue(Param.LAST_READ_TS, ts);
	}
	
}