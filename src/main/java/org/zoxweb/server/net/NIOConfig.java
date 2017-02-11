package org.zoxweb.server.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.zoxweb.server.crypto.CryptoUtil;
import org.zoxweb.server.http.proxy.NIOProxyProtocol;
import org.zoxweb.server.http.proxy.NIOProxyProtocol.NIOProxyProtocolFactory;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LoggerUtil;
import org.zoxweb.server.net.NIOTunnel.NIOTunnelFactory;
import org.zoxweb.server.net.security.SSLSessionDataFactory;
import org.zoxweb.server.net.security.SecureNetworkTunnel;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.data.ConfigDAO;
import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.util.ArrayValues;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.SharedUtil;

/**
 * NIO config starts multi 
 * @author mnael
 *
 */
public class NIOConfig 
{
	

	
	
	private ConfigDAO configDAO;
	private static final transient Logger log = Logger.getLogger(NIOConfig.class.getName());

	
	
	public NIOConfig(String configDAOFile) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException
	{
		this((ConfigDAO)GSONUtil.fromJSON(IOUtil.inputStreamToString(configDAOFile)));
	}
	
	public NIOConfig(ConfigDAO configDAO)
	{
		this.configDAO = parse(configDAO);
	}
	
	
	
	public NIOSocket create() throws IOException
	{
		NIOSocket ret = new NIOSocket(TaskUtil.getDefaultTaskProcessor());
		
		for (NVEntity nve : configDAO.getContent().values())
		{
			// create the SSLEngine first
			// and attachments
			if (nve instanceof ConfigDAO)
			{
				ConfigDAO config = (ConfigDAO)nve;
				
				if (config.attachement() instanceof ProtocolSessionFactory)
				{
					ProtocolSessionFactory<?> psf = (ProtocolSessionFactory<?>) config.attachement();
					
					if (psf.getIncomingSSLSessionDataFactory() != null && psf instanceof NIOTunnelFactory)
					{
						log.info("Creating secure network tunnel:" + config.getProperties().get("port") + "," + ((NIOTunnelFactory)psf).getRemoteAddress() );
						// secure temporary fix since the NIO Secure Socket still not fully operational
						new SecureNetworkTunnel(psf.getIncomingSSLSessionDataFactory().getSSLContext().getServerSocketFactory(), 
											    Integer.parseInt(SharedUtil.lookupValue(config.getProperties().get("port"))), 
											    ((NIOTunnelFactory)psf).getRemoteAddress());
					}
					else
					{
						ServerSocketChannel ssc = ServerSocketChannel.open();
					
						ssc.bind(new InetSocketAddress(Integer.parseInt(SharedUtil.lookupValue(config.getProperties().get("port")))));
	
						ret.addServerSocket(ssc, psf);
					}
				}
			}
		}
		
		
		return ret;
	}
	
	
	public static ConfigDAO parse(ConfigDAO configDAO)
	{
		ArrayValues<NVEntity> content = configDAO.getContent();
		
		
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
						Object toAttach = Class.forName(config.getBeanClassName()).newInstance();
						
						if (toAttach instanceof SSLSessionDataFactory)
						{
							SSLContext sslc = CryptoUtil.initSSLContext(SharedUtil.getValue(config.getProperties().get("keystore_file")), 
																		SharedUtil.getValue(config.getProperties().get("keystore_type")), 
																		SharedUtil.getValue(config.getProperties().get("keystore_password")).toCharArray(),  
																		SharedUtil.getValue(config.getProperties().get("alias_password")) != null ?  SharedUtil.getValue(config.getProperties().get("alias_password")).toCharArray() : null, 
																		SharedUtil.getValue(config.getProperties().get("trustore_file")), 
																		SharedUtil.getValue(config.getProperties().get("trustore_password")) != null ?  SharedUtil.getValue(config.getProperties().get("trustore_password")).toCharArray() : null);
							((SSLSessionDataFactory)toAttach).setSSLContext(sslc);
							
							((SSLSessionDataFactory)toAttach).setExecutor(TaskUtil.getDefaultTaskProcessor());
						}
							
							
						config.attach(toAttach);
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
				if (config.attachement() instanceof NIOTunnelFactory)
				{
					NIOTunnelFactory nioTF = (NIOTunnelFactory) config.attachement();
					try 
					{
						String remote_host = SharedUtil.getValue(config.getProperties().get("remote_host"));
						String ssl_engine = SharedUtil.getValue(config.getProperties().get("ssl_engine"));
						nioTF.setRemoteAddress(new InetSocketAddressDAO(remote_host));
						if (ssl_engine != null)
						{
							ConfigDAO factory = (ConfigDAO)configDAO.getContent().get(ssl_engine);
							if (factory != null)
								nioTF.setIncomingSSLSessionDataFactory((SSLSessionDataFactory) factory.attachement());
						}
						
						
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
				else if (config.attachement() instanceof NIOProxyProtocolFactory)
				{
					NIOProxyProtocolFactory nioPPF = (NIOProxyProtocolFactory) config.attachement();
					try 
					{
						InetFilterRulesManager incomingIFRM =  new InetFilterRulesManager();
						InetFilterRulesManager outgoingIFRM =  new InetFilterRulesManager();
						for (GetNameValue<String> gnv : config.getProperties().values())
						{
							if(gnv.getName().equals("incoming_inet_rule"))
							{
								if (gnv.getValue() != null)
								{
									incomingIFRM.addInetFilterProp(gnv.getValue());
								}
							}
							
							if(gnv.getName().equals("outgoing_inet_rule"))
							{
								if (gnv.getValue() != null)
								{
									outgoingIFRM.addInetFilterProp(gnv.getValue());
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
						
						
						nioPPF.setLogger(LoggerUtil.loggerToFile(NIOProxyProtocol.class.getName()+".proxy", SharedUtil.lookupValue(config.getProperties().get("log_file"))));
						
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
	
	
	
	
	
	
	
	public static void main(String ...args)
	{
		try
		{
			System.out.println("loading file " + args[0]);
			 ConfigDAO configDAO =GSONUtil.fromJSON(IOUtil.inputStreamToString(args[0]));
			NIOConfig nioConfig = new NIOConfig(configDAO);
			nioConfig.create();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}