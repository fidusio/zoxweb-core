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
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.net.InetSocketAddressDAO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;


public class NIOTunnel
    extends ProtocolProcessor
{
    private static final Logger log = Logger.getLogger(NIOTunnel.class.getName());

	private static boolean debug = false;


	public static class NIOTunnelFactory
	    extends ProtocolSessionFactoryBase<NIOTunnel>
	{

		private InetSocketAddressDAO remoteAddress;

		public NIOTunnelFactory()
		{

		}


		public NIOTunnelFactory(InetSocketAddressDAO remoteAddress)
		{
			this.remoteAddress = remoteAddress;
		}

		public void setRemoteAddress(InetSocketAddressDAO rAddress)
		{
			remoteAddress = rAddress;
		}

		public InetSocketAddressDAO getRemoteAddress()
		{
			return remoteAddress;
		}

		@Override
		public NIOTunnel newInstance()
		{
			return new NIOTunnel(remoteAddress);
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return "NIOTunnelFactory";
		}
		public void init()
		{
			setRemoteAddress(new InetSocketAddressDAO(getProperties().getValue("remote_host")));
		}

		@Override
		public NIOChannelCleaner getNIOChannelCleaner() {
			return NIOChannelCleaner.DEFAULT;
		}

	}


	private volatile SocketChannel destinationChannel = null;
	private volatile SelectionKey  destinationSK = null;
	private volatile SocketChannel sourceChannel = null;
	private volatile SelectionKey  sourceSK = null;
	private volatile SocketAddress sourceAddress = null;
	private volatile ByteBuffer destinationBB;
	private volatile ByteBuffer sourceBB;


	final private InetSocketAddressDAO remoteAddress;

	public NIOTunnel(InetSocketAddressDAO remoteAddress)
	{
		this.remoteAddress = remoteAddress;
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
			IOUtil.close(destinationChannel);
			IOUtil.close(sourceChannel);
			ByteBufferUtil.cache(sourceBB, destinationBB);
			log.info("closed: " + sourceAddress + " - "   + remoteAddress);
		}
	}


	@Override
	public void accept(SelectionKey key)
	{
		try
    	{

			if(sourceChannel == null)
			{
				synchronized (this) {
					if(sourceChannel == null) {
						sourceChannel = (SocketChannel) key.channel();
						sourceAddress = sourceChannel.getRemoteAddress();
						sourceSK = key;
						destinationChannel = SocketChannel.open((new InetSocketAddress(remoteAddress.getInetAddress(), remoteAddress.getPort())));
						//relay = new ChannelRelayTunnel(getReadBufferSize(), destinationChannel, sourceChannel, sourceSK,  true,  getSelectorController());
						destinationBB = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.DIRECT);
						sourceBB = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.DIRECT);
						destinationSK = getSelectorController().register(NIOChannelCleaner.DEFAULT, destinationChannel, SelectionKey.OP_READ, this, new DefaultSKController(),false);

					}
				}

			}

			SocketChannel readChannel;
			SocketChannel writeChannel;
			ByteBuffer currentBB;

			if (key.channel() == sourceChannel)
			{
				readChannel = sourceChannel;
				writeChannel = destinationChannel;
				currentBB = sourceBB;
			}
			else
			{
				readChannel = destinationChannel;
				writeChannel = sourceChannel;
				currentBB = destinationBB;
			}
			int read = 0 ;
    		do
            {
    			{
					((Buffer)currentBB).clear();
    				read = readChannel.read(currentBB);
    			}
    			if (read > 0)
    			{
    				ByteBufferUtil.write(writeChannel, currentBB);
    			}
    		}
    		while(read > 0);
    		
    		if (read == -1)
    		{
    			if (debug) log.info("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+Read:" + read);

    			close();
    				
    			if (debug) log.info(key + ":" + key.isValid()+ " " + Thread.currentThread() + " " + TaskUtil.getDefaultTaskProcessor().availableExecutorThreads());		
    		}
    	}
    	catch(Exception e)
    	{
    		if (debug) e.printStackTrace();
    		IOUtil.close(this);
    		if (debug) log.info(System.currentTimeMillis() + ":Connection end " + key + ":" + key.isValid()+ " " + Thread.currentThread() + " " + TaskUtil.getDefaultTaskProcessor().availableExecutorThreads());
    		
    	}
	}
	
	@SuppressWarnings("resource")
    public static void main(String... args)
    {
		try
		{
			int index = 0;
			int port = Integer.parseInt(args[index++]);
			InetSocketAddressDAO remoteAddress = new InetSocketAddressDAO(args[index++]);
			TaskUtil.setThreadMultiplier(4);
			
			
			new NIOSocket(new InetSocketAddress(port), 128, new NIOTunnelFactory(remoteAddress), TaskUtil.getDefaultTaskProcessor());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			TaskUtil.getDefaultTaskScheduler().close();
			TaskUtil.getDefaultTaskProcessor().close();
		}
	}

}