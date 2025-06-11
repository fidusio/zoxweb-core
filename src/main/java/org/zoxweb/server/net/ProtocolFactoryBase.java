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


import org.zoxweb.shared.util.NVGenericMap;


public abstract class ProtocolFactoryBase<P extends ProtocolHandler>
        implements ProtocolFactory<P> {

    private volatile InetFilterRulesManager incomingInetFilterRulesManager;
    private volatile InetFilterRulesManager outgoingInetFilterRulesManager;

    private final NVGenericMap properties = new NVGenericMap();


    protected boolean complexSetup = false;


    @Override
    public boolean isBlocking() {
        // TODO Auto-generated method stub
        return false;
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

    public NVGenericMap getProperties() {
        return properties;
    }

    public boolean isComplexSetup() {
        return complexSetup;
    }

}
