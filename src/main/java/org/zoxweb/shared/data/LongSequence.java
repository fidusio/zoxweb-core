package org.zoxweb.shared.data;

import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;
import  org.zoxweb.shared.filters.LowerCaseFilter;

@SuppressWarnings("serial")
public class LongSequence
	extends SetNameDescriptionDAO
{
	public enum Param
		implements GetNVConfig
	{
		SEQUENCE(NVConfigManager.createNVConfig("sequence", "Sequence", "Sequence", true, true, long.class)),
		NAME(NVConfigManager.createNVConfig("name", "Name", "Name", true, true, true, String.class, LowerCaseFilter.SINGLETON)),
		DEFAULT_INCREMENT(NVConfigManager.createNVConfig("default_increment", "Default Increment", "DefaultIncrement", true, true, long.class)),
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
	
	
	
	private volatile long upperValue;
	
	public static final NVConfigEntity NVC_LONG_SEQUENCE = new NVConfigEntityPortable(
	        "long_sequence",
	        null,
	        "LongSequence",
	        true,
	        false,
	        false,
	        false,
	        LongSequence.class,
	        SharedUtil.extractNVConfigs(Param.values()),
	        null,
	        false,
	        SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
	);

	public LongSequence()
	{
		super(NVC_LONG_SEQUENCE);
		// TODO Auto-generated constructor stub
	}
	
	public synchronized void setSequenceValue(long val)
	{
		setValue(Param.SEQUENCE, val);
	}
	
	
	public synchronized long getSequenceValue()
	{
		return lookupValue(Param.SEQUENCE);
	}
	
	
	public void setDefaultIncrement(long increment)
	{
		setValue(Param.DEFAULT_INCREMENT, increment);
	}
	
	
	public long getDefaultIncrement()
	{
		return lookupValue(Param.DEFAULT_INCREMENT);
	}

	public long getUpperValue() {
		return upperValue;
	}

	public void setUpperValue(long upperValue) {
		this.upperValue = upperValue;
	}
	
	
	
	

}
