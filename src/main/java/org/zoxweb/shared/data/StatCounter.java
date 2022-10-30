package org.zoxweb.shared.data;



import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.zoxweb.shared.util.Const.TimeInMillis;

import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;


@SuppressWarnings("serial")
public class StatCounter
	extends TimeStampDAO
{

	
	
	public enum Param
	implements GetNVConfig
	{
		COUNTER(NVConfigManager.createNVConfig("counter", "Counter", "Counter", true, true, long.class)),
		REFERENCE_TS(NVConfigManager.createNVConfig("ref_ts", "Time stamp reference", "TimeStampReference", true, false, false, true, Date.class, null)),
		
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
	
	public static final NVConfigEntity NVC_STAT_COUNTER_DAO = new NVConfigEntityLocal(
	        "stat_counter",
	        null,
	        StatCounter.class.getSimpleName(),
	        true,
	        false,
	        false,
	        false,
	        StatCounter.class,
	        SharedUtil.extractNVConfigs(Param.values()),
	        null,
	        false,
	        TimeStampDAO.NVC_TIME_STAMP_DAO
	);

	public StatCounter()
	{
		super(NVC_STAT_COUNTER_DAO);
		// TODO Auto-generated constructor stub
		setCreationTime(System.currentTimeMillis());
		setReferenceTime(getCreationTime());

		increment(0);
	}
	
	
	/**
	 * Increment by 1
	 * @return
	 */
	public long increment()
	{
		return increment(1);
	}

	
	public synchronized long increment(long increment)
	{
		long val = getCounter() + increment;
		setCounter(val);
		setLastTimeUpdated(System.currentTimeMillis());
		return val;
	}
	
	public long getCounter()
	{
		return lookupValue(Param.COUNTER);
	}
	
	public synchronized void setCounter(long counter)
	{
		setValue(Param.COUNTER, counter);
	}
	
	/**
	 * Return the time in millis
	 * @return
	 */
	public long getReferenceTime()
	{
		return lookupValue(Param.REFERENCE_TS);
	}
	
	public void setReferenceTime()
	{
		setReferenceTime(System.currentTimeMillis());
	}
	
	public synchronized void setReferenceTime(long ts)
	{
		setValue(Param.REFERENCE_TS, ts);
	}
	
	public double rate(TimeUnit tut)
	{
		// default in millis
		double rate = (double)getCounter()/(double)delta();
		switch(tut)
		{
			case DAYS:
				rate *= TimeInMillis.DAY.MILLIS;
			break;
			case HOURS:
				rate *= TimeInMillis.HOUR.MILLIS;
			break;
			case MINUTES:
				rate *= TimeInMillis.MINUTE.MILLIS;
			break;
			case NANOSECONDS:
				rate /= 1000000;
			break;
			case SECONDS:
				rate *= TimeInMillis.SECOND.MILLIS;
			break;
			case MILLISECONDS:
			// no conversion 
		default:
			break;
		}
		
		return rate;
	}
	
	
	
	
	
	/**
	 * Return lastime updated - reference time in millis
	 * @return
	 */
	public long delta()
	{
		return getLastTimeUpdated() - getReferenceTime();
	}
	
	/**
	 * 
	 * @return
	 */
	public long deltaNow()
	{
		return System.currentTimeMillis() - getReferenceTime();
	}
	
	public long deltaSinceCreation()
	{
		return System.currentTimeMillis() - getCreationTime();
	}
}
