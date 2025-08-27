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

import java.util.Date;

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
public class VMInfoDAO 
    extends SetNameDescriptionDAO
{

	public enum Param
		implements GetNVConfig
    {
		CORE_COUNT(NVConfigManager.createNVConfig("core_count", "Number of available CPU cores", "CoreCount", true, true, Integer.class)),
		FREE_MEMORY(NVConfigManager.createNVConfig("free_memory", "Free memory", "FreeMemory", true, true, Long.class)),
		TOTAL_MEMORY(NVConfigManager.createNVConfig("total_memory", "Total memory", "TotalMemory", true, true, Long.class)),
		USED_MEMORY(NVConfigManager.createNVConfig("used_memory", "Used memory", "UsedMemory", true, true, Long.class)),
		MAX_MEMORY(NVConfigManager.createNVConfig("max_memory", "Maximum memory", "MaxMemory", true, true, Long.class)),
		TIME_STAMP(NVConfigManager.createNVConfig("time_stamp", "Time stamp", "TimeStampInterface", true, true, Date.class))
		
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
	
	public static final NVConfigEntity NVC_VMINFO_DAO = new NVConfigEntityPortable(
            "vm_info_dao",
            null,
            VMInfoDAO.class.getSimpleName(),
            true,
            false,
            false,
            false,
            VMInfoDAO.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );

	/**
	 * The default constructor.
	 */
	public VMInfoDAO()
	{
		super(NVC_VMINFO_DAO);
	}
	
	/**
	 * Returns the core count.
	 * @return core counts 
	 */
	public int getCoreCount()
	{
		return lookupValue(Param.CORE_COUNT);
	}
	
	/**
	 * Sets the core count.
	 * @param cc
	 */
	public void setCoreCount(int cc)
	{
		setValue(Param.CORE_COUNT, cc);
	}
	
	/**
	 * Returns the free memory.
	 * @return free memory
	 */
	public long getFreeMemory()
	{
		return lookupValue(Param.FREE_MEMORY);
	}
	
	/**
	 * Sets the free memory.
	 * @param freeMemory
	 */
	public void setFreeMemory(long freeMemory)
	{
		setValue(Param.FREE_MEMORY, freeMemory);
	}
	
	/**
	 * Returns the max memory.
	 * @return max memory
	 */
	public long getMaxMemory()
	{
		return lookupValue(Param.MAX_MEMORY);
	}
	
	/**
	 * Sets the max memory.
	 * @param maxMemory
	 */
	public void setMaxMemory(long maxMemory)
	{
		setValue(Param.MAX_MEMORY, maxMemory);
	}
	
	/**
	 * Returns the used memory.
	 * @return used memory
	 */
	public long getUsedMemory()
	{
		return lookupValue(Param.USED_MEMORY);
	}
	
	/**
	 * Sets the used memory.
	 * @param usedMemory
	 */
	public void setUsedMemory(long usedMemory)
	{
		setValue(Param.USED_MEMORY, usedMemory);
	}
	
	/**
	 * Returns the time stamp.
	 * @return time stamp
	 */
	public Date getTimeStamp()
	{
		return new Date((Long) lookupValue(Param.TIME_STAMP));
	}
	
	/**
	 * Sets the time stamp.
	 * @param date
	 */
	public void setTimeStamp(Date date)
	{
		setValue(Param.TIME_STAMP, date.getTime());
	}
	
	/**
	 * Returns the total memory.
	 * @return total memory
	 */
	public long getTotalMemory()
	{
		return lookupValue(Param.TOTAL_MEMORY);
	}
	
	/**
	 * Sets the total memory.
	 * @param totalMemory
	 */
	public void setTotalMemory(long totalMemory)
	{
		setValue(Param.TOTAL_MEMORY, totalMemory);
	}
	
}