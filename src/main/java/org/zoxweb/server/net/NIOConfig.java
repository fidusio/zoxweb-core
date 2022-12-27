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

import org.zoxweb.server.http.proxy.NIOProxyProtocol.NIOProxyProtocolFactory;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.NIOTunnel.NIOTunnelFactory;
import org.zoxweb.server.net.security.IPBlockerListener;
import org.zoxweb.server.net.security.SecureNetworkTunnel;
import org.zoxweb.server.security.CryptoUtil;
import org.zoxweb.server.security.SSLContextInfo;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.app.AppCreatorDefault;
import org.zoxweb.shared.data.ConfigDAO;
import org.zoxweb.shared.security.IPBlockerConfig;
import org.zoxweb.shared.util.ArrayValues;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.NVStringList;

import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.security.Provider;
import java.util.HashSet;
import java.util.Set;

import static org.zoxweb.server.security.SSLContextInfo.Param.CIPHERS;
import static org.zoxweb.server.security.SSLContextInfo.Param.PROTOCOLS;


/**
 * NIO config starts multi 
 * @author mnael
 *
 */
public class NIOConfig
extends AppCreatorDefault<NIOSocket, ConfigDAO>
{
	public static final String RESOURCE_NAME = "NIOConfig";
	private NIOSocket nioSocket;
	

	public static final LogWrapper log = new LogWrapper(NIOConfig.class).setEnabled(false);
	private final Set<Closeable> services = new HashSet<>();



	
	public NIOConfig(String configDAOFile) throws  IOException
	{
		this(GSONUtil.fromJSON(IOUtil.inputStreamToString(configDAOFile)));
	}
	
	
	public NIOConfig(InputStream configDAOFile) throws IOException
	{
		this (GSONUtil.fromJSON(IOUtil.inputStreamToString(configDAOFile, true)));
	}
	
	public NIOConfig(ConfigDAO configDAO)
	{
		setAppConfig(configDAO);
	}

	public NIOConfig()
	{
	}
	public NIOConfig setNIOSocket(NIOSocket nios)
	{
		nioSocket = nios;
		return this;
	}

	public NIOSocket getNIOSocket(){return nioSocket;}

	public String getName()
	{
		return RESOURCE_NAME;
	}
	
	
	public synchronized NIOSocket createApp() throws IOException
	{
		if(nioSocket == null)
			nioSocket = new NIOSocket(TaskUtil.getDefaultTaskProcessor());
		services.add(nioSocket);
		
		for (NVEntity nve : getAppConfig().getContent().values())
		{
			// create the SSLEngine first
			// and attachments
			if (nve instanceof ConfigDAO)
			{
				ConfigDAO config = (ConfigDAO)nve;
				
				if (config.attachment() instanceof ProtocolFactory)
				{
					int port = ((ConfigDAO) nve).getProperties().getValue("port");
					int backlog = 128;
					try
					{
						backlog = ((ConfigDAO) nve).getProperties().getValue("backlog");
					}
					catch(Exception e)
					{
						
					}
					ProtocolFactory<?> psf = (ProtocolFactory<?>) config.attachment();
					
					if (psf instanceof NIOTunnelFactory && psf.getProperties().getValue("ssl_engine") != null)
					{

						ConfigDAO sslContent =psf.getProperties().getValue("ssl_engine");
						SSLContext sslContext = (SSLContext) sslContent.attachment();
						if(log.isEnabled()) log.getLogger().info("Creating secure network tunnel:" + port+ "," + ((NIOTunnelFactory)psf).getRemoteAddress() );
						// secure temporary fix since the NIO Secure Socket still not fully operational
						services.add(new SecureNetworkTunnel(sslContext.getServerSocketFactory(),
											    port, backlog,
											    ((NIOTunnelFactory)psf).getRemoteAddress()));
					}
					else
					{
						ServerSocketChannel ssc = ServerSocketChannel.open();
					
						ssc.bind(new InetSocketAddress(port), backlog);
	
						nioSocket.addServerSocket(ssc, psf);
					}
					
					if(log.isEnabled()) log.getLogger().info("Service added " + psf.getName() +" port:" + port + " backlog:" + backlog);
				}
			}
		}
		
		
		return nioSocket;
	}
	
	
	public static ConfigDAO parse(ConfigDAO configDAO)
	{
		ArrayValues<NVEntity> content = configDAO.getContent();
		
		if(log.isEnabled()) log.getLogger().info("Start Parsing");
		// first pass
		for (NVEntity nve : content.values())
		{
			// create the SSLEngine first
			// and attachments
			if (nve instanceof ConfigDAO)
			{
				ConfigDAO config = (ConfigDAO)nve;
				
				if (config.getBeanClassName() != null)
				{
					try 
					{
						Class<?> clazz =  Class.forName(config.getBeanClassName());
						if(log.isEnabled()) log.getLogger().info("Class: " + clazz);
						
						if (clazz.isAssignableFrom(SSLContext.class))
						{
							if(log.isEnabled()) log.getLogger().info("We have SSLContext");
							String ksPassword = config.getProperties().getValue("keystore_password");
							String aliasPassword = config.getProperties().getValue("alias_password");
							String trustStorePassword = config.getProperties().getValue("truststore_password");
							String trustFile = config.getProperties().getValue("truststore_file");
							String providerName = config.getProperties().getValue("provider");
							String protocol = config.getProperties().getValue("protocol");
							NVStringList protocols =((NVStringList)config.getProperties().get(PROTOCOLS));
							NVStringList ciphers =((NVStringList)config.getProperties().get(CIPHERS));

							Provider provider = null;
							if(providerName != null)
							{
								Class pClass = Class.forName(providerName);
								provider = (Provider) pClass.getDeclaredConstructor().newInstance();
							}
							SSLContext sslContext = CryptoUtil.initSSLContext(protocol, provider, IOUtil.locateFile(config.getProperties().getValue("keystore_file")),
																		config.getProperties().getValue("keystore_type"),
																		ksPassword.toCharArray(),  
																		aliasPassword != null ?  aliasPassword.toCharArray() : null,
																		trustFile != null ? IOUtil.locateFile(trustFile) : null,
																		trustStorePassword != null ?  trustStorePassword.toCharArray() : null);


							config.attach(new SSLContextInfo(sslContext,
									protocols != null ? protocols.getValues() :null,
									ciphers != null ? ciphers.getValues() : null));
						}
						else {
							Object toAttach = clazz.getDeclaredConstructor().newInstance();
							config.attach(toAttach);
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	
		
		
		for (NVEntity nve : content.values())
		{
			if (nve instanceof ConfigDAO)
			{
				ConfigDAO config = (ConfigDAO)nve;

				if (config.attachment() instanceof NIOProxyProtocolFactory)
				{
					NIOProxyProtocolFactory nioPPF = (NIOProxyProtocolFactory) config.attachment();
					try 
					{
						InetFilterRulesManager incomingIFRM =  new InetFilterRulesManager();
						InetFilterRulesManager outgoingIFRM =  new InetFilterRulesManager();
						NVStringList iRules = (NVStringList) config.getProperties().get("incoming_inet_rule");
						NVStringList oRules = (NVStringList) config.getProperties().get("outgoing_inet_rule");
						if (iRules != null)
						{
							for (String rule : iRules.getValue())
							{
								if(log.isEnabled()) log.getLogger().info("Adding Incoming rule:" + rule);
								try {
									incomingIFRM.addInetFilterProp(rule);
								}
								catch(Exception e)
								{
									e.printStackTrace();
									try {
										incomingIFRM.addInetFilterProp(rule);
									}
									catch(Exception e1)
									{
										e.printStackTrace();
									}
								}
								
							}
						}
						
						if (oRules != null)
						{
							for (String rule : oRules.getValue())
							{
								if(log.isEnabled()) log.getLogger().info("Adding Incoming rule:" + rule);
								try {
									outgoingIFRM.addInetFilterProp(rule);
								}
								catch(Exception e)
								{
									e.printStackTrace();
									try {
										outgoingIFRM.addInetFilterProp(rule);
									}
									catch(Exception e1)
									{
										e.printStackTrace();
									}
								}
								
							}
						}
						
						if (incomingIFRM.getAll().size() > 0)
						{
							nioPPF.setIncomingInetFilterRulesManager(incomingIFRM);
						}
						
						if (outgoingIFRM.getAll().size() > 0)
						{
							nioPPF.setOutgoingInetFilterRulesManager(outgoingIFRM);
						}

//						if(!SharedStringUtil.isEmpty(config.getProperties().getValue("log_file")))
//							nioPPF.setLogger(LoggerUtil.loggerToFile(NIOProxyProtocol.class.getName()+".proxy", config.getProperties().getValue("log_file")));
						
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
				else if (config.attachment() instanceof ProtocolFactoryBase)
				{
					ProtocolFactoryBase nioTF = (ProtocolFactoryBase) config.attachment();
					try
					{
						String ssl_engine = config.getProperties().getValue("ssl_engine");
						NVPair rh = (NVPair) config.getProperties().get("remote_host");
						if(log.isEnabled()) log.getLogger().info("ssl_engine_found: " + ssl_engine);
						if(rh != null)
							nioTF.getProperties().add(rh);
						if(ssl_engine != null)
						{
							ConfigDAO sslContent = (ConfigDAO)configDAO.getContent().get(ssl_engine);
							if(log.isEnabled()) log.getLogger().info("sslContent: " + sslContent);
							if(sslContent != null) {
								if(log.isEnabled()) log.getLogger().info("" + sslContent);
								nioTF.getProperties().add("ssl_engine", sslContent);
							}
						}


//						if(!SharedStringUtil.isEmpty(config.getProperties().getValue("log_file")))
//							nioTF.setLogger(LoggerUtil.loggerToFile(NIOTunnel.class.getName()+".proxy", config.getProperties().getValue("log_file")));


						nioTF.init();

					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		return configDAO;
	}
	
	


	@Override
	public void close() throws IOException 
	{
		// TODO Auto-generated method stub
		for (Closeable c : services)
		{
			IOUtil.close(c);
		}
	}


	@Override
	public NIOConfig setAppConfig(ConfigDAO appConfig)
	{
		// TODO Auto-generated method stub
		return (NIOConfig) super.setAppConfig(parse(appConfig));
	}



	public static void main(String ...args)
	{
		try
		{
			int index = 0;
			log.getLogger().info("loading file " + args[index]);
			ConfigDAO configDAO = GSONUtil.fromJSON(IOUtil.inputStreamToString(args[index++]));
			log.getLogger().info(GSONUtil.toJSON(configDAO, true, false, false));
			NIOConfig nioConfig = new NIOConfig(configDAO);
			NIOSocket nioSocket = nioConfig.createApp();
			if (args.length > index) {
				IPBlockerConfig ipBlockerConfig = GSONUtil.fromJSON(IOUtil.inputStreamToString(args[index++]), IPBlockerConfig.class);
				IPBlockerListener.Creator c = new IPBlockerListener.Creator();
				//if(log.isEnabled()) log.getLogger().info("\n" + GSONUtil.toJSON(appConfig, true, false, false));
				c.setAppConfig(ipBlockerConfig);
				c.createApp();
				nioSocket.setEventManager(TaskUtil.getDefaultEventManager());

			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}


	
}
