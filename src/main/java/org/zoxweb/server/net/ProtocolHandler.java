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

import org.zoxweb.shared.util.CloseableType;
import org.zoxweb.shared.util.GetDescription;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.NVGenericMap;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public abstract class ProtocolHandler
	implements GetName, GetDescription, CloseableType, Consumer<SelectionKey>
{

	private static final AtomicLong ID_COUNTER = new AtomicLong();

	protected final long id = ID_COUNTER.incrementAndGet();

	private volatile SelectorController selectorController;
	private volatile InetFilterRulesManager outgoingInetFilterRulesManager;

	protected volatile SocketChannel phSChannel;
	protected volatile SelectionKey phSK;




	private volatile NVGenericMap properties = null;
	protected volatile Executor executor;
	protected final AtomicBoolean isClosed = new AtomicBoolean(false);

	protected ProtocolHandler()
	{
		
	}





	public long getID(){return id;}


	/**
	 * @return the selector
	 */
	public SelectorController getSelectorController() 
	{
		return selectorController;
	}


	/**
	 * @param selectorController the selector to set
	 */
	public void setSelectorController(SelectorController selectorController)
	{
		this.selectorController = selectorController;
	}


	public void setupConnection(AbstractSelectableChannel asc, boolean isBlocking) throws IOException
	{
		phSChannel = (SocketChannel) asc;
		getSelectorController().register(phSChannel, SelectionKey.OP_READ, this, isBlocking);
	}


	public InetFilterRulesManager getOutgoingInetFilterRulesManager() 
	{
		return outgoingInetFilterRulesManager;
	}


	public void setOutgoingInetFilterRulesManager(InetFilterRulesManager inetFilterRulesManager) 
	{
		this.outgoingInetFilterRulesManager = inetFilterRulesManager;
	}


	public void setProperties(NVGenericMap prop)
	{
		properties = prop;
	}
	
	public NVGenericMap getProperties()
	{
		return properties;
	}
	public void setExecutor(Executor exec)
	{
		this.executor = exec;
	}

	public Executor getExecutor()
	{
		return executor;
	}



//	public void setSessionCallback(BaseSessionCallback<?> sessionCallback) {
//		throw new UnsupportedOperationException("Can't set session callback");
//	}

	public void close() throws IOException
	{
		if(!isClosed.getAndSet(true))
		{
			close_internal();
		}
	}

	@Override
	public boolean isClosed()
	{
		return isClosed.get()  || (phSChannel != null && !phSChannel.isOpen());
	}

	abstract  protected void close_internal() throws IOException;
}
