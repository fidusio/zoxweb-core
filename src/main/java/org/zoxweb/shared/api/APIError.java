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
package org.zoxweb.shared.api;

import org.zoxweb.shared.api.APIException.Category;
import org.zoxweb.shared.api.APIException.Code;
import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class APIError
        extends SetNameDescriptionDAO {

    public enum Params
            implements GetNVConfig {

        CODE(NVConfigManager.createNVConfig("code", "Error code", "Code", false, true, String.class)),
        MESSAGE(NVConfigManager.createNVConfig("message", "Error message", "Message", false, true, String.class)),
        CAUSE(NVConfigManager.createNVConfig("cause", "Error cause", "Cause", false, true, String.class)),
        URL_REDIRECT(NVConfigManager.createNVConfig("url_redirect", "URL redirect", "URLRedirect", false, true, String.class)),
        RELOAD(NVConfigManager.createNVConfig("reload", "reload", "Reload", false, true, boolean.class)),
        EXCEPTION_CLASS_NAME(NVConfigManager.createNVConfig("exp_classname", "Exception class name", "ExceptionClassName", false, true, String.class)),


        ;

        private final NVConfig nvc;

        Params(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity API_ERROR = new NVConfigEntityPortable("api_error",
            null,
            "APIError",
            true,
            false,
            false,
            false,
            APIError.class,
            SharedUtil.extractNVConfigs(Params.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO);


    public APIError(String message) {
        this();
        setMessage(message);
    }

    public APIError(String message, String code) {
        this();
        setMessage(message);
        setCode(code);
    }

    public APIError(String message, String code, String cause) {
        this();
        setMessage(message);
        setCode(code);
        setCause(cause);
    }


    public APIError(Exception e) {
        this();
        setException(e);
    }

    public APIError(Exception e, String code) {
        this(e);
        setCode(code);
    }

    public APIError() {
        super(API_ERROR);
    }

    public String getCode() {
        return lookupValue(Params.CODE);
    }

    public void setCode(String code) {
        setValue(Params.CODE, code);
    }

    public String getMessage() {
        return lookupValue(Params.MESSAGE);
    }

    public void setMessage(String message) {
        setValue(Params.MESSAGE, message);
    }

    public String getCause() {
        return lookupValue(Params.CAUSE);
    }

    public void setCause(String cause) {
        setValue(Params.CAUSE, cause);
    }

    public String getExceptionClassName() {
        return lookupValue(Params.EXCEPTION_CLASS_NAME);
    }

    public void setExceptionClassName(String exceptionClassName) {
        setValue(Params.EXCEPTION_CLASS_NAME, exceptionClassName);
    }

    public void setException(Exception e) {
        setMessage(e.getMessage());
        setExceptionClassName(e.getClass().getName());
        if (e instanceof AccessException) {
            AccessException ae = (AccessException) e;
            setReloadRequired(ae.isReloadRequired());
            setURLRedirect(ae.getURLRedirect());
        }
    }

    public Throwable toException() {

        if (NullPointerException.class.getName().equals(getExceptionClassName())) {
            return new NullPointerException(getMessage());
        }

        if (IllegalArgumentException.class.getName().equals(getExceptionClassName())) {
            return new IllegalArgumentException(getMessage());
        }

        if (AccessException.class.getName().equals(getExceptionClassName())) {

            return new AccessException(getMessage(), getURLRedirect(), isReloadRequired());
        }


        if (APIException.class.getName().equals(getExceptionClassName())) {
            return new APIException(getMessage());
        }


        return new APIException(getMessage(), Category.GENERIC, Code.GENERIC);
    }

    /**
     * Get the url redirect
     *
     * @return the redirect url
     */
    public String getURLRedirect() {
        return lookupValue(Params.URL_REDIRECT);
    }


    /**
     * Set the redirect url optional
     *
     * @param urlRedirect
     */
    public void setURLRedirect(String urlRedirect) {
        setValue(Params.URL_REDIRECT, urlRedirect);
    }

    public boolean isReloadRequired() {
        return lookupValue(Params.RELOAD);
    }

    public void setReloadRequired(boolean reload) {
        setValue(Params.RELOAD, reload);
    }
}
