package org.zoxweb.shared.data;


import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("serial")
public class CurrentTimestamp
	extends SetNameDescriptionDAO
{
	

	public enum Param
	implements GetNVConfig
	{
		TIMESTAMP(NVConfigManager.createNVConfig("timestamp", "Current time value", "Timestamp", true, false, false, true, Long.class, null)),
		UNIT(NVConfigManager.createNVConfig("unit", "current time value unit", "Unit", true, false, false, true, TimeUnit.class, null)),
		SOURCE(NVConfigManager.createNVConfig("source", "current time value unit", "Source", false, false, String.class)),
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
	
	public static final NVConfigEntity NVC_CURRENT_TIMESTAMP = new NVConfigEntityPortable(
	        "current_timestamp",
	        null,
	        CurrentTimestamp.class.getSimpleName(),
	        true,
	        false,
	        false,
	        false,
	        CurrentTimestamp.class,
	        SharedUtil.extractNVConfigs(Param.values()),
	        null,
	        false,
	        SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
	);
	
	public CurrentTimestamp()
	{
		this(System.currentTimeMillis(), TimeUnit.MILLISECONDS, null);
		// TODO Auto-generated constructor stub
	}
	
	public CurrentTimestamp(long currentTimeValue, TimeUnit tut)
	{
		this(currentTimeValue, tut, null);
	}
	
	public CurrentTimestamp(long currentTimeValue, TimeUnit tut, String source)
	{
		super(NVC_CURRENT_TIMESTAMP);
		setValue(Param.TIMESTAMP, currentTimeValue);
		setValue(Param.UNIT, tut);
		setValue(Param.SOURCE, source);
	}
	
	public long currentTimestamp()
	{
		return lookupValue(Param.TIMESTAMP);
	}
	
	public TimeUnit unit()
	{
		return lookupValue(Param.UNIT);
	}
	
	
	
}
