package org.zoxweb.shared.data;


import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.Const.TimeInMillis;


@SuppressWarnings("serial")
public class ScheduleTypeDAO
extends SetNameDescriptionDAO
{

	public enum Param
		implements GetNVConfig
	{
		SCHEDULE_TYPE(NVConfigManager.createNVConfig("schedule_type", "Schedule type", "ScheduleType", true, false, false, true, String.class, null)),
		START(NVConfigManager.createNVConfig("start", "Start time.", "Start", true, false, false, true, String.class, null)),
		END(NVConfigManager.createNVConfig("end", "End time", "End", true, false, false, true, String.class, null)),
		
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
	
	public static final NVConfigEntity NVC_SCHEDULE_TYPE_DAO = new NVConfigEntityLocal(
	        "schedule_type_dao",
	        null,
	        ScheduleTypeDAO.class.getSimpleName(),
	        true,
	        false,
	        false,
	        false,
	        ScheduleTypeDAO.class,
	        SharedUtil.extractNVConfigs(Param.values()),
	        null,
	        false,
	        SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
	);
	
	private transient long relativeStartMillis;
	private transient long relativeEndMillis;

	public ScheduleTypeDAO()
	{
		super(NVC_SCHEDULE_TYPE_DAO);
	}
	
	
	public String getScheduleType()
	{
		return lookupValue(Param.SCHEDULE_TYPE);
	}
	
	public void setScheduleType(String type)
	{
		setValue(Param.SCHEDULE_TYPE, type);
	}
	
	public String getStart()
	{
		return lookupValue(Param.START);
	}
	
	public void setStart(String start)
	{
		setValue(Param.START, start);
	}
	
	
	public String getEnd()
	{
		return lookupValue(Param.END);
	}
	
	public void setEnd(String end)
	{
		setValue(Param.END, end);
	}
	
	public long getStartInMillis()
	{
		return TimeInMillis.toMillis(getStart());
	}
	
	
	public long getEndInMillis()
	{
		return TimeInMillis.toMillis(getEnd());
	}


	public long getRelativeStartMillis() {
		return relativeStartMillis;
	}


	public void setRelativeStartMillis(long relativeStartMillis) {
		this.relativeStartMillis = relativeStartMillis;
	}


	public long getRelativeEndMillis() {
		return relativeEndMillis;
	}


	public void setRelativeEndMillis(long relativeMillis) {
		this.relativeEndMillis = relativeMillis;
	}
	
	
}
	

