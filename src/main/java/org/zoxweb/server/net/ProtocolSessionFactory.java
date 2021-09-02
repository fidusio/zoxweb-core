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


import org.zoxweb.shared.util.GetNVProperties;
import org.zoxweb.shared.util.GetName;

public interface  ProtocolSessionFactory<P extends ProtocolProcessor>
extends GetName, GetNVProperties
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
	 * Set the filer rule manager for incoming connections
	 * @return incoming filter manager
	 */
	InetFilterRulesManager getIncomingInetFilterRulesManager();

	/**
	 * Set the incoming filter rule manager.
	 * @param incomingIFRM input filter manager
	 */
	void setIncomingInetFilterRulesManager(InetFilterRulesManager incomingIFRM);
	
	/**
	 * Get the outgoing connection rule manager if applicable
	 * @return outgoing filter
	 */
	InetFilterRulesManager getOutgoingInetFilterRulesManager();
	/**
	 * Set the outgoing connection rule manager if applicable
	 * @param outgoingIFRM outgoing filter
	 */
	void setOutgoingInetFilterRulesManager(InetFilterRulesManager outgoingIFRM);
	
	/**
	 * Get the custom logger
	 * @return custom logger
	 */
	Logger getLogger();

	/**
	 * Set the logger custom logger
	 * @param logger to be set
	 */
	void setLogger(Logger logger);


	/**
	 *
	 */
	void init();
	
}
