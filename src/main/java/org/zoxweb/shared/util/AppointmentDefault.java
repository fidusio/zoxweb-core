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
package org.zoxweb.shared.util;

import org.zoxweb.shared.util.Const.TimeInMillis;

import java.io.Serializable;

@SuppressWarnings("serial")
public class AppointmentDefault
    implements Appointment, Serializable
{
	
	private long delay;
	private long expiration;
	private long expirationInMicros;
	private boolean isClosed = false;

	public AppointmentDefault()
    {
		this(0);
	}
	
	public AppointmentDefault(String time)
	{
		this(TimeInMillis.toMillis(time));
	}
	
	public AppointmentDefault(long delayInMillis)
    {
		setDelayInNanos(delayInMillis, 0);
	}
	
	public AppointmentDefault(long delayInMillis, long nanoOffset)
	{
		setDelayInNanos(delayInMillis, nanoOffset);
	}

	/**
	 * @return delay in millis
	 */
	@Override
	public long getDelayInMillis()
    {
		return delay;
	}

	@Override
	public  void setDelayInMillis(long delayInMillis)
    {	
	  setDelayInNanos(delayInMillis, 0);
	}

	/**
	 * Set the delay in nanos and adjust the expirations in millis and micros
	 * @param delayInMillis
	 * @param nanoOffset
	 */
	public synchronized void setDelayInNanos(long delayInMillis, long nanoOffset)
	{
		if(isClosed())
			throw new IllegalArgumentException("Appointment Closed");
	  	delay = delayInMillis;
	  	expiration = System.currentTimeMillis() + delay;
	  	expirationInMicros = (expiration*1000) + Math.abs((nanoOffset%1000000)/1000);
	}

	@Override
	public synchronized long getExpirationInMillis()
    {
		return expiration;
	}

	@Override
	public boolean cancel()
    {
		return false;
	}

	/**
	 * Equals updated supports microseconds equality
	 * @param o to check
	 * @return true o = this or the getExpirationInMicros are equals
	 */
	public boolean equals(Object o)
    {
		if (o == this)
			return true;
        else if (o != null && o instanceof Appointment)
        	return ((Appointment) o).getPreciseExpiration() == getPreciseExpiration();
        return false;
    }

	/**
	 * Adjusted expiration to granular precision beyond millis
	 * @return
	 */
	@Override
	public synchronized long getPreciseExpiration()
	{
		// TODO Auto-generated method stub
		return expirationInMicros;
	}

	/**
	 * Close
	 */
	public synchronized void close()
	{
		isClosed = true;
	}

	@Override
	public void reset(boolean runOnce) {

	}

	public boolean isClosed()
	{
		return isClosed;
	}


}