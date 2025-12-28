/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
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

import org.zoxweb.shared.util.GetNVProperties;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.InstanceFactory;

public interface ProtocolFactory<P extends ProtocolHandler>
        extends GetName, GetNVProperties, InstanceFactory.Creator<P> {


    /**
     * True if the factory is of blocking type
     *
     * @return blocking status
     */
    boolean isBlocking();

    /**
     * Set the filer rule manager for incoming connections
     *
     * @return incoming filter manager
     */
    InetFilterRulesManager getIncomingInetFilterRulesManager();

    /**
     * Set the incoming filter rule manager.
     *
     * @param incomingIFRM input filter manager
     */
    void setIncomingInetFilterRulesManager(InetFilterRulesManager incomingIFRM);

    /**
     * Get the outgoing connection rule manager if applicable
     *
     * @return outgoing filter
     */
    InetFilterRulesManager getOutgoingInetFilterRulesManager();

    /**
     * Set the outgoing connection rule manager if applicable
     *
     * @param outgoingIFRM outgoing filter
     */
    void setOutgoingInetFilterRulesManager(InetFilterRulesManager outgoingIFRM);


    /**
     * Init the protocol factory
     */
    void init();


    /**
     * If true it suggest to use a thread for the connection setup if a thread pool is available
     *
     * @return the setup mode
     */
    boolean isComplexSetup();

}
