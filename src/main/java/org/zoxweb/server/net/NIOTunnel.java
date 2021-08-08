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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;

import org.zoxweb.server.net.security.SSLSessionData;
import org.zoxweb.server.net.security.SSLSessionDataFactory;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.net.InetSocketAddressDAO;


public class NIOTunnel
    extends ProtocolSessionProcessor
{
    private static final transient Logger log = Logger.getLogger(NIOTunnel.class.getName());

	private static boolean debug = false;
	
	
	public static class NIOTunnelFactory
	    extends ProtocolSessionFactoryBase<NIOTunnel>
	{
		
		private InetSocketAddressDAO remoteAddress;
		
		public NIOTunnelFactory()
		{
			
		}
		
		public NIOTunnelFactory(InetSocketAddressDAO remoteAddress, SSLContext sslContext)
		{
			this(remoteAddress, new SSLSessionDataFactory(sslContext, null));	
		}
		
		
		public NIOTunnelFactory(InetSocketAddressDAO remoteAddress, SSLSessionDataFactory sslUtil)
		{
			this.remoteAddress = remoteAddress;
			this.incomingSSLSessionFactory = sslUtil;	
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
			return "TunnelFactory";
		}


//		@Override
//		public boolean isBlocking() {
//			return false;
//		}
//
//		@Override
//		public SSLUtil getSSLUtil() {
//			return sslUtil;
//		}
		
	}
	

	private SocketChannel remoteChannel = null;
	
	private SocketChannel clientChannel = null;
	private SelectionKey  clientChannelSK = null;
	
	private ChannelRelayTunnel relay = null;
	
	final private InetSocketAddressDAO remoteAddress;

	public NIOTunnel(InetSocketAddressDAO remoteAddress)
	{
		this.remoteAddress = remoteAddress;
		bBuffer = ByteBuffer.allocate(getReadBufferSize());
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
		getSelectorController().cancelSelectionKey(clientChannelSK);
		IOUtil.close(remoteChannel);
		IOUtil.close(clientChannel);
		postOp();
		log.info("closed:" + remoteAddress);
	}

	@Override
	public void accept(SelectionKey key)
	{
		try
    	{
			SSLSessionData sslSessionData = ((ProtocolSessionProcessor)key.attachment()).getInputSSLSessionData();
			if(clientChannel == null)
			{
				clientChannel = (SocketChannel)key.channel();
				clientChannelSK = key;
				
				remoteChannel = SocketChannel.open((new InetSocketAddress(remoteAddress.getInetAddress(), remoteAddress.getPort())));
				relay = new ChannelRelayTunnel(getReadBufferSize(), remoteChannel, clientChannel, clientChannelSK,  true,  getSelectorController());
				relay.setOutputSSLSessionData(sslSessionData);
				getSelectorController().register(NIOChannelCleaner.DEFAULT, remoteChannel, SelectionKey.OP_READ, relay, false);
			}

			int read = 0 ;
    		do
            {
    			if (sslSessionData != null)
    			{
    				// ssl mode
    				read = sslSessionData.read(((SocketChannel)key.channel()), bBuffer, true);
    			}
    			else
    			{
    				bBuffer.clear();
    				read = ((SocketChannel)key.channel()).read(bBuffer);
    			}
    			if (read > 0)
    			{
    				ByteBufferUtil.write(remoteChannel, bBuffer);
    			}
    		}
    		while(read > 0);
    		
    		if (read == -1)
    		{
    			if (debug) log.info("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+Read:" + read);
    			
    			getSelectorController().cancelSelectionKey(key);
    			IOUtil.close(relay);	
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
		finally
		{
			//setSeletectable(true);
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
			
			
			new NIOSocket(new InetSocketAddress(port), 128, new NIOTunnelFactory(remoteAddress, (SSLSessionDataFactory)null), TaskUtil.getDefaultTaskProcessor());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			TaskUtil.getDefaultTaskScheduler().close();
			TaskUtil.getDefaultTaskProcessor().close();
		}
	}

}