package org.zoxweb.shared.data;


import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.util.List;


@SuppressWarnings("serial")
public class ScheduleConfig
	extends TimeStampDAO 
{
	
	 public enum PropParam
	 	implements GetName
	 {
		 URL("url"),
		 ENABLED("enabled"),
		 ON_COMMANDS("on_commands"),
		 OFF_COMMANDS("off_commands"),
		 
	     ;
		 private final String name;
			
		 PropParam (String name) {
	         this.name = name;
	     }
	
	     
	     public String getName() {
	         return name;
	     }
	 }

	 public enum Param
	     implements GetNVConfig 
	 {
	
		 PROPERTIES(NVConfigManager.createNVConfig("properties", "Generic properties", "Properties", true, true, NVGenericMap.class)),
		 SCHEDULES(NVConfigManager.createNVConfigEntity("schedules", "Schedule in cron or time format", "Schedule", true, true, ScheduleType.class, ArrayType.LIST)),
	     ;
	
	     private final NVConfig nvc;
	
	     Param(NVConfig nvc) {
	         this.nvc = nvc;
	     }
	
	     @Override
	     public NVConfig getNVConfig() {
	         return nvc;
	     }
	 }
	
	 public static final NVConfigEntity NVC_CRON_CONFIG = new NVConfigEntityPortable(
	         "schedule_config",
	         null,
	         ScheduleConfig.class.getSimpleName(),
	         true,
	         false,
	         false,
	         false,
	         ScheduleConfig.class,
	         SUS.extractNVConfigs(Param.values()),
	         null,
	         false,
	         TimeStampDAO.NVC_TIME_STAMP_DAO
	 );
	
	public ScheduleConfig()
	{
		super(NVC_CRON_CONFIG);
		// TODO Auto-generated constructor stub
	}
	
	public NVGenericMap getProperties()
	{
		return (NVGenericMap) lookup(Param.PROPERTIES);
	}
	public String getURL()
	{
		return getProperties().getValue((GetName)PropParam.URL);
	}
	
	public void setURL(String url)
	{
		url = FilterType.URL.validate(url);
		getProperties().add(PropParam.URL.getName(), url);
		
	}
	
	
	
	public String[] getOnCommands()
	{
		List<String> nvsl = getProperties().getValue((GetName)PropParam.ON_COMMANDS);
		return nvsl.toArray(new String[nvsl.size()]);
	}
	
	public void setOnCommands(String[] onCommands)
	{
		NVStringList nvsl = SUS.toNVStringList(PropParam.ON_COMMANDS.getName(), onCommands, true);
		getProperties().add(nvsl);
	}
	
	public String[] getOffCommands()
	{
		List<String> nvsl = getProperties().getValue((GetName)PropParam.OFF_COMMANDS);
		return nvsl.toArray(new String[nvsl.size()]);
	}
	
	public void setOffCommands(String[] offCommands)
	{
		NVStringList nvsl = SUS.toNVStringList(PropParam.OFF_COMMANDS.getName(), offCommands, true);
		getProperties().add(nvsl);
	}
	public void setEnabled(boolean status)
	{
		getProperties().add(new NVBoolean(PropParam.ENABLED.getName(), status));
	}
	
	public boolean isEnabled() 
	{
		NVBoolean enabled = (NVBoolean) getProperties().get((GetName)PropParam.ENABLED);
		if(enabled != null)
		{
			return enabled.getValue();
		}
		
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public ArrayValues<NVEntity> getSchedules()
	{
		return (ArrayValues<NVEntity>)lookup(Param.SCHEDULES);
	}
	
	
}
