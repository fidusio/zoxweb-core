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


import java.util.logging.Logger;

import org.zoxweb.server.net.security.SSLSessionDataFactory;
import org.zoxweb.shared.util.NVGenericMap;
;

public abstract class ProtocolSessionFactoryBase<P extends ProtocolSessionProcessor>
	implements ProtocolSessionFactory<P>
{

	private InetFilterRulesManager incomingInetFilterRulesManager;
	private InetFilterRulesManager outgoingInetFilterRulesManager;
	protected SSLSessionDataFactory incomingSSLSessionFactory = null;
	private Logger logger;
	private NVGenericMap properties = new NVGenericMap();
	

	
//	protected ProtocolSessionFactoryBase(InetFilterRulesManager incoming, InetFilterRulesManager outgoing, SSLUtil sslUtil)
//	{
//		setIncomingInetFilterRulesManager(incoming);
//		setOutgoingInetFilterRulesManager(outgoing);
//		this.sslUtil = sslUtil;
//	}
	
	
	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	@Override
	public boolean isBlocking() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SSLSessionDataFactory getIncomingSSLSessionDataFactory() {
		// TODO Auto-generated method stub
		return incomingSSLSessionFactory;
	}
	
	
	public void setIncomingSSLSessionDataFactory(SSLSessionDataFactory sslsdf) {
		// TODO Auto-generated method stub
		incomingSSLSessionFactory = sslsdf;
	}

	@Override
	public InetFilterRulesManager getIncomingInetFilterRulesManager() {
		// TODO Auto-generated method stub
		return incomingInetFilterRulesManager;
	}

	@Override
	public void setIncomingInetFilterRulesManager(InetFilterRulesManager incomingIFRM) {
		// TODO Auto-generated method stub
		incomingInetFilterRulesManager = incomingIFRM;
	}
	
	
	@Override
	public InetFilterRulesManager getOutgoingInetFilterRulesManager() {
		// TODO Auto-generated method stub
		return outgoingInetFilterRulesManager;
	}

	@Override
	public void setOutgoingInetFilterRulesManager(InetFilterRulesManager incomingIFRM) {
		// TODO Auto-generated method stub
		outgoingInetFilterRulesManager = incomingIFRM;
	}
	
	
	public NVGenericMap getSessionProperties()
	{
		return properties;
	}

}
