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
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.task.TaskUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;


public class NIOSocketHandler
    extends ProtocolHandler
{
    private static final LogWrapper log = new LogWrapper(NIOSocketHandler.class).setEnabled(false);
	private volatile ByteBuffer phBB = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.DIRECT, 1024);

	private PlainSessionCallback sessionCallback;

	public NIOSocketHandler(PlainSessionCallback psc)
	{
		this.sessionCallback = psc;
	}
	
	@Override
	public String getName()
	{
		return "NIOTunnel";
	}

	@Override
	public String getDescription() 
	{
		return "NIO Tunnel";
	}

	@Override
	public void close() throws IOException
    {
		if(!isClosed.getAndSet(true))
		{
			IOUtil.close(phSChannel);
			IOUtil.close(sessionCallback.get());
			ByteBufferUtil.cache(phBB);
		}
	}


	@Override
	public void accept(SelectionKey key)
	{
		try
    	{


			if(sessionCallback.getConfig() == null)
			{
				sessionCallback.setConfig(phSChannel);
			}

			int read;
    		do
            {
				((Buffer) phBB).clear();
				read = phSChannel.isConnected()? phSChannel.read(phBB) : -1;

    			if (read > 0)
    			{
					sessionCallback.accept(phBB);
    			}
    		}
    		while(read > 0);
    		
    		if (read == -1)
    		{
    			if (log.isEnabled()) log.getLogger().info("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+Read:" + read);

    			close();

				if (log.isEnabled()) log.getLogger().info(key + ":" + key.isValid()+ " " + Thread.currentThread() + " " + TaskUtil.getDefaultTaskProcessor().availableExecutorThreads());
    		}
    	}
    	catch(Exception e)
    	{
    		if (log.isEnabled()) e.printStackTrace();
    		IOUtil.close(this);
			if (log.isEnabled()) log.getLogger().info(System.currentTimeMillis() + ":Connection end " + key + ":" + key.isValid()+ " " + Thread.currentThread() + " " + TaskUtil.getDefaultTaskProcessor().availableExecutorThreads());
    		
    	}
	}

	public void setupConnection(AbstractSelectableChannel asc, boolean isBlocking) throws IOException
	{
		phSChannel = (SocketChannel) asc;
		getSelectorController().register(phSChannel, SelectionKey.OP_READ, this, isBlocking);
		sessionCallback.setProtocolHandler(this);

	}

	@Override
	public void setSessionCallback(BaseSessionCallback<?> sessionCallback) {
		if(!(sessionCallback instanceof PlainSessionCallback))
		{
			throw new IllegalArgumentException("sessionCallback is not instance of PlainSessionCallback");
		}

		this.sessionCallback = (PlainSessionCallback) sessionCallback;
		if(this.sessionCallback.getConfig() == null)
		{
			this.sessionCallback.setConfig(phSChannel);
			sessionCallback.setProtocolHandler(this);
		}
	}


	@SuppressWarnings("resource")
    public static void main(String... args)
    {
		try
		{
			int index = 0;
			int port = Integer.parseInt(args[index++]);
			Class<? extends PlainSessionCallback> clazz = (Class<? extends PlainSessionCallback>) Class.forName(args[index++]);
			TaskUtil.setThreadMultiplier(8);
			
			
			new NIOSocket(new InetSocketAddress(port), 128, new NIOSocketHandlerFactory(clazz), TaskUtil.getDefaultTaskProcessor());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			TaskUtil.getDefaultTaskScheduler().close();
			TaskUtil.getDefaultTaskProcessor().close();
		}
	}


}