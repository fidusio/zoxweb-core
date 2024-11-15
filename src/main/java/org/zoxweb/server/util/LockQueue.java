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
package org.zoxweb.server.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.SimpleQueue;

public class LockQueue
{

	private SimpleQueue<Lock> queue = new SimpleQueue<Lock>();

	/**
	 * Create an empty lock queue
	 */
	public LockQueue()
    {
	}
	
	/**
	 * Create a lock queue specific  size
	 * @param size of the initial queue.
	 * @exception IllegalArgumentException if the queue size < 0 
	 */
	public LockQueue(int size)
    {
		if (size < 0)
			throw new IllegalArgumentException("Invalid queue size " + size);
		
		for (int i=0; i < size; i++)
		{
			queue.queue(new ReentrantLock());
		}
	}

	/**
	 * This method return the next available lock, will block the lock queue is empty
	 * @return the next available lock
	 */
	public Lock dequeueLock()
    {
		Lock ret = null;
		
		while ((ret = queue.dequeue()) == null)
        {
			synchronized(this)
            {
				try
                {
					wait(Const.TimeInMillis.MILLI.MILLIS*500);
				}
				catch (InterruptedException e)
                {
					e.printStackTrace();
				}
			}
		}

		ret.lock();

		return ret;
	}
	
	/**
	 * Add a lock to the queue
	 * @param lock to be added
	 */
	public void queueLock(Lock lock)
    {
		SUS.checkIfNulls("Can't add null lock", lock);
		lock.unlock();
		queue.queue(lock);

		synchronized(this)
        {
			notifyAll();
		}
	}
	
	/**
	 * Return the number of available locks
	 * @return available locks
	 */
	public int availableLocks()
	{
		return queue.size();
	}

}