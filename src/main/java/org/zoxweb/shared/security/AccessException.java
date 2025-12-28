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
package org.zoxweb.shared.security;

import org.zoxweb.shared.util.ExceptionReason;

/**
 * This exception is caused by a security access revocation
 *
 * @author mnael
 */
@SuppressWarnings("serial")
public class AccessException
        extends RuntimeException
        implements ExceptionReason {
    private String urlRedirect = null;
    private boolean reload = false;
    private int statusCode = -1;


    private Reason reason = Reason.ACCESS_DENIED;

    public AccessException() {
        super();

    }

    public AccessException(String message) {
        super(message);
    }

    public AccessException(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }

    public AccessException(String message, String urlRedirect) {
        this(message, urlRedirect, false);

    }

    public AccessException(String message, String urlRedirect, boolean reload) {
        super(message);
        this.urlRedirect = urlRedirect;
        this.reload = reload;
    }

    /**
     * Get the url redirect
     *
     * @return the url redirect
     */
    public String getURLRedirect() {
        return urlRedirect;
    }


    /**
     * Set the redirect url optional
     *
     * @param urlRedirect
     */
    public void setURLRedirect(String urlRedirect) {
        this.urlRedirect = urlRedirect;
    }

    public boolean isReloadRequired() {
        return reload;
    }

    public void setReloadRequired(boolean reload) {
        this.reload = reload;
    }


    public Reason getReason() {
        // TODO Auto-generated method stub
        return reason;
    }

    public void setReason(Reason reason) {
        // TODO Auto-generated method stub
        this.reason = reason;
    }

    @Override
    public int getStatusCode() {
        // TODO Auto-generated method stub
        return statusCode;
    }

    @Override
    public void setStatusCode(int code) {
        // TODO Auto-generated method stub
        statusCode = code;
    }

}