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

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.ByteBufferUtil.BufferType;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;

import java.io.Closeable;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;


public class  ChannelRelayTunnel
	extends ProtocolHandler
{


	private static final LogWrapper log = new LogWrapper(ChannelRelayTunnel.class).setEnabled(false);

	private ByteChannel readSource;
	private ByteChannel writeDestination;
	private SelectionKey currentSK = null;
	private SelectionKey writeChannelSK;
	private final boolean autoCloseDestination;
	private Closeable closeInterface = null;

	private ByteBuffer sBuffer;
	
	
	
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
		sBuffer = ByteBufferUtil.allocateByteBuffer(BufferType.DIRECT, bufferSize);
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

	public void close() throws IOException 
	{
		if(!isClosed.getAndSet(true))
		{
			// TODO Auto-generated method stub
			if (closeInterface != null) {
				IOUtil.close(closeInterface);
			} else {
				IOUtil.close(readSource);
				getSelectorController().cancelSelectionKey(currentSK);

				if (autoCloseDestination) {
					IOUtil.close(writeDestination);
				}
				ByteBufferUtil.cache(sBuffer);
			}
		}
	}
	
	
	public synchronized void accept(SelectionKey key)
	{
		
		try
		{
			
			if (key != null)
				currentSK = key;
			int read;
			do
			{
				((Buffer)sBuffer).clear();
				read = ((SocketChannel)currentSK.channel()).read(sBuffer);
				
//				SSLSessionData outputSSLSessionData = getOutputSSLSessionData();
				if (read > 0)
				{
					ByteBufferUtil.write(writeDestination, sBuffer);
					if(log.isEnabled()) log.getLogger().info(ByteBufferUtil.toString(sBuffer));
				}
			}while(read > 0);
			
	    	if(read < 0) //end of stream we have to close 
	    		IOUtil.close(this);
	    	
		}
		catch(Exception e)
		{
			if(log.isEnabled()) log.getLogger().info("error:" +e);
			if (!(e instanceof IOException))
				e.printStackTrace();
			IOUtil.close(this);
		}
		
		notifyAll();
		
	}
	
	
	
	
	public synchronized void waitThenStopReading(SelectionKey sk)
	{
		SKAttachment ska = (SKAttachment) sk.attachment();
		while(!ska.isSelectable() && sk.channel().isOpen())
		{
			try 
			{
				wait(100);
				if(log.isEnabled()) log.getLogger().info("after wait");
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