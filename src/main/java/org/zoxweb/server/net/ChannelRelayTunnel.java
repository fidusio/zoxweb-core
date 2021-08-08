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
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.net.security.SSLSessionData;
import org.zoxweb.server.io.ByteBufferUtil.BufferType;


public class  ChannelRelayTunnel
	extends ProtocolSessionProcessor
{

	private static boolean debug = false;
	private static final transient Logger log = Logger.getLogger(ChannelRelayTunnel.class.getName());

	private ByteChannel readSource;
	private ByteChannel writeDestination;
	private SelectionKey currentSK = null;
	@SuppressWarnings("unused")
	private SelectionKey writeChannelSK;
	private final boolean autoCloseDestination;
	//private ByteBuffer bBuffer = null;
	private Closeable closeInterface = null;
	//private SourceOrigin origin = null;
	
	
	
	public ChannelRelayTunnel(int bufferSize, SocketChannel readSource,
			  				  SocketChannel writeDestination, SelectionKey writeChannelSK, boolean autoCloseDestination,
			  				  SelectorController sc)
	{
		this(bufferSize, readSource, writeDestination, writeChannelSK, autoCloseDestination, sc, null);
	}
	
	
	
	public ChannelRelayTunnel(int bufferSize, SocketChannel readSource,
							  SocketChannel writeDestination, SelectionKey writeChannelSK,boolean autoCloseDestination,
							  SelectorController sc, Closeable closeInterface)
	{
		//this.origin = origin;
		bBuffer = ByteBufferUtil.allocateByteBuffer(BufferType.HEAP, bufferSize);
		this.readSource = readSource;
		this.writeDestination = writeDestination;
		this.writeChannelSK = writeChannelSK;
		this.autoCloseDestination = autoCloseDestination;
		this.closeInterface = closeInterface;
		setSelectorController(sc);
		
	}
	
	
	
	public String getName() {
		// TODO Auto-generated method stub
		return "ChannelRelayTunnel";
	}
	

	public String getDescription() {
		// TODO Auto-generated method stub
		return "NIO Channel Relay Tunnel";
	}
	
//	public SourceOrigin getSourceOrigin()
//	{
//		return origin;
//	}
	
	
	public void close() throws IOException 
	{
		
		// TODO Auto-generated method stub
		if (closeInterface != null)
		{
			IOUtil.close(closeInterface);
		}
		else
		{
			IOUtil.close(readSource);
			getSelectorController().cancelSelectionKey(currentSK);
			
			if (autoCloseDestination)
			{
				IOUtil.close(writeDestination);
			}
			postOp();
		}
	}
	
	
	public synchronized  void accept(SelectionKey key)
	{
		
		try
		{
			
			if (key != null)
				currentSK = key;
			int read;
			do
			{
				bBuffer.clear();
				read = ((SocketChannel)currentSK.channel()).read(bBuffer);
				
				SSLSessionData outputSSLSessionData = getOutputSSLSessionData();
				if (read > 0)
				{
					if (outputSSLSessionData != null)
					{
						outputSSLSessionData.write(writeDestination, bBuffer, true);
						if (debug) log.info("Wrote Encrypted DATA !!!!!");
						
					}
					else
					{
						ByteBufferUtil.write(writeDestination, bBuffer);
						if (debug) log.info(ByteBufferUtil.toString(bBuffer));
					}
				}
			}while(read > 0);
			
	    	if(read < 0) //end of stream we have to close 
	    		IOUtil.close(this);
	    	
		}
		catch(Exception e)
		{
			log.info("error:" +e);
			if (!(e instanceof IOException))
				e.printStackTrace();
			IOUtil.close(this);
		}
		
		notifyAll();
		
	}
	
	
	
	
	public synchronized void waitThenStopReading()
	{
		
		while(!isSelectable())
		{
			try 
			{
				wait(100);
				if(debug) log.info("after wait");
			} 
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (currentSK != null)
			getSelectorController().cancelSelectionKey(currentSK);
		else
			IOUtil.close(this);
		
	}



}