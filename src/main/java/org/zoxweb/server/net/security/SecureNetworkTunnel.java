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

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.NIOTunnel.NIOTunnelFactory;
import org.zoxweb.server.net.NetworkTunnel;
import org.zoxweb.server.security.CryptoUtil;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.net.IPAddress;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;

public class SecureNetworkTunnel
    implements Runnable, Closeable
{

	public static final LogWrapper log = new LogWrapper(SecureNetworkTunnel.class);
	/*
	 * json file format
	   {
			keystore_file: "file location",
			keystore_type: "jks",
			keystore_password: "passwd",
			alias_password: "passwd"
	   }
	 */
	public static class KeyStoreConfig
	{
		public String keystore_file;
		public String keystore_type;
		public String keystore_password;
		public String truststore_file;
		public String truststore_password;
		public String alias_password;
	}
	
	
	
	private ServerSocket ss;
	private IPAddress remoteSocketAddress;
	public SecureNetworkTunnel(SSLServerSocketFactory sslssf, int localPort, int backlog, IPAddress remoteAddress) throws IOException
	{
		ss = sslssf.createServerSocket(localPort, backlog);
		remoteSocketAddress = remoteAddress;
		new Thread(this).start();
	}
	
	
	//@SuppressWarnings("resource")
	@Override
	public void run() 
	{
		try
		{
			if (log.isEnabled()) log.getLogger().info("secure socket started " + ss.getLocalPort() + " " + remoteSocketAddress);
			while (!ss.isClosed())
			{
				Socket sIn = ss.accept();
				Socket sRemote = new Socket(remoteSocketAddress.getInetAddress(), remoteSocketAddress.getPort());
				//sIn.setSoTimeout(1000);
				//log.info(ss.getClass().getName() + " " +sIn.getClass().getName());
				new NetworkTunnel(sIn, sRemote);
			}
		}
		catch(Exception e)
		{
			IOUtil.close(ss);
		}
	}
	
	
	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		ss.close();
	}
	
	@SuppressWarnings("resource")
	public static void main(String ...args)
	{
		try
		{
			int index = 0;
			boolean nio = false;
			String ksConfig = args[index++];
			if (!new File(ksConfig).isFile())
			{
				ksConfig = args[index++];
				nio = true;
			}


			KeyStoreConfig ksc = GSONUtil.create(false).fromJson(IOUtil.inputStreamToString(new FileInputStream(ksConfig), true), KeyStoreConfig.class);
			
			
			//System.setProperty("javax.net.debug","all");
			SSLContext sslc = CryptoUtil.initSSLContext(ksc.keystore_file, ksc.keystore_type, ksc.keystore_password.toCharArray(),  
					ksc.alias_password != null ?  ksc.alias_password.toCharArray() : null, null, null);
			
	
			
			if (nio)
			{
				if (log.isEnabled()) log.info("Creating NIO Secure tunnel");
				
				NIOSocket nios = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
				for(; index < args.length; index++)
				{
					try
					{
						String[] parsed = args[index].split(",");
						int port = Integer.parseInt(parsed[0]);
						IPAddress remoteAddress = new IPAddress(parsed[1]);
						ServerSocketChannel ssc = ServerSocketChannel.open();
					
						ssc.bind(new InetSocketAddress(port));
						System.out.println("Adding:" + ssc + " " + remoteAddress);
						nios.addServerSocket(ssc, new NIOTunnelFactory(remoteAddress));
						//					
					}
					catch(Exception e)
					{
					
						e.printStackTrace();
					}
				}
			
			}
			else
			{
				if (log.isEnabled()) log.info("Creating classic Secure tunnel");
				SSLServerSocketFactory sslssf = sslc.getServerSocketFactory();
				for(; index < args.length; index++)
				{
					try
					{
						String[] parsed = args[index].split(",");
						int port = Integer.parseInt(parsed[0]);
						IPAddress remoteAddress = new IPAddress(parsed[1]);
						new SecureNetworkTunnel(sslssf, port, 128, remoteAddress);
						
					}
					catch(Exception e)
					{
						if (log.isEnabled()) log.info("Failed to start " + args[index]);
						e.printStackTrace();
					}
				}
			}
				
			

		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.err.println("SecureNetworkTunnel keystoreJSONConfigFile localport,remoteHost:remotePort...");
			System.exit(0);
		}
	}


	
	
}
