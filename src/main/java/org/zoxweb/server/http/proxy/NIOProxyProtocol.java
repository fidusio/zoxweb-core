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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import org.zoxweb.server.http.HTTPUtil;
import org.zoxweb.server.io.ByteBufferUtil;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.server.logging.LoggerUtil;
import org.zoxweb.server.io.ByteBufferUtil.BufferType;
import org.zoxweb.server.net.ChannelRelayTunnel;
import org.zoxweb.server.net.InetFilterRulesManager;
import org.zoxweb.server.net.NIOChannelCleaner;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.NetUtil;
import org.zoxweb.server.net.ProtocolSessionFactoryBase;
import org.zoxweb.server.net.ProtocolSessionProcessor;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPHeaderName;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.http.HTTPStatusCode;
import org.zoxweb.shared.net.InetSocketAddressDAO;
import org.zoxweb.shared.protocol.ProtocolDelimiter;
import org.zoxweb.shared.security.SecurityStatus;
import org.zoxweb.shared.util.Const.SourceOrigin;
import org.zoxweb.shared.util.NVBoolean;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.SharedStringUtil;

public class NIOProxyProtocol 
	extends ProtocolSessionProcessor
{

	private static boolean debug = false;
	private static final transient Logger log = Logger.getLogger(NIOProxyProtocol.class.getName());
	public static final String NAME = "ZWNIOProxy";
	
	public static final String AUTHENTICATION = "AUTHENTICATION";

	private static class RequestInfo
	{
		HTTPMessageConfigInterface hmci = null;
		int payloadIndex = -1;
		int payloadSent = 0;
		InetSocketAddressDAO remoteAddress = null;
		boolean headerNotSent = true;
		
		RequestInfo(HTTPMessageConfigInterface hmci, UByteArrayOutputStream ubaos)
		{
			this.hmci = hmci;
			remoteAddress = HTTPUtil.parseHost(hmci.getURI());
			//if (hmci.getContentLength() > 0)
			{
				payloadIndex = ubaos.indexOf(ProtocolDelimiter.CRLFCRLF.getBytes());
				if (payloadIndex > 0)
				{
					payloadIndex += ProtocolDelimiter.CRLFCRLF.getBytes().length;
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
				ByteBufferUtil.write(remoteChannel, HTTPUtil.formatRequest(hmci, true, null, HTTPHeaderName.PROXY_CONNECTION.getName()));
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
		extends ProtocolSessionFactoryBase<NIOProxyProtocol>
	{

		public NIOProxyProtocolFactory()
		{
			getSessionProperties().add(new NVBoolean(NIOProxyProtocol.AUTHENTICATION, false));
		}
		
		@Override
		public NIOProxyProtocol newInstance()
		{
			NIOProxyProtocol ret = new NIOProxyProtocol();
			ret.setOutgoingInetFilterRulesManager(getOutgoingInetFilterRulesManager());
			ret.setProperties(getSessionProperties());
			
			return ret;
		}
		
	}
	
	private UByteArrayOutputStream requestBuffer = new UByteArrayOutputStream();
	private HTTPMessageConfigInterface requestMCCI = null;
	private InetSocketAddressDAO lastRemoteAddress = null;
	private SocketChannel remoteChannel = null;
	private SelectionKey  remoteChannelSK = null;
	private SocketChannel clientChannel = null;
	private SelectionKey  clientChannelSK = null;
	private ChannelRelayTunnel channelRelay = null;
	private RequestInfo requestInfo = null;

	//private int changeConnection = 0;
	//protected Set<SelectionKey> remoteSet = null;//new HashSet<SelectionKey>();
	private boolean ssl = false;

	private NIOProxyProtocol()
	{
		bBuffer =  ByteBufferUtil.allocateByteBuffer(BufferType.HEAP, getReadBufferSize());
	}
	
	
	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getDescription() 
	{
		return "Experimental http proxy";
	}

	@Override
	public void close() throws IOException
	{
		IOUtil.close(clientChannel);
		//if (ssl)
		{
			if (channelRelay != null)
			{
				IOUtil.close(channelRelay);
			}
			else
			{
				IOUtil.close(remoteChannel);
			}
		}
		postOp();
	}

	@Override
	protected void readData(SelectionKey key) 
	{
		try
    	{
			if (clientChannel == null)
			{
				clientChannel = (SocketChannel)key.channel();
				clientChannelSK = key;
			}
			
			
			int read = 0;
    		do
    		{
    			bBuffer.clear();
    			
    			read = ((SocketChannel)key.channel()).read(bBuffer);
    			if (read > 0)
    			{
    				if (ssl)
    				{
    					ByteBufferUtil.write(remoteChannel, bBuffer);
    					//System.out.println(ByteBufferUtil.toString(bBuffer));
    				}
    				else
    				{
    					ByteBufferUtil.write(requestBuffer, bBuffer);
    					//log.info(new String(requestBuffer.getInternalBuffer(), 0, requestBuffer.size()));
    					tryToConnectRemote(requestBuffer, read);
    				}	
    			}

    		}
    		while(read > 0);

    		if (read == -1)
    		{
    			if(debug)
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
    		if(debug)
    			e.printStackTrace();
    		IOUtil.close(this);
    		
    	}
    	
    	//setSeletectable(true);

	}

	

	
	
	public static  HTTPMessageConfigInterface createErrorMSG(int a, String errorMsg, String url)
	{
		HTTPMessageConfigInterface hcc = createHeader(null, a);
		
		hcc.getHeaderParameters().add(new NVPair("Content-Type","text/html; charset=iso-8859-1"));
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
		+ "<p class=\"i25\"><A HREF=\"http://www.zoxweb.com/\">ProxyNIO</A> HTTP Proxy, Version " + NAME
		+"</p>\r"
		//+"<p class=\"i25\"><A HREF=\"http://" + localhost + "/\">jHTTPp2 local website</A> <A HREF=\"http://" + localhost + "/" + server.WEB_CONFIG_FILE + "\">Configuration</A></p>"
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
		hcc.getHeaderParameters().add(new NVPair("Server", NAME));
		if (httpStatus == 501)
		{
			hcc.getHeaderParameters().add(new NVPair("Allow", "GET, HEAD, POST, PUT, DELETE, CONNECT, PATCH"));
		}
		hcc.getHeaderParameters().add(new NVPair("Cache-Control", "no-cache, must-revalidate"));
		hcc.getHeaderParameters().add(new NVPair("Connection","close"));
		return hcc;
	}
	
	



	private boolean isRequestValid(RequestInfo requestInfo, UByteArrayOutputStream requestRawBuffer) throws IOException
	{
		if (((Boolean)getProperties().getValue(AUTHENTICATION)))
		{
			if (requestInfo.getHTTPMessageConfigInterface().getHeaderParameters().get(HTTPHeaderName.PROXY_AUTHORIZATION) == null)
			{
				HTTPMessageConfigInterface hccError = createErrorMSG(HTTPStatusCode.PROXY_AUTHENTICATION_REQUIRED.CODE, HTTPStatusCode.PROXY_AUTHENTICATION_REQUIRED.REASON, requestMCCI.getURI());
				hccError.getHeaderParameters().add(new NVPair(HTTPHeaderName.PROXY_AUTHENTICATE, "Basic "));
				ByteBufferUtil.write(clientChannel, HTTPUtil.formatResponse(hccError, requestRawBuffer));
				close();
				return false;	
			}
			
			requestInfo.getHTTPMessageConfigInterface().getHeaderParameters().remove(HTTPHeaderName.PROXY_AUTHORIZATION);
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
				return;
			}	
			//InetSocketAddressDAO remoteAddress = HTTPUtil.parseHost(requestMCCI.getURI());
			if (requestMCCI.getMethod() == HTTPMethod.CONNECT)
			{

				ssl = true;
				if (NetUtil.checkSecurityStatus(getOutgoingInetFilterRulesManager(), requestInfo.remoteAddress.getInetAddress(), remoteChannel) !=  SecurityStatus.ALLOW)
				{
					HTTPMessageConfigInterface hccError = createErrorMSG(403, "Access Denied", requestMCCI.getURI());
					
					ByteBufferUtil.write(clientChannel, HTTPUtil.formatResponse(hccError, requestRawBuffer));
					close();
					return;	
					// we must reply with an error
				}
				if(remoteChannel != null && !NetUtil.areInetSocketAddressDAOEquals(requestInfo.remoteAddress, lastRemoteAddress))
				{
					if(debug)
						log.info("NOT supposed to happen");
					//IOUtil.close(remoteChannel);
					if (channelRelay != null)
					{
						// try to read any pending data
						// very very nasty bug
						channelRelay.readData(remoteChannelSK);
						channelRelay.waitThenStopReading();
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
					
					ByteBufferUtil.write(clientChannel, HTTPUtil.formatResponse(hccError, requestRawBuffer));
					close();
					return;	
				}
				
				
				
    			requestRawBuffer.reset();
    			
    			requestRawBuffer.write(requestMCCI.getHTTPVersion().getValue() + " 200 Connection established" + ProtocolDelimiter.CRLF);
    			//if (requestInfo.remoteAddress.getPort() != 80)
    				requestRawBuffer.write(HTTPHeaderName.PROXY_AGENT + ": " +getName() + ProtocolDelimiter.CRLFCRLF);
    			//else
    			//	requestRawBuffer.write(ProtocolDelimiter.CRLF.getBytes());
    			
    			// tobe tested
    			//remoteChannelSK = getSelectorController().register(NIOChannelCleaner.DEFAULT, remoteChannel, SelectionKey.OP_READ, new ChannelRelayTunnel(SourceOrigin.REMOTE, getReadBufferSize(), remoteChannel, clientChannel, clientChannelSK, true, getSelectorController()), FACTORY.isBlocking());
    			
    			ByteBufferUtil.write(clientChannel, requestRawBuffer);
    			requestRawBuffer.reset();
    			requestMCCI = null;
    			if(debug)
    				log.info(new String(requestRawBuffer.toByteArray()));
    			
    			
    			remoteChannelSK = getSelectorController().register(NIOChannelCleaner.DEFAULT,
    													  		   remoteChannel,
    													  		   SelectionKey.OP_READ,
    													  		   new ChannelRelayTunnel(SourceOrigin.REMOTE, getReadBufferSize(), remoteChannel, clientChannel, clientChannelSK, true, getSelectorController()),
    													  		   false);
    			requestInfo = null;
    			
			}
			else
			{
				if (debug)
					log.info(new String(requestRawBuffer.toByteArray()));
				
				
				if (NetUtil.checkSecurityStatus(getOutgoingInetFilterRulesManager(), requestInfo.remoteAddress.getInetAddress(), remoteChannel) !=  SecurityStatus.ALLOW)
				{
					HTTPMessageConfigInterface hccError = createErrorMSG(403, "Access Denied", requestMCCI.getURI());
				
					ByteBufferUtil.write(clientChannel, HTTPUtil.formatResponse(hccError, requestRawBuffer));
					close();
					return;	
					// we must reply with an error
				}
				
				if(!NetUtil.areInetSocketAddressDAOEquals(requestInfo.remoteAddress, lastRemoteAddress))
				{
					
					//IOUtil.close(remoteChannel);
					if (channelRelay != null)
					{
						channelRelay.readData(remoteChannelSK);
						channelRelay.waitThenStopReading();
						if(debug)
							log.info("THIS IS  supposed to happen RELAY STOP:" +lastRemoteAddress + "," + requestInfo.remoteAddress);
					}
					else if (remoteChannelSK != null)
					{
						if (debug)
							log.info("THIS IS  supposed to happen CANCEL READ");
						getSelectorController().cancelSelectionKey(remoteChannelSK);
					}
				}
				
				
				
				
				if(remoteChannelSK == null || !remoteChannelSK.isValid())
				{
					//log.info("ChangeConnection:" + changeConnection);
					try
					{
						remoteChannel = SocketChannel.open((new InetSocketAddress(requestInfo.remoteAddress.getInetAddress(), requestInfo.remoteAddress.getPort())));
						//remoteChannelSK = getSelectorController().register(remoteSet, remoteChannel, SelectionKey.OP_READ, new ChannelRelay(remoteChannel, clientChannel), FACTORY.isBlocking());
					}
					catch(Exception e)
					{
						if (debug)
						{
							log.info(new String(requestRawBuffer.toByteArray()));
							log.info("" + requestInfo.remoteAddress);
							e.printStackTrace();
						}
						HTTPMessageConfigInterface hccError = createErrorMSG(404, "Host Not Found", requestInfo.remoteAddress.getInetAddress() + ":" + requestInfo.remoteAddress.getPort());
						ByteBufferUtil.write(clientChannel, HTTPUtil.formatResponse(hccError, requestRawBuffer));
						close();
						return;	
					}
				}
				
				if (requestMCCI.isMultiPartEncoding())
				{
					log.info("We have multi Econding");
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
					channelRelay = new ChannelRelayTunnel(SourceOrigin.REMOTE, getReadBufferSize(), remoteChannel, clientChannel, clientChannelSK, true, getSelectorController());
					remoteChannelSK = getSelectorController().register(NIOChannelCleaner.DEFAULT, remoteChannel, SelectionKey.OP_READ, channelRelay, false);
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
	
	
	
	
	
	

	

	
	
	@SuppressWarnings("resource")
	public static void main(String ...args)
	{
		try
		{
			
			int index = 0;
			int port = Integer.parseInt(args[index++]);
			InetFilterRulesManager clientIFRM = null;
			TaskUtil.setThreadMultiplier(4);
			
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
			log.info("filename:" + filename);
			
			NIOProxyProtocolFactory factory = new NIOProxyProtocolFactory();
			factory.setLogger(LoggerUtil.loggerToFile(NIOProxyProtocol.class.getName()+".proxy", filename));
			factory.setIncomingInetFilterRulesManager(clientIFRM);
			
			
			NIOSocket nios = new NIOSocket(factory, new InetSocketAddress(port), TaskUtil.getDefaultTaskProcessor());
			nios.setStatLogCounter(0);
			
			//nios.addSeverSocket(2401, new NIOTunnelFactory(new InetSocketAddressDAO("10.0.0.1:2401")));
			Runnable cleaner = new Runnable()
					{
						NIOSocket toClose;
						Runnable init(NIOSocket nios)
						{
							this.toClose = nios;
							log.info("Cleaner initiated");
							return this;
						}
						@Override
						public void run() 
						{
							long ts = System.nanoTime();
							// TODO Auto-generated method stub
							IOUtil.close(toClose);
							TaskUtil.getDefaultTaskScheduler().close();
							TaskUtil.getDefaultTaskProcessor().close();
							ts = System.nanoTime() - ts;
							log.info("Cleanup took:" + ts);
						}
				
					}.init(nios);
			
			Runtime.getRuntime().addShutdownHook(new Thread(cleaner));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.err.println("HTTPProxy <port>");
			TaskUtil.getDefaultTaskScheduler().close();
			TaskUtil.getDefaultTaskProcessor().close();
		}
	}

	
	
	@SuppressWarnings("unused")
	private static void logInfo(HTTPMessageConfigInterface hmci)
	{
		if (hmci != null)
		{
			if(hmci.getHeaderParameters().get(HTTPHeaderName.CONTENT_LENGTH) != null)
			log.info(""+hmci.getContentLength() + ", " +hmci.getContentType());
		}
	}
}