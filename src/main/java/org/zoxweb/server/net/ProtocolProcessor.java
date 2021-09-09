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

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.zoxweb.server.io.ByteBufferUtil;


import org.zoxweb.shared.util.GetDescription;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.NVGenericMap;

public abstract class ProtocolProcessor
	implements GetName, GetDescription, Closeable, Consumer<SelectionKey>
{


	private volatile SelectorController selectorController;
	private volatile InetFilterRulesManager outgoingInetFilterRulesManager;
	private volatile int defaultReadBufferSize = ByteBufferUtil.DEFAULT_BUFFER_SIZE;

	private volatile NVGenericMap properties = null;
	protected volatile Executor executor;
	
	protected ProtocolProcessor()
	{
		
	}
	
	public int getReadBufferSize()
	{
		return defaultReadBufferSize;
	}
	
	public synchronized void setReadBufferSize(int size)
	{
		if (size < 512)
			throw new IllegalArgumentException("Invalid size " + size + " min allowed size 512 bytes");
		defaultReadBufferSize = size;
	}
	


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
	


	protected void acceptConnection(NIOChannelCleaner ncc, AbstractSelectableChannel asc, boolean isBlocking) throws IOException {
		selectorController.register(ncc,  asc, SelectionKey.OP_READ, this, isBlocking);
	}

	public InetFilterRulesManager getOutgoingInetFilterRulesManager() 
	{
		return outgoingInetFilterRulesManager;
	}


	public void setOutgoingInetFilterRulesManager(InetFilterRulesManager inetFilterRulesManager) 
	{
		this.outgoingInetFilterRulesManager = inetFilterRulesManager;
	}

	public boolean channelReadState(Channel channel){return true;}

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

}
