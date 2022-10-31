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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;


import org.zoxweb.server.http.proxy.NIOProxyProtocol.NIOProxyProtocolFactory;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.server.logging.LoggerUtil;
import org.zoxweb.server.net.InetFilterRulesManager;
import org.zoxweb.server.net.NIOSocket;
import org.zoxweb.server.net.InetFilterRulesManager.InetFilterRule;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.ApplicationConfigManager;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.data.ApplicationConfigDAO;

import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

/**
 *
 */
public class JHTTPPUtil 
{
	
	//private static final transient Logger log = Logger.getLogger(Const.LOGGER_NAME);
	public final static LogWrapper log = new LogWrapper(JHTTPPUtil.class);
	
	private JHTTPPUtil()
	{
		
	}
	
	
	//
	// Copyright (C)1996,1998 by Jef Poskanzer <jef@acme.com>. All rights
	// reserved.
	//
	// Redistribution and use in source and binary forms, with or without
	// modification, are permitted provided that the following conditions
	// are met:
	// 1. Redistributions of source code must retain the above copyright
	// notice, this list of conditions and the following disclaimer.
	// 2. Redistributions in binary form must reproduce the above copyright
	// notice, this list of conditions and the following disclaimer in the
	// documentation and/or other materials provided with the distribution.
	//
	// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
	// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
	// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
	// PURPOSE
	// ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
	// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
	// CONSEQUENTIAL
	// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
	// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
	// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
	// STRICT
	// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
	// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
	// SUCH DAMAGE.
	//
	// Visit the ACME Labs Java page for up-to-date versions of this and other
	// fine Java utilities: http://www.acme.com/java/

	// / URLDecoder to go along with java.net.URLEncoder. Why there isn't
	// already a decoder in the standard library is a mystery to me.
	public static String urlDecoder(String encoded) 
	{
		StringBuffer decoded = new StringBuffer();
		int len = encoded.length();
		for (int i = 0; i < len; ++i) {
			if (encoded.charAt(i) == '%' && i + 2 < len) {
				int d1 = Character.digit(encoded.charAt(i + 1), 16);
				int d2 = Character.digit(encoded.charAt(i + 2), 16);
				if (d1 != -1 && d2 != -1)
					decoded.append((char) ((d1 << 4) + d2));
				i += 2;
			} else if (encoded.charAt(i) == '+')
				decoded.append(' ');
			else
				decoded.append(encoded.charAt(i));
		}
		return decoded.toString();
	}
	
	
	public static JHTTPPServer proxySetup(int port, String fileName) throws NullPointerException, IOException
	{
		InetFilterRulesManager ifrm = null; 

		
		log.info("proxy rules filename:"+fileName);
		if (fileName != null)
		{
			File file = new File(fileName);
			if ( file.exists())
			{
				log.info("proxy rules filename:"+fileName + " exists");
				
				String json = IOUtil.inputStreamToString( new FileInputStream( file), true);
				List<InetFilterRule> filters = GSONUtil.fromJSONs(json, Base64Type.DEFAULT, InetFilterRule.class);
				ifrm =  new InetFilterRulesManager();
				ifrm.setAll(filters);
				
			}
			else
			{
				log.info("proxy rules filename:"+fileName  + " do not exit");
			}
		}
		
		JHTTPPServer proxy = new JHTTPPServer(port, ifrm);
	
	
		log.info("Proxy set properly:" + proxy.getProxyPort());
		log.info("Proxy filter rules:" + (ifrm != null ?  ifrm.getAll() : ""));
		
		return proxy;
		
	}
	
	public static JHTTPPServer proxySetup(ApplicationConfigDAO acd) throws NullPointerException, IOException
	{
		// setup default filter
		InetFilterRulesManager ifrm = new InetFilterRulesManager();
//		InetFilterDAO ipf = new InetFilterDAO();
//		ipf.setIP("127.0.0.1");
//		ipf.setNetworkMask("255.255.255.255");
		int port = 8080;
		
		// load the proxy rules
		if (acd != null)
		{
			String proxyOn = acd.lookupValue("proxy_start");
			if (!( proxyOn != null && Boolean.parseBoolean(proxyOn)))
			{
				return null;
			}
			
			
			String proxyRulesFileName = acd.lookupValue("proxy_rules");
			log.info("proxy_rules:" + proxyRulesFileName);
			if (proxyRulesFileName != null)
			{
				String fullFileName = ApplicationConfigManager.SINGLETON.concatWithEnvVar("conf", proxyRulesFileName);
				log.info("proxy rules filename:"+fullFileName);
				File file = new File(fullFileName);
				if ( file.exists())
				{
					log.info("proxy rules filename:"+fullFileName + " exists");
					try
					{
						String json = IOUtil.inputStreamToString(new FileInputStream( file), true);
						List<InetFilterRule> filters = GSONUtil.fromJSONs(json, Base64Type.DEFAULT, InetFilterRule.class);
						ifrm.setAll(filters);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
				else
				{
					log.info("proxy rules filename:"+fullFileName  + "do not exit");
				}
			}
			
			String proxyPort = acd.lookupValue("proxy_port");
			if ( !SharedStringUtil.isEmpty( proxyPort))
			{
				try
				{
					port = Integer.parseInt( proxyPort);
				}
				catch( NumberFormatException e)
				{
					e.printStackTrace();
				}
				
			}
		}
		else
		{
			return null;
		}
			
			
		
		JHTTPPServer proxy = new JHTTPPServer(port, ifrm);
		
		
		
		proxy.setDebugEnabled(acd.lookupValue("proxy_debug") != null ? Boolean.parseBoolean(acd.lookupValue("proxy_debug")) : false);
	
		log.info("Proxy set properly:" + proxy.getProxyPort());
		log.info("Proxy filter rules:" + ifrm.getAll());
		
		return proxy;
	}
	
	
	public static NIOSocket nioProxySetup(ApplicationConfigDAO acd) throws NullPointerException, IOException
	{
		// setup default filter
		InetFilterRulesManager ifrm = new InetFilterRulesManager();
//		InetFilterDAO ipf = new InetFilterDAO();
//		ipf.setIP("127.0.0.1");
//		ipf.setNetworkMask("255.255.255.255");
		//TaskUtil.setThreadMultiplier(4);
		int port = 8080;
		int backlog = 256;
		String proxyLogFile = null;
		
		// load the proxy rules
		if (acd != null)
		{
			String proxyOn = acd.lookupValue("proxy_start");
			if (!( proxyOn != null && Boolean.parseBoolean(proxyOn)))
			{
				return null;
			}
			
			
			String proxyRulesFileName = acd.lookupValue("proxy_rules");
			log.info("proxy_rules:" + proxyRulesFileName);
			if (proxyRulesFileName != null)
			{
				String fullFileName = ApplicationConfigManager.SINGLETON.concatWithEnvVar("conf", proxyRulesFileName);
				log.info("proxy rules filename:"+fullFileName);
				File file = new File(fullFileName);
				if ( file.exists())
				{
					log.info("proxy rules filename:"+fullFileName + " exists");
					try
					{
						String json = IOUtil.inputStreamToString(new FileInputStream( file), true);
						List<InetFilterRule> filters = GSONUtil.fromJSONs(json, Base64Type.DEFAULT, InetFilterRule.class);
						ifrm.setAll(filters);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
				else
				{
					log.info("proxy rules filename:"+fullFileName  + "do not exit");
				}
			}
			
			
			try 
			{
				port = acd.getProperties().getValue("proxy_port");
				backlog = acd.getProperties().getValue("port_backlog");
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			
			
			proxyLogFile = acd.lookupValue("proxy_log_file");
		}
		else
		{
			return null;
		}
		
		NIOProxyProtocolFactory factory = new NIOProxyProtocolFactory();
		factory.setIncomingInetFilterRulesManager(ifrm);
		factory.setLogger(LoggerUtil.loggerToFile(NIOProxyProtocol.class.getName()+".proxy", proxyLogFile));
			
		NIOSocket nsio = new NIOSocket(new InetSocketAddress(port), backlog, factory, TaskUtil.getDefaultTaskProcessor());	
		
		
	
		log.info("Proxy set properly:" + port);
		log.info("Proxy filter rules:" + ifrm.getAll());
		
		return nsio;
	}
}
