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
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

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
		// DO NOT CHANGE to nanos millis are plenty
		// the million granularity of nanos might cause long issues
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
        else if (o instanceof Appointment)
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
	public boolean reset(boolean runOnce) {
		return false;
	}


	/**
	 * @return
	 */
	@Override
	public long execCount()
	{
		return 0;
	}

	public boolean isClosed()
	{
		return isClosed;
	}


	/**
	 * Returns the remaining delay associated with this object, in the
	 * given time unit.
	 *
	 * @param unit the time unit
	 * @return the remaining delay; zero or negative values indicate
	 * that the delay has already elapsed
	 */
	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(getExpirationInMillis() - System.currentTimeMillis() , TimeUnit.MILLISECONDS);
	}

	/**
	 * Compares this object with the specified object for order.  Returns a
	 * negative integer, zero, or a positive integer as this object is less
	 * than, equal to, or greater than the specified object.
	 *
	 * <p>The implementor must ensure
	 * {@code sgn(x.compareTo(y)) == -sgn(y.compareTo(x))}
	 * for all {@code x} and {@code y}.  (This
	 * implies that {@code x.compareTo(y)} must throw an exception iff
	 * {@code y.compareTo(x)} throws an exception.)
	 *
	 * <p>The implementor must also ensure that the relation is transitive:
	 * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
	 * {@code x.compareTo(z) > 0}.
	 *
	 * <p>Finally, the implementor must ensure that {@code x.compareTo(y)==0}
	 * implies that {@code sgn(x.compareTo(z)) == sgn(y.compareTo(z))}, for
	 * all {@code z}.
	 *
	 * <p>It is strongly recommended, but <i>not</i> strictly required that
	 * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
	 * class that implements the {@code Comparable} interface and violates
	 * this condition should clearly indicate this fact.  The recommended
	 * language is "Note: this class has a natural ordering that is
	 * inconsistent with equals."
	 *
	 * <p>In the foregoing description, the notation
	 * {@code sgn(}<i>expression</i>{@code )} designates the mathematical
	 * <i>signum</i> function, which is defined to return one of {@code -1},
	 * {@code 0}, or {@code 1} according to whether the value of
	 * <i>expression</i> is negative, zero, or positive, respectively.
	 *
	 * @param o the object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object
	 * is less than, equal to, or greater than the specified object.
	 * @throws NullPointerException if the specified object is null
	 * @throws ClassCastException   if the specified object's type prevents it
	 *                              from being compared to this object.
	 */
	@Override
	public int compareTo(Delayed o) {
		// may cause error
		return SharedUtil.signum( getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
	}
}