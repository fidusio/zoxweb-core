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
 * @see HTTPAuthorizationBasic
 * @see HTTPAuthorizationJWTBearer
 */
@SuppressWarnings("serial")
public class HTTPAuthorization
        extends SetNameDAO {


    public static final NVConfig NVC_AUTH_SCHEME = NVConfigManager.createNVConfig("auth_scheme", null, "HTTPAuthScheme", false, true, HTTPAuthScheme.class);
    public static final NVConfig NVC_TOKEN = NVConfigManager.createNVConfig("token", null, "Token", false, true, NamedValue.class);
    public static final NVConfig NVC_TYPE_IS_HEADER = NVConfigManager.createNVConfig("name_is_header", null, "TypeIsHeader", false, true, boolean.class);
    public static final NVConfigEntity NVC_HTTP_AUTHORIZATION = new NVConfigEntityPortable("http_authorization", null, null, true, false, false, false, HTTPAuthorization.class, SharedUtil.toNVConfigList(NVC_AUTH_SCHEME, NVC_TOKEN, NVC_TYPE_IS_HEADER), null, false, SetNameDAO.NVC_NAME_DAO);


    /**
     * Default constructor creating an empty HTTPAuthorization.
     */
    public HTTPAuthorization() {
        super(NVC_HTTP_AUTHORIZATION);
    }

    /**
     * Constructs an HTTPAuthorization with the specified authentication scheme.
     *
     * @param type the HTTP authentication scheme (Basic, Bearer, etc.)
     */
    public HTTPAuthorization(HTTPAuthScheme type) {
        this();
        setAuthScheme(type);
    }

    /**
     * Constructs an HTTPAuthorization with a custom type and token.
     *
     * @param type  the custom authentication type name
     * @param token the authentication token
     */
    public HTTPAuthorization(String type, String token) {
        this(type, token, false);
    }

    /**
     * Constructs an HTTPAuthorization with a custom type, token, and header flag.
     *
     * @param type         the custom authentication type name
     * @param token        the authentication token
     * @param typeIsHeader if true, the type is used as the header name
     */
    public HTTPAuthorization(String type, String token, boolean typeIsHeader) {
        this(HTTPAuthScheme.GENERIC);
        setName(type);
        setToken(token);
        setValue(NVC_TYPE_IS_HEADER, typeIsHeader);

    }

    /**
     * Constructs an HTTPAuthorization by parsing a combined type and token string.
     * The string should be in format "scheme token" (e.g., "Bearer abc123").
     *
     * @param typePlusToken the combined authentication string
     * @throws IllegalArgumentException if the format is invalid
     */
    public HTTPAuthorization(String typePlusToken) {
        this(HTTPAuthScheme.GENERIC);


        typePlusToken = typePlusToken.trim();
        String scheme;
        int index = typePlusToken.indexOf(" ");
        if (index == -1)
            throw new IllegalArgumentException("Invalid token: " + typePlusToken);

        scheme = typePlusToken.substring(0, index);

        String tokenVal = typePlusToken.substring(index).trim();
        if (SUS.isEmpty(tokenVal))
            throw new IllegalArgumentException("Empty token value: " + tokenVal);


        setName(scheme);
        setToken(tokenVal);

        String[] pairs = tokenVal.split(",\\s*");
        NamedValue<?> token = lookup(NVC_TOKEN);
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
     * Constructs an HTTPAuthorization with the specified scheme and token.
     *
     * @param type  the HTTP authentication scheme
     * @param token the authentication token
     */
    public HTTPAuthorization(HTTPAuthScheme type, String token) {
        this(type);
        setToken(token);
    }

    /**
     * Returns whether the type is used as the header name.
     *
     * @return true if the type should be used as the header name
     */
    public boolean isTypeHeader() {
        return lookupValue(NVC_TYPE_IS_HEADER);
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
     * Returns the token as a NamedValue for generic authentication handling.
     *
     * @return the token with its associated properties
     */
    public NamedValue<String> getGenericToken() {
        return lookup(NVC_TOKEN);
    }

    /**
     * @param token the token to set
     */
    public void setToken(String token) {
        NVBase<String> nvb = lookup(NVC_TOKEN);
        nvb.setValue(token);
    }


    /**
     * Returns the authentication scheme.
     *
     * @return the HTTP authentication scheme
     */
    public HTTPAuthScheme getAuthScheme() {
        return lookupValue(NVC_AUTH_SCHEME);
    }

    /**
     * Converts this authorization to an HTTP Authorization header name-value pair.
     *
     * @return the Authorization header as a name-value pair
     */
    public GetNameValue<String> toHTTPHeader() {
        NVPair ret;
        if (isTypeHeader()) {
            ret = (NVPair) getAuthScheme().toHTTPHeader(getToken());
            ret.setName(getName());
        } else
            ret = (NVPair) getAuthScheme().toHTTPHeader(getName(), getToken());

        return ret;
    }


    /**
     * Sets the authentication scheme.
     *
     * @param type the authentication scheme to set
     */
    protected void setAuthScheme(HTTPAuthScheme type) {
        setValue(NVC_AUTH_SCHEME, type);
        if (type != HTTPAuthScheme.GENERIC)
            setName(type.getName());
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
