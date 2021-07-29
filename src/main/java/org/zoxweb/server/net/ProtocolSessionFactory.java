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
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.NVGenericMap;

public interface  ProtocolSessionFactory<P extends ProtocolSessionProcessor>
extends GetName
{
	/**
	 * Create a new instance of the underlying protocol
	 * @return  new instance of session protocol
	 */
	P newInstance();
	/**
	 * True if the factory is of blocking type
	 * @return blocking status
	 */
	boolean isBlocking();

	
	/**
	 * Get the SSL session factory
	 * @return SSL sesstion factory
	 */
	SSLSessionDataFactory getIncomingSSLSessionDataFactory();
	
	/**
	 * Set the SSL session factory for incoming connection
	 * @param sslSessionDataFactory
	 */
	void setIncomingSSLSessionDataFactory(SSLSessionDataFactory sslSessionDataFactory);
	
	/**
	 * Set the filer rule manager for incoming connections
	 * @return filter manager
	 */
	InetFilterRulesManager getIncomingInetFilterRulesManager();
	/**
	 * Set the incoming filter rule manager.
	 * @param incomingIFRM input filter manager
	 */
	void setIncomingInetFilterRulesManager(InetFilterRulesManager incomingIFRM);
	
	/**
	 * Get the outgoing connection rule manager if applicable
	 * @return
	 */
	InetFilterRulesManager getOutgoingInetFilterRulesManager();
	/**
	 * Set the outgoing connection rule manager if applicable
	 * @param incomingIFRM
	 */
	void setOutgoingInetFilterRulesManager(InetFilterRulesManager incomingIFRM);
	
	/**
	 * Get the logger
	 * @return
	 */
	Logger getLogger();
	
	/**
	 * Set the logger
	 * @param logger
	 */
	void setLogger(Logger logger);
	
	
	/**
	 * Get the generic properties
	 * @return
	 */
	NVGenericMap getSessionProperties();
	
}
