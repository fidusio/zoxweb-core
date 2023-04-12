/*
 * Copyright (c) 2017-2021 XlogistX.IO Inc.
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
package org.zoxweb.server.net.ssl;

import org.zoxweb.server.fsm.StateMachine;
import org.zoxweb.server.fsm.TriggerConsumer;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.logging.LoggerUtil;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.ProtocolHandler;
import org.zoxweb.server.security.CryptoUtil;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.util.ParamUtil;
import org.zoxweb.shared.util.SharedUtil;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;


public class SSLNIOSocket
    extends ProtocolHandler
{


//	private class PostHandshake extends TriggerConsumer<SSLSessionConfig>
//	{
//
//		private final SSLNIOSocket sslns;
//		PostHandshake(SSLNIOSocket sslns)
//		{
//			super(POST_HANDSHAKE);
//			this.sslns = sslns;
//		}
//		@Override
//		public void accept(SSLSessionConfig config)
//		{
//			if(config.remoteAddress != null && config.inRemoteData == null)
//			{
//				synchronized (config)
//				{
//					if(config.inRemoteData == null)
//					{
//						try
//						{
//							config.inRemoteData = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.DIRECT, ByteBufferUtil.DEFAULT_BUFFER_SIZE);
//							config.remoteChannel = SocketChannel.open((new InetSocketAddress(config.remoteAddress.getInetAddress(), config.remoteAddress.getPort())));
//							sslns.getSelectorController().register(config.remoteChannel, SelectionKey.OP_READ, sslns, false);
//						}
//						catch(Exception e)
//						{
//							if (log.isEnabled()) log.getLogger().info("" + e);
//							if (log.isEnabled()) log.getLogger().info("connect to " + config.remoteAddress + " FAILED");
//							config.close();
//						}
//					}
//				}
//			}
//		}
//	}


	private static class TunnelCallback extends SSLSessionCallback
	{

		@Override
		public void exception(Exception e) {
			// exception handling
			//e.printStackTrace();
			if (log.isEnabled()) log.getLogger().info(e + "");
		}

		@Override
		public void accept(ByteBuffer buffer) {
			// data handling
			if(buffer != null)
			{
				try
				{
					ByteBufferUtil.smartWrite(null, getConfig().remoteChannel, buffer);
				}
				catch(Exception e)
				{
					e.printStackTrace();
					if (log.isEnabled()) log.getLogger().info(e+"");
					// we should close
					IOUtil.close(get());
				}
			}
		}
	}




    public static final LogWrapper log = new LogWrapper(SSLNIOSocket.class).setEnabled(false);

	private SSLConnectionHelper sslDispatcher = null;
	private SSLSessionConfig config = null;
	final public InetSocketAddressDAO remoteAddress;
	final private SSLContextInfo sslContext;
	private final SSLSessionCallback sessionCallback;
	///private StaticSSLStateMachine staticSSLStateMachine = null;

//	public SSLNIOSocket(SSLContextInfo sslContext, InetSocketAddressDAO ra)
//	{
//
//		this(sslContext, ra, new TunnelCallback());
//	}

	public SSLNIOSocket(SSLContextInfo sslContext, SSLSessionCallback sessionCallback)
	{
		this(sslContext, sessionCallback, null);
	}

	public SSLNIOSocket(SSLContextInfo sslContext, SSLSessionCallback sessionCallback, InetSocketAddressDAO ra)
	{
		SharedUtil.checkIfNulls("context  can't be null", sslContext);
		this.sslContext = sslContext;
		remoteAddress = ra;
		if(remoteAddress != null && sessionCallback == null)
		{
			//this.sessionCallback = new TunnelCallback();
			this.sessionCallback = new TunnelCallback();
		}
		else
		{
			SharedUtil.checkIfNulls("SSL session call can't be null", sessionCallback);
			this.sessionCallback = sessionCallback;
		}

		SharedUtil.checkIfNulls("Session callback can't be null", this.sessionCallback);
	}
	
	@Override
	public String getName()
	{
		return "SSLNIOSocket";
	}

	@Override
	public String getDescription() 
	{
		return "SSL NIO Server Socket";
	}

	@Override
	public void close()
    {
		if(config != null)
			config.close();

	}


	SSLContextInfo getSSLContextInfo()
	{
		return sslContext;
	}

	@Override
	public  void accept(SelectionKey key)
	{
		if (log.isEnabled()) log.getLogger().info("Start of Accept SSLNIOSocket");
		try
    	{
			// begin handshake will be called once subsequent calls are ignored
			config.beginHandshake(false);

			if (log.isEnabled()) log.getLogger().info("AcceptNewData: " + key);
			if (key.channel() == config.sslChannel && config.sslChannel.isConnected())
			{
				sslDispatcher.dispatch(config.getHandshakeStatus(), sessionCallback);
			}
			else if (key.channel() == config.remoteChannel && config.remoteChannel.isConnected())
			{
				// this is the tunnel section
				int bytesRead;
				do {
					bytesRead = config.remoteChannel.isConnected() ? config.remoteChannel.read(config.inRemoteData) : -1;
					if (bytesRead > 0 ) config.sslOutputStream.write(config.inRemoteData);
				}while(bytesRead > 0);

				if (bytesRead == -1) {
					if (log.isEnabled())
						log.getLogger().info("SSL-CHANNEL-CLOSED-NEED_UNWRAP: " + config.getHandshakeStatus() + " bytesRead: " + bytesRead);
					close();
				}
			}
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    		close();
			if (log.isEnabled()) log.getLogger().info(System.currentTimeMillis() + ":Connection end " + key + ":" + key.isValid() + " " + TaskUtil.availableThreads(getExecutor()));
    	}
		if (log.isEnabled()) log.getLogger().info( "End of SSLNIOSocket-ACCEPT  available thread:" + TaskUtil.availableThreads(getExecutor()));
	}



	@Override
	public void setupConnection(AbstractSelectableChannel asc, boolean isBlocking) throws IOException {
		SSLStateMachine sslStateMachine = SSLStateMachine.create(this);
    	sslDispatcher = sslStateMachine;
		config = sslStateMachine.getConfig();
    	config.selectorController = getSelectorController();
		config.sslChannel = (SocketChannel) asc;
		config.remoteAddress = remoteAddress;
		config.sslOutputStream = new SSLChannelOutputStream(config, 512 );
		sessionCallback.setConfig(config);
		sslStateMachine.start(true);
		// not sure about
		//config.beginHandshake(false);
		getSelectorController().register(asc, SelectionKey.OP_READ, this, isBlocking);
	}



//	public void setupConnection(AbstractSelectableChannel asc, boolean isBlocking) throws IOException {
//		config = new SSLSessionConfig(sslContext);//sslStateMachine.getConfig();
//		config.selectorController = getSelectorController();
//		config.sslChannel = (SocketChannel) asc;
//		config.remoteAddress = remoteAddress;
//		config.sslOutputStream = new SSLChannelOutputStream(config, 512 );
//		sessionCallback.setConfig(config);
//		sslDispatcher = new CustomSSLStateMachine(this);
//		//sslStateMachine.start(true);
//		// not sure about
//		// start the handshake here
//		//config.beginHandshake(false);
//		getSelectorController().register(asc, SelectionKey.OP_READ, this, isBlocking);
//	}






	@SuppressWarnings("resource")
    public static void main(String... args)
    {
    	LoggerUtil.enableDefaultLogger("org.zoxweb");
		try
		{
			ParamUtil.ParamMap params = ParamUtil.parse("-", args);
			int port = params.intValue("-port");
			String keystore = params.stringValue("-keystore");
			String ksType = params.stringValue("-kstype");
			String ksPassword = params.stringValue("-kspassword");
			boolean dbg = params.nameExists("-dbg");
			String ra =  params.stringValue("-ra", true);
			InetSocketAddressDAO remoteAddress = ra != null ? new InetSocketAddressDAO(ra) : null;

			if(dbg)
			{
				StateMachine.log.setEnabled(true);
				TriggerConsumer.log.setEnabled(true);
				SSLSessionConfig.log.setEnabled(true);
			}




			//TaskUtil.setThreadMultiplier(4);
			SSLContext sslContext = CryptoUtil.initSSLContext(keystore, ksType, ksPassword.toCharArray(), null, null ,null);

			new NIOSocket(new InetSocketAddress(port), 512, new SSLNIOSocketFactory(new SSLContextInfo(sslContext), remoteAddress), TaskUtil.getDefaultTaskProcessor());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			TaskUtil.getDefaultTaskScheduler().close();
			TaskUtil.getDefaultTaskProcessor().close();
		}
	}


	public SSLSessionConfig getConfig()
	{
		return config;
	}


	void createRemoteConnection()
	{
		if(config.remoteAddress != null && config.inRemoteData == null)
		{
			synchronized (config)
			{
				if(config.inRemoteData == null)
				{
					try
					{

						config.inRemoteData = ByteBufferUtil.allocateByteBuffer(ByteBufferUtil.BufferType.DIRECT, ByteBufferUtil.DEFAULT_BUFFER_SIZE);
						config.remoteChannel = SocketChannel.open((new InetSocketAddress(config.remoteAddress.getInetAddress(), config.remoteAddress.getPort())));
						getSelectorController().register(config.remoteChannel, SelectionKey.OP_READ, this, false);
						//sessionCallback.setRemoteSocket(config.remoteChannel);

					}
					catch(Exception e)
					{
						e.printStackTrace();
						if (log.isEnabled()) log.getLogger().info("" + e);
						if (log.isEnabled()) log.getLogger().info("connect to " + config.remoteAddress + " FAILED");
						config.close();
					}
				}
			}
		}
	}

}