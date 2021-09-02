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

import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.zoxweb.shared.util.SharedUtil;

public class SSLSessionDataFactory 
{
	private SSLContext sslContext;
	private volatile Executor executor;

	public SSLSessionDataFactory()
	{
		
	}
	
	public SSLSessionDataFactory(SSLContext sslContext, Executor executor)
	{
		setSSLContext(sslContext);
		setExecutor(executor);
	}

	public void setSSLContext(SSLContext sslContext)
	{
		SharedUtil.checkIfNulls("Can't have a null sllContext", sslContext);
		this.sslContext = sslContext;
	}

	public SSLContext getSSLContext()
	{
		return sslContext;
	}
	
	public Executor getExecutor() 
	{
		return executor;
	}

	public void setExecutor(Executor executor)
	{
		this.executor = executor;
	}
	
	public SSLSessionData create(boolean client)
	{
		SSLEngine sslEngine = sslContext.createSSLEngine();
		sslEngine.setUseClientMode(client);
		//slEngine.setWantClientAuth(true);
		SSLSessionData ret = new SSLSessionData(sslEngine, executor);
		
		return ret;
	}

}