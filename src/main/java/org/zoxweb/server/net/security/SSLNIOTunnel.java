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
package org.zoxweb.server.net.security;

import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.net.*;
import org.zoxweb.server.security.CryptoUtil;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.data.ConfigDAO;
import org.zoxweb.shared.net.InetSocketAddressDAO;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.logging.Logger;


public class SSLNIOTunnel
    extends ProtocolProcessor
{
    private static final transient Logger log = Logger.getLogger(SSLNIOTunnel.class.getName());

	private static boolean debug = true;


	public static class SSLNIOTunnelFactory
	    extends ProtocolSessionFactoryBase<SSLNIOTunnel>
	{

		private InetSocketAddressDAO remoteAddress;
		private SSLContext sslContext;

		public SSLNIOTunnelFactory()
		{

		}


		public SSLNIOTunnelFactory(SSLContext sslContext, InetSocketAddressDAO remoteAddress)
		{
			this.remoteAddress = remoteAddress;
			this.sslContext = sslContext;
		}

		public SSLContext getSSLContext()
		{
			return sslContext;
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
		public SSLNIOTunnel newInstance()
		{
			return new SSLNIOTunnel(sslContext, remoteAddress);
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return "NIOTunnelFactory";
		}

		public void init()
		{
			setRemoteAddress(new InetSocketAddressDAO(getProperties().getValue("remote_host")));
			sslContext = (SSLContext) ((ConfigDAO)getProperties().getValue("ssl_engine")).attachment();
		}

	}


	private SocketChannel destinationChannel = null;
	private SocketChannel sourceChannel = null;
	private ByteBuffer dBuffer = null;
	private SSLEngine sslEngine = null;
	private NIOSSLServer niosslServer = null;
	//private SelectionKey  clientChannelSK = null;
	//private ChannelRelayTunnel relay = null;
	private SSLContext sslContext;

	final private InetSocketAddressDAO remoteAddress;

	public SSLNIOTunnel(SSLContext sslContext, InetSocketAddressDAO remoteAddress)
	{
		this.remoteAddress = remoteAddress;
		this.sslContext = sslContext;

	}
	
	@Override
	public String getName()
	{
		return "SSLNIOTunnel";
	}

	@Override
	public String getDescription() 
	{
		return "SSLNIO Tunnel";
	}

	@Override
	public void close() throws IOException
    {
		//getSelectorController().cancelSelectionKey(clientChannelSK);
		IOUtil.close(destinationChannel);
		IOUtil.close(sourceChannel);

		//postOp();
		log.info("closed:" + remoteAddress);
	}




	@Override
	public  void accept(SelectionKey key)
	{
		try
    	{
			// first call
			if(sourceChannel == null)
			{
				synchronized (this) {
					if(sourceChannel == null) {
						log.info("First call for "  + key);
						sourceChannel = (SocketChannel) key.channel();
						destinationChannel = SocketChannel.open((new InetSocketAddress(remoteAddress.getInetAddress(), remoteAddress.getPort())));
						getSelectorController().register(NIOChannelCleaner.DEFAULT, destinationChannel, SelectionKey.OP_READ, this, false);

					}
				}
			}


			int read = 0 ;
			if(key.channel() == sourceChannel)
			{
				// reading encrypted data
				if (debug) log.info("incoming data on secure channel " + key);
				ByteBuffer temp = niosslServer.read((SocketChannel) key.channel(), sslEngine, sBuffer);
				if(temp == null) {
					close();
					return;
				}


				ByteBufferUtil.write(destinationChannel, temp);

				log.info("decrypted buffer : " + temp);
			}
			else if (key.channel() == destinationChannel)
			{
				if (debug) log.info("incoming data from remote channel " + key);

				do
				{

					dBuffer.clear();

					// modify if currentSourceChannel == sourceChannel
					read = ((SocketChannel) key.channel()).read(dBuffer);
					if (debug) log.info("byte read: " + read);
					if (read > 0)
					{
						// modify currentDestinationChannel == sourceChannel
						niosslServer.write(sourceChannel, sslEngine, dBuffer);
					}
				}
				while(read > 0);
			}




    		
    		if (read == -1)
    		{
    			if (debug) log.info("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+Read:" + read);
    			
    			getSelectorController().cancelSelectionKey(key);
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


	protected void acceptConnection(NIOChannelCleaner ncc, AbstractSelectableChannel asc, boolean isBlocking) throws IOException {
		// must be modified do the handshake



		sslEngine = sslContext.createSSLEngine();
		sslEngine.setUseClientMode(false);
		sslEngine.beginHandshake();

		niosslServer = new NIOSSLServer(TaskUtil.getDefaultTaskProcessor(), sslContext);
		if (niosslServer.doHandshake((SocketChannel) asc, sslEngine)) {
			sBuffer = ByteBuffer.allocate(2*niosslServer.appDataBufferSize());
			dBuffer = ByteBuffer.allocate(2*niosslServer.appDataBufferSize());
			getSelectorController().register(ncc,  asc, SelectionKey.OP_READ, this, isBlocking);
		} else {
			asc.close();
			log.info("Connection closed due to handshake failure.");
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
			String keystore = args[index++];
			String ksType = args[index++];
			String ksPassword = args[index++];
			TaskUtil.setThreadMultiplier(4);
			SSLContext sslContext = CryptoUtil.initSSLContext(IOUtil.locateFile(keystore), ksType, ksPassword.toCharArray(), null, null ,null);

			new NIOSocket(new InetSocketAddress(port), 128, new SSLNIOTunnelFactory(sslContext, remoteAddress), TaskUtil.getDefaultTaskProcessor());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			TaskUtil.getDefaultTaskScheduler().close();
			TaskUtil.getDefaultTaskProcessor().close();
		}
	}

}