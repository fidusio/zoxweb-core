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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import org.zoxweb.server.io.ByteBufferUtil;

import org.zoxweb.server.net.security.SSLSessionData;

import org.zoxweb.shared.util.GetDescription;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.NVGenericMap;

public abstract class ProtocolSessionProcessor
	implements GetName, GetDescription, Closeable, Runnable
{

	private volatile boolean selectable = true;
	private volatile SelectorController selectorController;
	private volatile InetFilterRulesManager outgoingInetFilterRulesManager;
	private volatile int defaultReadBufferSize = ByteBufferUtil.DEFAULT_BUFFER_SIZE;
	protected volatile ByteBuffer bBuffer = null;
	private volatile SSLSessionData outputSSLSessionData;
	private volatile SSLSessionData inputSSLSessionData;
	private volatile SelectionKey attachment;
	private volatile NVGenericMap properties = null;
	
	protected ProtocolSessionProcessor()
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
	
	public boolean isSelectable()
	{
		return selectable;
	}
	
	
	protected void setSelectable(boolean stat)
	{
		selectable = stat;
	}
	
	

	
	public synchronized void attach(SelectionKey sk)
	{
		attachment = sk;
	}
	
	protected synchronized SelectionKey detach()
	{
		SelectionKey ret = attachment;
		attachment = null;
		return ret;
	}
	
	public void run()
	{
		try
		{
			readData(detach());
		}
		catch(Exception e)
		{
			
		}
		// very crucial be set to true after the processRead call
		//selectable = true;
		setSelectable(true);
	}
	
	
	protected abstract void readData(SelectionKey sk);


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
	
	
	public synchronized void postOp()
	{
		if (bBuffer != null)
		{
			ByteBufferUtil.cache(bBuffer);
			//bBuffer = null;
		}
	}

	public InetFilterRulesManager getOutgoingInetFilterRulesManager() 
	{
		return outgoingInetFilterRulesManager;
	}


	public void setOutgoingInetFilterRulesManager(InetFilterRulesManager inetFilterRulesManager) 
	{
		this.outgoingInetFilterRulesManager = inetFilterRulesManager;
	}




	public SSLSessionData getOutputSSLSessionData()
	{
		return outputSSLSessionData;
	}



	public void setOutputSSLSessionData(SSLSessionData outputSSLSessionData)
	{
		this.outputSSLSessionData = outputSSLSessionData;
	}



	public SSLSessionData getInputSSLSessionData()
	{
		return inputSSLSessionData;
	}



	public void setInputSSLSessionData(SSLSessionData inputSSLSessionData)
	{
		this.inputSSLSessionData = inputSSLSessionData;
	}

	
	public void setProperties(NVGenericMap prop)
	{
		properties = prop;
	}
	
	public NVGenericMap getProperties()
	{
		return properties;
	}

}
