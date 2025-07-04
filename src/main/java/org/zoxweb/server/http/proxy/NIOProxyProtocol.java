
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
package org.zoxweb.server.http.proxy;

import org.zoxweb.server.http.HTTPUtil;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.ByteBufferUtil.BufferType;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.net.*;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.protocol.Delimiter;
import org.zoxweb.shared.security.SecurityStatus;
import org.zoxweb.shared.util.NVBoolean;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.ResourceManager;
import org.zoxweb.shared.util.SharedStringUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NIOProxyProtocol 
	extends ProtocolHandler
{
	public final static String NIO_PROXY = ResourceManager.SINGLETON.register(ResourceManager.Resource.PROXY_SERVER, "NIOHTTPProxy")
			.lookupResource(ResourceManager.Resource.PROXY_SERVER);

	public final static LogWrapper log = new LogWrapper(NIOProxyProtocol.class).setEnabled(false);


	
	public static final String AUTHENTICATION = "AUTHENTICATION";



	private static class RequestInfo
	{
		final HTTPMessageConfigInterface hmci;
		int payloadIndex = -1;
		int payloadSent = 0;
		final IPAddress remoteAddress;
		boolean headerNotSent = true;
		
		RequestInfo(HTTPMessageConfigInterface hmci, UByteArrayOutputStream ubaos)
		{
			this.hmci = hmci;
			remoteAddress = HTTPUtil.parseHost(hmci.getURI());
			//if (hmci.getContentLength() > 0)
			{
				payloadIndex = ubaos.indexOf(Delimiter.CRLFCRLF.getBytes());
				if (payloadIndex > 0)
				{
					payloadIndex += Delimiter.CRLFCRLF.getBytes().length;
				}
				else
				{
					throw new IllegalArgumentException("invalid message");
				}
				
			}
		}

		public void writeHeader(SocketChannel remoteChannel, UByteArrayOutputStream requestRawBuffer)
			throws IOException
		{
			if (headerNotSent)
			{
				ByteBufferUtil.write(remoteChannel, HTTPUtil.formatRequest(hmci, true, null, HTTPHeader.PROXY_CONNECTION.getName()));
				headerNotSent = false;
				writePayload(remoteChannel, requestRawBuffer, payloadIndex);
				if (hmci.getContentLength()  < 0)
    			{
    				
    				requestRawBuffer.reset();
    			}
			}
		}
		
		public void writePayload(SocketChannel remoteChannel, UByteArrayOutputStream requestRawBuffer, int fromIndex) throws IOException
		{
			if (hmci.getContentLength() > 1 && !isRequestComplete())
			{
				int dataLength = requestRawBuffer.size() - fromIndex;
				if ((dataLength + payloadSent) > hmci.getContentLength())
				{
					dataLength = hmci.getContentLength() - payloadSent - fromIndex;
				}
				
				ByteBufferUtil.write(remoteChannel, requestRawBuffer.getInternalBuffer(), fromIndex, dataLength);
				payloadSent += dataLength;
				requestRawBuffer.shiftLeft(fromIndex+dataLength, 0);
			}
		}
		
		
		public boolean isRequestComplete()
		{
			return payloadSent == hmci.getContentLength();
		}
		
		
		public HTTPMessageConfigInterface getHTTPMessageConfigInterface()
		{
			return hmci;
		}
		
		
	}
	
	
	public static final class NIOProxyProtocolFactory
		extends ProtocolFactoryBase<NIOProxyProtocol>
	{

		public NIOProxyProtocolFactory()
		{
			getProperties().add(new NVBoolean(NIOProxyProtocol.AUTHENTICATION, false));
		}
		
		@Override
		public NIOProxyProtocol newInstance()
		{
			NIOProxyProtocol ret = new NIOProxyProtocol();
			ret.setOutgoingInetFilterRulesManager(getOutgoingInetFilterRulesManager());
			ret.setProperties(getProperties());
			
			return ret;
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return "NIOProxyFactory";
		}

		public void init(){}

//		@Override
//		public NIOChannelCleaner getNIOChannelCleaner() {
//			return NIOChannelCleaner.DEFAULT;
//		}
		
		
	}
	
	private UByteArrayOutputStream requestBuffer = new UByteArrayOutputStream();
	private HTTPMessageConfigInterface requestMCCI = null;
	private IPAddress lastRemoteAddress = null;
	private SocketChannel remoteChannel = null;
	private SelectionKey  remoteChannelSK = null;
	//private SocketChannel phSChannel = null;
	//private SelectionKey  clientChannelSK = null;
	private ChannelRelayTunnel channelRelay = null;
	private RequestInfo requestInfo = null;
	private final ByteBuffer sourceBB;

	private boolean relayConnection = false;

	private NIOProxyProtocol()
	{
		super(true);
		sourceBB =  ByteBufferUtil.allocateByteBuffer(BufferType.DIRECT);
	}
	
	
	@Override
	public String getName()
	{
		return NIO_PROXY;
	}

	@Override
	public String getDescription() 
	{
		return "Experimental http proxy";
	}

	@Override
	protected void close_internal() throws IOException
	{

		IOUtil.close(phSChannel);

		if (channelRelay != null) {
			IOUtil.close(channelRelay);
		} else {
			IOUtil.close(remoteChannel);
		}
		ByteBufferUtil.cache(sourceBB);

	}



	@Override
	public void accept(SelectionKey key)
	{
		try
    	{
			if (phSK == null)
			{
				phSK = key;
			}
			
			
			int read;
    		do
    		{
				((Buffer)sourceBB).clear();
    			
    			read = ((SocketChannel)key.channel()).read(sourceBB);
    			if (read > 0)
    			{
    				if (relayConnection)
    				{
    					ByteBufferUtil.write(remoteChannel, sourceBB);
    					//log.info(ByteBufferUtil.toString(bBuffer));
    				}
    				else
    				{
    					ByteBufferUtil.write(sourceBB, requestBuffer, true);
    					//log.info(new String(requestBuffer.getInternalBuffer(), 0, requestBuffer.size()));
    					tryToConnectRemote(requestBuffer, read);
    				}	
    			}

    		}
    		while(read > 0);


    		if (read == -1)
    		{
    			if(log.isEnabled())
    				log.info("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+Read:" + read);
    			
    			
    			
    			close();
//    			if (remoteChannel == null || !remoteChannel.isOpen())
//    			{
//    				if(debug)
//    					log.info("Will close the whole connection since remote it is NOT OPEN:" + key + " remote:" +remoteChannel);
//    				
//    				//
//    				getSelectorController().cancelSelectionKey(key);
//    			}
//    			else
//    			{
//    				getSelectorController().cancelSelectionKey(key);
//    				
//
//    				if (debug)
//    					log.info(key + ":" + key.isValid()+ " " + Thread.currentThread() + " " + TaskUtil.getDefaultTaskProcessor().availableExecutorThreads());
//    					
//    			}
    		}
			
    	}
    	catch(Exception e)
    	{
    		if(log.isEnabled())
    			e.printStackTrace();
    		IOUtil.close(this);
    		
    	}
	}

	

	
	
	public static  HTTPMessageConfigInterface createErrorMSG(int a, String errorMsg, String url)
	{
		HTTPMessageConfigInterface hcc = createHeader(null, a);
		
		hcc.getHeaders().add(new NVPair("Content-Type","text/html; charset=iso-8859-1"));
		String msg = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\r"
		+ "<HTML><HEAD>\r"
		+ "<TITLE>" + hcc.getHTTPStatusCode().CODE + "</TITLE>\r"
		+ "</HEAD>\r"  // use css style sheet in htdocs
		+ "<BODY BGCOLOR=\"#FFFFFF\" TEXT=\"#000000\" LINK=\"#000080\" VLINK=\"#000080\" ALINK=\"#000080\">\r"
		+ "<h2 class=\"headline\">HTTP " + hcc.getHTTPStatusCode().CODE + " </h2>\r"
		+ "<HR size=\"4\">\r"
		+ "<p class=\"i30\">Your request for the following URL failed:</p>"
		+ "<p class=\"tiagtext\"><a href=\"" + url + "\">" + url + "</A> </p>\r"
		+ "<P class=\"i25\">Reason: " + errorMsg + "</P>"
		+ "<HR size=\"4\">\r"
		+ "<p class=\"i25\"><A HREF=\"http://www.zoxweb.com/\">ProxyNIO</A> HTTP Proxy, Version " + NIO_PROXY
		+"</p>\r"
		+ "</BODY></HTML>";
		hcc.setContent(SharedStringUtil.getBytes(msg));
		
		return hcc;
		
		
	}
	
	
	public static HTTPMessageConfigInterface createHeader(HTTPMessageConfigInterface hcc, int httpStatus)
	{
		
		if (hcc == null)
			hcc = HTTPMessageConfig.createAndInit(null, null, (HTTPMethod)null);
		
		HTTPStatusCode statusCode = HTTPStatusCode.statusByCode(httpStatus);
		
		if (statusCode == null)
			statusCode = HTTPStatusCode.INTERNAL_SERVER_ERROR;
		
		hcc.setHTTPStatusCode(statusCode);
		hcc.getHeaders().add(HTTPHeader.SERVER.getName(), NIO_PROXY);
		if (httpStatus == 501)
		{
			hcc.getHeaders().add(new NVPair("Allow", "GET, HEAD, POST, PUT, DELETE, CONNECT, PATCH"));
		}
		hcc.getHeaders().add(new NVPair("Cache-Control", "no-cache, must-revalidate"));
		hcc.getHeaders().add(new NVPair("Connection","close"));
		return hcc;
	}
	
	



	private boolean isRequestValid(RequestInfo requestInfo, UByteArrayOutputStream requestRawBuffer) throws IOException
	{
		if (((Boolean)getProperties().getValue(AUTHENTICATION)))
		{
			if (requestInfo.getHTTPMessageConfigInterface().getHeaders().get(HTTPHeader.PROXY_AUTHORIZATION) == null)
			{
				HTTPMessageConfigInterface hccError = createErrorMSG(HTTPStatusCode.PROXY_AUTHENTICATION_REQUIRED.CODE, HTTPStatusCode.PROXY_AUTHENTICATION_REQUIRED.REASON, requestMCCI.getURI());
				hccError.getHeaders().add(new NVPair(HTTPHeader.PROXY_AUTHENTICATE, "Basic "));
				ByteBufferUtil.write(phSChannel, HTTPUtil.formatResponse(hccError, requestRawBuffer));
				close();
				return false;	
			}
			
			requestInfo.getHTTPMessageConfigInterface().getHeaders().remove(HTTPHeader.PROXY_AUTHORIZATION);
		}
		return true;
	}
		
	
	
	private void tryToConnectRemote(UByteArrayOutputStream requestRawBuffer, int lastRead) throws IOException
	{
		
	
		// new code 
		if (requestMCCI == null)
		{
			requestMCCI = HTTPUtil.parseRawHTTPRequest(requestRawBuffer, requestMCCI, true);
			if (requestMCCI != null)
				requestInfo = new RequestInfo(requestMCCI, requestRawBuffer);
		}
		
		if (requestInfo != null)
		{
			if (!isRequestValid(requestInfo, requestRawBuffer))
			{
				log.info("returning not continuing");
				log.info("" + requestMCCI);
				return;
			}



			//InetSocketAddressDAO remoteAddress = HTTPUtil.parseHost(requestMCCI.getURI());
			if (requestMCCI.getMethod() == HTTPMethod.CONNECT)
			{

				relayConnection = true;
				if (NetUtil.checkSecurityStatus(getOutgoingInetFilterRulesManager(), requestInfo.remoteAddress.getInetAddress(), remoteChannel) !=  SecurityStatus.ALLOW)
				{
					HTTPMessageConfigInterface hccError = createErrorMSG(403, "Access Denied", requestMCCI.getURI());
					
					ByteBufferUtil.write(phSChannel, HTTPUtil.formatResponse(hccError, requestRawBuffer));
					close();
					return;	
					// we must reply with an error
				}
				if(remoteChannel != null && !NetUtil.areInetSocketAddressDAOEquals(requestInfo.remoteAddress, lastRemoteAddress))
				{
					log.info("NOT supposed to happen");
					//IOUtil.close(remoteChannel);
					if (channelRelay != null)
					{
						// try to read any pending data
						// very, very nasty bug
						channelRelay.accept(remoteChannelSK);
						channelRelay.waitThenStopReading(remoteChannelSK);
					}
					else
						getSelectorController().cancelSelectionKey(remoteChannelSK);
				}
				
				
				try
				{
					remoteChannel = SocketChannel.open((new InetSocketAddress(requestInfo.remoteAddress.getInetAddress(), requestInfo.remoteAddress.getPort())));
				}
				catch(Exception e)
				{
					
					HTTPMessageConfigInterface hccError = createErrorMSG(404, "Host Not Found", requestMCCI.getURI());
					
					ByteBufferUtil.write(phSChannel, HTTPUtil.formatResponse(hccError, requestRawBuffer));
					close();
					return;	
				}
				
				
				
    			requestRawBuffer.reset();


    			requestRawBuffer.write(requestMCCI.getHTTPVersion().getValue() + " 200 Connection established" + Delimiter.CRLF);
				//requestRawBuffer.write(HTTPVersion.HTTP_1_0.getValue() + " 200 Connection established" + ProtocolDelimiter.CRLF);
    			//if (requestInfo.remoteAddress.getPort() != 80)
    				requestRawBuffer.write(HTTPHeader.PROXY_AGENT + ": " +getName() + Delimiter.CRLFCRLF);
    			//else
    			//	requestRawBuffer.write(ProtocolDelimiter.CRLF.getBytes());
    			
    			// tobe tested
    			//remoteChannelSK = getSelectorController().register(NIOChannelCleaner.DEFAULT, remoteChannel, SelectionKey.OP_READ, new ChannelRelayTunnel(SourceOrigin.REMOTE, getReadBufferSize(), remoteChannel, clientChannel, clientChannelSK, true, getSelectorController()), FACTORY.isBlocking());
    			
    			ByteBufferUtil.write(phSChannel, requestRawBuffer);
    			requestRawBuffer.reset();
    			requestMCCI = null;
    			if(log.isEnabled())
    				log.getLogger().info(new String(requestRawBuffer.toByteArray()));
    			
    			
    			remoteChannelSK = getSelectorController().register(remoteChannel,
    													  		   SelectionKey.OP_READ,
    													  		   new ChannelRelayTunnel(ByteBufferUtil.DEFAULT_BUFFER_SIZE, remoteChannel, phSChannel, phSK, true, getSelectorController()),
    													  		  false);
    			requestInfo = null;
    			
			}
			else
			{
				if (log.isEnabled())
					log.getLogger().info(new String(requestRawBuffer.toByteArray()));
				
				
				if (NetUtil.checkSecurityStatus(getOutgoingInetFilterRulesManager(), requestInfo.remoteAddress.getInetAddress(), remoteChannel) !=  SecurityStatus.ALLOW)
				{
					HTTPMessageConfigInterface hccError = createErrorMSG(403, "Access Denied", requestMCCI.getURI());
				
					ByteBufferUtil.write(phSChannel, HTTPUtil.formatResponse(hccError, requestRawBuffer));
					close();
					return;	
					// we must reply with an error
				}
				
				if(!NetUtil.areInetSocketAddressDAOEquals(requestInfo.remoteAddress, lastRemoteAddress))
				{
					
					//IOUtil.close(remoteChannel);
					if (channelRelay != null)
					{
						channelRelay.accept(remoteChannelSK);
						channelRelay.waitThenStopReading(remoteChannelSK);
						if(log.isEnabled())
							log.getLogger().info("THIS IS  supposed to happen RELAY STOP:" +lastRemoteAddress + "," + requestInfo.remoteAddress);
					}
					else if (remoteChannelSK != null)
					{
						if (log.isEnabled())
							log.getLogger().info("THIS IS  supposed to happen CANCEL READ");
						getSelectorController().cancelSelectionKey(remoteChannelSK);
					}
				}
				
				
				
				
				if(remoteChannelSK == null || !remoteChannelSK.isValid())
				{
					//log.getLogger().info("ChangeConnection:" + changeConnection);
					try
					{
						remoteChannel = SocketChannel.open((new InetSocketAddress(requestInfo.remoteAddress.getInetAddress(), requestInfo.remoteAddress.getPort())));
						//remoteChannelSK = getSelectorController().register(remoteSet, remoteChannel, SelectionKey.OP_READ, new ChannelRelay(remoteChannel, clientChannel), FACTORY.isBlocking());
					}
					catch(Exception e)
					{
						if (log.isEnabled())
						{
							log.getLogger().info(new String(requestRawBuffer.toByteArray()));
							log.getLogger().info("" + requestInfo.remoteAddress);
							e.printStackTrace();
						}
						HTTPMessageConfigInterface hccError = createErrorMSG(404, "Host Not Found", requestInfo.remoteAddress.getInetAddress() + ":" + requestInfo.remoteAddress.getPort());
						ByteBufferUtil.write(phSChannel, HTTPUtil.formatResponse(hccError, requestRawBuffer));
						close();
						return;	
					}
				}
				
				if (requestMCCI.isMultiPartEncoding())
				{
					log.getLogger().info("We have multi Econding");
				}
				
			
				requestInfo.writeHeader(remoteChannel, requestRawBuffer);
				requestInfo.writePayload(remoteChannel, requestRawBuffer, 0);
				
//				if (requestInfo == null)
//				{
//					requestInfo = new RequestInfo(requestMCCI, requestRawBuffer);
//					ByteBufferUtil.write(remoteChannel, HTTPUtil.formatRequest(requestMCCI, true, null, HTTPHeaderName.PROXY_CONNECTION.getName()));
//					if (requestInfo.hmci.getContentLength() > 1)
//	    			{
//	    				ByteBufferUtil.write(remoteChannel, requestRawBuffer.getInternalBuffer(), requestInfo.payloadPendingIndex, requestRawBuffer.size() - requestInfo.payloadPendingIndex);
//	    				requestInfo.bytesSent += requestRawBuffer.size() - requestInfo.payloadPendingIndex;
//	    				requestRawBuffer.reset();
//	    				if (requestInfo.bytesSent == requestInfo.hmci.getContentLength())
//	    				{
//	    					requestInfo = null;
//	    	    			requestMCCI = null;
//	    	    			lastRemoteAddress = requestInfo.remoteAddress;
//	    					
//	    				}
//	    			}
//				}
				
				
				if(remoteChannelSK == null || !remoteChannelSK.isValid())
				{
					channelRelay = new ChannelRelayTunnel(ByteBufferUtil.DEFAULT_BUFFER_SIZE, remoteChannel, phSChannel, phSK, true, getSelectorController());
					remoteChannelSK = getSelectorController().register(remoteChannel, SelectionKey.OP_READ, channelRelay, false);
				}
				
				
				
    			
//				if (requestInfo != null && requestInfo.hmci.getContentLength()  > 1 && requestInfo.payloadSent != requestInfo.hmci.getContentLength() && requestRawBuffer.size() > 0)
//				{
//					ByteBufferUtil.write(remoteChannel, requestRawBuffer.getInternalBuffer(), 0, requestRawBuffer.size());
//    				requestInfo.payloadSent += requestRawBuffer.size();	
//				}
				
				
				lastRemoteAddress = requestInfo.remoteAddress;
				
				if (requestInfo != null && requestInfo.payloadSent == requestInfo.hmci.getContentLength() || requestInfo.hmci.getContentLength() < 1)
				{
					requestInfo = null;
	    			requestMCCI = null;
				}
				
//				lastRemoteAddress = requestInfo.remoteAddress;
//				requestRawBuffer.reset();
    			
			}
		}
		
		
		// code to be removed 
	
		
	}
	
	
	
	
	
	

	

	
	
	
	public static void main(String ...args)
	{
		try
		{
			
			int index = 0;
			IPAddress addressPort = new IPAddress(args[index++]);
			int threadCount = index < args.length ? Integer.parseInt(args[index++]) : 8;
			InetFilterRulesManager clientIFRM = null;
			TaskUtil.setThreadMultiplier(threadCount);
			
			String filename = null;
			
			for(; index < args.length; index++)
			{
				
				
				if ("-f".equalsIgnoreCase(args[index]))
				{
					filename = args[++index];
				}
				else
				{
					
					if(clientIFRM == null)
					{
						clientIFRM = new InetFilterRulesManager();
					}
					try
					{
						clientIFRM.addInetFilterProp(args[index]);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}
			log.getLogger().info("filename:" + filename);
			
			NIOProxyProtocolFactory factory = new NIOProxyProtocolFactory();
			//factory.setLogger(LoggerUtil.loggerToFile(NIOProxyProtocol.class.getName()+".proxy", filename));
			factory.setIncomingInetFilterRulesManager(clientIFRM);
			
			
			NIOSocket nios = new NIOSocket(TaskUtil.defaultTaskProcessor(), TaskUtil.defaultTaskScheduler());
			nios.addServerSocket(addressPort, 256, factory);
			nios.setStatLogCounter(0);
			Runnable cleaner = new Runnable()
					{
						NIOSocket toClose;
						Runnable init(NIOSocket nios)
						{
							this.toClose = nios;
							log.getLogger().info("Cleaner initiated");
							return this;
						}
						@Override
						public void run() 
						{
							long ts = System.nanoTime();
							// TODO Auto-generated method stub
							IOUtil.close(toClose);
							TaskUtil.defaultTaskScheduler().close();
							TaskUtil.defaultTaskProcessor().close();
							ts = System.nanoTime() - ts;
							log.getLogger().info("Cleanup took:" + ts);
						}
				
					}.init(nios);
			
			Runtime.getRuntime().addShutdownHook(new Thread(cleaner));

			log.getLogger().info("Proxy Started @ " + addressPort + " java version:" + System.getProperty("java.version") +
			 " thread count " + TaskUtil.defaultTaskProcessor().workersThreadCapacity());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.err.println("HTTPProxy <port> <thread-count>");
			TaskUtil.defaultTaskScheduler().close();
			TaskUtil.defaultTaskProcessor().close();
		}
	}

	
	
	@SuppressWarnings("unused")
	private static void logInfo(HTTPMessageConfigInterface hmci)
	{
		if (hmci != null)
		{
			if(hmci.getHeaders().get(HTTPHeader.CONTENT_LENGTH) != null)
				log.getLogger().info(""+hmci.getContentLength() + ", " +hmci.getContentType());
		}
	}
}