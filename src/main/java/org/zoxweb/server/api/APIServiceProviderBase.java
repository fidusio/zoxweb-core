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
package org.zoxweb.server.api;

import org.zoxweb.server.util.ServerUtil;
import org.zoxweb.shared.api.APIConfigInfo;
import org.zoxweb.shared.api.APIExceptionHandler;
import org.zoxweb.shared.api.APIServiceProvider;
import org.zoxweb.shared.data.DataConst.APIProperty;
import org.zoxweb.shared.util.Const;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.SUS;

import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("serial")
public abstract class APIServiceProviderBase<P, S>
        implements APIServiceProvider<P, S> {

    private long lastAccessTS;
    protected AtomicLong pendingCalls = new AtomicLong();
    private APIConfigInfo apiConfigInfo;
    private APIExceptionHandler apiExceptionHandler;

    protected APIServiceProviderBase() {
        touch();
    }

    /**
     * Updates the last time object was used (milliseconds).
     * @return last time accessed
     */
    protected synchronized long touch() {
        lastAccessTS = System.currentTimeMillis();
        return lastAccessTS;
    }

    /**
     * Returns the last time it was accessed (milliseconds).
     * @return the last it was accessed in millis
     */
    public long lastTimeAccessed() {
        return lastAccessTS;
    }

    /**
     * Return the delta between NOW and last time the object was used.
     * @return the inactivity duration in millis
     */
    public long inactivityDuration() {
        return System.currentTimeMillis() - lastTimeAccessed();
    }


    public APIConfigInfo getAPIConfigInfo() {
        return apiConfigInfo;
    }


    public void setAPIConfigInfo(APIConfigInfo configInfo) {
        apiConfigInfo = configInfo;
    }


    public APIExceptionHandler getAPIExceptionHandler() {
        return apiExceptionHandler;
    }


    public void setAPIExceptionHandler(APIExceptionHandler exceptionHandler) {
        this.apiExceptionHandler = exceptionHandler;
    }

    @SuppressWarnings("unchecked")
    public <T> T lookupProperty(GetName propertyName) {
        SUS.checkIfNulls("Null property name", propertyName);

        if (propertyName instanceof APIProperty) {
            APIProperty apiProperty = (APIProperty) propertyName;

            switch (apiProperty) {
                case ASYNC_CREATE:
                    return (T) Boolean.TRUE;
                case RETRY_DELAY:
                    return (T) Long.valueOf(Const.TimeInMillis.SECOND.MILLIS * ServerUtil.RNG.nextInt(4) + Const.TimeInMillis.SECOND.MILLIS * 2);
                case ASYNC_DELETE:
                case ASYNC_READ:
                case ASYNC_UPDATE:
                default:
                    break;

            }
        }

        return null;
    }

    public boolean isBusy() {
        return pendingCalls.get() != 0;
    }

}