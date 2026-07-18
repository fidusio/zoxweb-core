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
package org.zoxweb.shared.http;

import org.zoxweb.shared.data.SetNameDAO;
import org.zoxweb.shared.util.*;

/**
 * Data Access Object representing HTTP authorization credentials.
 * This class stores authentication scheme information and tokens used for HTTP Authorization headers.
 * <p>
 * Supports various authentication schemes including Basic, Bearer, and custom/generic schemes.
 * Can be serialized and converted to HTTP Authorization header format.
 * </p>
 *
 * @author mnael
 * @see HTTPAuthScheme
 */
@SuppressWarnings("serial")
public class HTTPAuthorization
        extends SetNameDAO {


    public static final NVConfig NVC_AUTH_SCHEME = NVConfigManager.createNVConfig("auth_scheme", null, "HTTPAuthScheme", false, true, String.class);
    public static final NVConfig NVC_TOKEN = NVConfigManager.createNVConfig("token", null, "Token", false, true, NamedValue.class);
    //public static final NVConfig NVC_TYPE_IS_HEADER = NVConfigManager.createNVConfig("type_is_header", null, "TypeIsHeader", false, true, boolean.class);
    public static final NVConfigEntity NVC_HTTP_AUTHORIZATION = new NVConfigEntityPortable("http_authorization", null, null, true, false, false, false, HTTPAuthorization.class, SharedUtil.toNVConfigList(NVC_AUTH_SCHEME, NVC_TOKEN), null, false, SetNameDAO.NVC_NAME_DAO);


    /**
     * Default constructor creating an empty HTTPAuthorization.
     */
    public HTTPAuthorization() {
        super(NVC_HTTP_AUTHORIZATION);
    }

    /**
     * Constructs an HTTPAuthorization with the specified authentication scheme.
     *
     * @param httpAuthScheme the HTTP authentication scheme (Basic, Bearer, etc.)
     */
    public HTTPAuthorization(String httpAuthScheme) {
        this();
        setAuthScheme(httpAuthScheme);
    }


    public static HTTPAuthorization createBasic(String user, String password) {
        HTTPAuthorization ret = new HTTPAuthorization(HTTPAuthScheme.BASIC.getName());
        ret.setName(HTTPHeader.AUTHORIZATION.getName());
        ret.setToken(new String(SharedBase64.encode(SharedStringUtil.getBytes(SUS.toCanonicalID(':', user, password)))));
        ret.getTokenProperties().build("user", user).build("password", password);
        return ret;
    }

    public static HTTPAuthorization createBearer(String token) {
        HTTPAuthorization ret = new HTTPAuthorization(HTTPAuthScheme.BEARER.getName());
        ret.setName(HTTPHeader.AUTHORIZATION.getName());
        ret.setToken(token);
        return ret;
    }

    public static HTTPAuthorization createAuthorization(String authScheme, String token) {
        HTTPAuthorization ret = new HTTPAuthorization(authScheme);
        ret.setName(HTTPHeader.AUTHORIZATION.getName());
        ret.setToken(token);
        return ret;
    }


    public static HTTPAuthorization createGeneric(String authzName, String authScheme, String token) {
        HTTPAuthorization ret = new HTTPAuthorization(authScheme);
        ret.setName(authzName);
        ret.setToken(token);
        return ret;
    }

    public static HTTPAuthorization parse(GetNameValue<String> header) {



        String schemePlusToken = header.getValue().trim();
        String scheme;
        String tokenVal;
        int index = schemePlusToken.indexOf(" ");
        if (index == -1) {
            // No scheme prefix (e.g. a bare API-key header value): treat the whole value as the token.
            scheme = null;
            tokenVal = schemePlusToken;
        } else {
            scheme = schemePlusToken.substring(0, index);
            tokenVal = schemePlusToken.substring(index).trim();
        }
        if (SUS.isEmpty(tokenVal))
            throw new IllegalArgumentException("Empty token value: " + tokenVal);

        HTTPAuthorization ret = new HTTPAuthorization(scheme);
        ret.setName(header.getName());

        ret.setToken(tokenVal);

        String[] pairs = tokenVal.split(",\\s*");
        NamedValue<?> token = ret.lookup(NVC_TOKEN);
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            String[] nv = pair.split("=", 2);
            String key = nv[0].trim();
            if (nv.length > 1) {
                String val = nv[1].trim();
                if (val.startsWith("\"") && val.endsWith("\"")) {
                    val = val.substring(1, val.length() - 1);
                }
                token.getProperties().build(key, val);
            }
        }
        return ret;
    }


    /**
     * Returns the authentication type name.
     *
     * @return the type name (e.g., "Basic", "Bearer")
     */
    public String getType() {
        return getName();
    }


    /**
     * Protected constructor for subclasses.
     *
     * @param nvce the NVConfigEntity for the subclass
     * @param type the authentication scheme
     */
    protected HTTPAuthorization(NVConfigEntity nvce, HTTPAuthScheme type) {
        super(nvce);
        setAuthScheme(type);
    }


    /**
     * @return the token
     */
    public String getToken() {
        return (String) lookup(NVC_TOKEN).getValue();
    }


    /**
     * @param token the token to set
     */
    public void setToken(String token) {
        setValue(NVC_TOKEN, token);
    }

    public NVGenericMap getTokenProperties() {
        return ((NamedValue<String>) lookup(NVC_TOKEN)).getProperties();
    }

    public HTTPAuthScheme authSchemeAsEnum() {
        return SharedUtil.lookupEnum(getAuthScheme(), HTTPAuthScheme.BASIC, HTTPAuthScheme.BEARER);
    }


    /**
     * Returns the authentication scheme.
     *
     * @return the HTTP authentication scheme
     */
    public String getAuthScheme() {
        return lookupValue(NVC_AUTH_SCHEME);
    }

    /**
     * Converts this authorization to an HTTP Authorization header name-value pair.
     *
     * @return the Authorization header as a name-value pair
     */
    public GetNameValue<String> toHTTPHeader() {
        return GetNameValue.create(getName(), getAuthScheme() != null ? getAuthScheme() + " " + getToken() : getToken());
    }


    /**
     * Sets the authentication scheme.
     *
     * @param type the authentication scheme to set
     */
    protected void setAuthScheme(HTTPAuthScheme type) {
        setValue(NVC_AUTH_SCHEME, type != null ? type.getName() : null);
    }

    public void setAuthScheme(String authScheme) {
        setValue(NVC_AUTH_SCHEME, authScheme);
    }

    /**
     * Returns the string representation of this authorization in HTTP header format.
     *
     * @return the Authorization header value (e.g., "Bearer token123")
     */
    public String toString() {
        return toHTTPHeader().getValue();
    }


}
