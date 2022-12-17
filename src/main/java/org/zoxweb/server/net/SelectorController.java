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
package org.zoxweb.server.net;

import org.zoxweb.shared.util.SharedUtil;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is used to allow the Selector object to be used in multi-threaded environment.
 * @author mnael
 *
 */
public class SelectorController 
{
	private final Selector selector;
	private final Lock selectLock = new ReentrantLock();
	private final Lock lock = new ReentrantLock();
	
	
	/**
	 * Create a Selector Controller
	 * @param selector the selector object
	 */
	public SelectorController(Selector selector)
	{
		this.selector = selector;
	}
	
	/**
	 * Register the socket channel with the selector by applying the following procedure:
	 * <ol>
	 * <li> invoke lock on the general lock
	 * <li> wakeup the selector
	 * <li> invoke lock on the select lock
	 * <li> register the channel with the selector
	 * <li> unlock the general lock
	 * <li> unlock the select lock
	 * <li> return the selection key
	 * </ol>
	 * @param ch selectable channel
	 * @param ops channel ops
	 * @return SelectionKey
	 * @throws IOException is case of error
	 */
	public SelectionKey register(AbstractSelectableChannel ch, int ops) throws IOException
	{
		return register(null, ch, ops, null, false);
	}

	public SelectionKey register(AbstractSelectableChannel ch, int ops, Object attachment) throws IOException
	{
		return register(null, ch, ops, attachment, false);
	}



	

	/**
	 * Register the socket channel with the selector by applying the following procedure:
	 * <ol>
	 * <li> invoke lock on the general lock
	 * <li> wakeup the selector
	 * <li> invoke lock on the select lock
	 * <li> register the channel with the selector
	 * <li> unlock the general lock
	 * <li> unlock the select lock
	 * <li> return the selection key
	 * </ol>
	 * @param niocc nio channel cleaner
	 * @param ch selectable channel
	 * @param ops channel ops
	 * @param attachment attachment object
	 * @param blocking true if blocking
	 * @return SelectionKey registered selection key with the selector
	 * @throws IOException is case of error
	 */
	public SelectionKey register(NIOChannelCleaner niocc,
								 AbstractSelectableChannel ch,
								 int ops,
								 Object attachment,
								 boolean blocking) throws IOException
	{
		SelectionKey ret;
		try
		{
			// block the select lock just in case
			lock.lock();
			// wakeup the selector
			selector.wakeup();
			// invoke the main lock
			selectLock.lock();
			SharedUtil.getWrappedValue(ch).configureBlocking(blocking);


			ret = SharedUtil.getWrappedValue(ch).register(selector, ops, new SKAttachment(attachment));
			if (niocc != null)
			{
				niocc.add(ret);
			}
		}
		finally
		{
			lock.unlock();
			selectLock.unlock();
			
		}
		
		return ret;
	}


	public SelectionKey register(NIOChannelCleaner niocc,
								 AbstractSelectableChannel ch,
								 int ops,
								 Object attachment)
			throws IOException
	{
		SelectionKey ret;
		try
		{
			// block the select lock just in case
			lock.lock();
			// wakeup the selector
			selector.wakeup();
			// invoke the main lock
			selectLock.lock();
			//SharedUtil.getWrappedValue(ch).configureBlocking(blocking);
			ret = SharedUtil.getWrappedValue(ch).register(selector, ops, new SKAttachment(attachment));
			if (niocc != null)
			{
				niocc.add(ret);
			}
		}
		finally
		{
			lock.unlock();
			selectLock.unlock();

		}

		return ret;
	}

	
	/**
	 * Blocking select
	 * @return number of selection match
	 * @throws IOException is case of error
	 */
	public int select() throws IOException
	{
		return select(0);
	}
	
	/**
	 * 
	 * @param timeout selection timeout
	 * @return number of selected keys
	 * @throws IOException in case of error
	 */
	public int select(long timeout) throws IOException
	{
		try
		{
			// we must lock
			lock.lock();
			// and immediately unlock caused by a very fast thread;
			lock.unlock();
			selectLock.lock();
			return selector.select(timeout);
		}
		finally
		{
			selectLock.unlock();
		}
	}
	
	public void cancelSelectionKey(SelectionKey sk)
	{

		if (sk != null)
		{
			try
			{
				// block the select lock just in case
				lock.lock();
				// wakeup the selector
				selector.wakeup();
				// invoke the main lock
				selectLock.lock();
				sk.cancel();


			}
			finally
			{
				lock.unlock();
				selectLock.unlock();
			}
		}

	}

	public void cancelSelectionKey(SelectableChannel ch)
	{
		if(ch != null){
			cancelSelectionKey(ch.keyFor(getSelector()));
		}
	}
	
	

	
	public Selector getSelector()
	{
		return selector;
	}

}
