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

import org.zoxweb.shared.security.JWT;
import org.zoxweb.shared.security.JWTEncoder;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 * HTTP Bearer Authentication credentials container for JWT (JSON Web Token) authentication.
 * Stores the JWT token, signing key, and JWT encoder used to generate Bearer authorization headers.
 * <p>
 * The token is encoded using the provided JWTEncoder and key when converted to an HTTP
 * Authorization header in the format: "Bearer encoded_jwt_token".
 * </p>
 *
 * @author mnael
 * @see HTTPAuthorization
 * @see HTTPAuthScheme#BEARER
 * @see org.zoxweb.shared.security.JWT
 */
@SuppressWarnings("serial")
public class HTTPAuthorizationJWTBearer
extends HTTPAuthorization
{
	
	
	public static final NVConfig NVC_KEY = NVConfigManager.createNVConfig("key", null,"Token", false, true, byte[].class);
	public static final NVConfig NVC_JWT = NVConfigManager.createNVConfigEntity("jwt", "jwt token", "JWT", false, true, JWT.class, ArrayType.NOT_ARRAY);
	
	public static final NVConfigEntity NVC_HTTP_AUTHORIZATION_JWT_BEARER = new NVConfigEntityPortable("http_authorization_jwt_bearer", null , null, true, false, false, false, HTTPAuthorizationJWTBearer.class, SharedUtil.toNVConfigList(NVC_KEY, NVC_JWT), null, false, HTTPAuthorization.NVC_HTTP_AUTHORIZATION);
	
	private transient JWTEncoder jwtEncoder = null;
	
	
	/**
	 * Default constructor creating an empty JWT Bearer authorization.
	 */
	public HTTPAuthorizationJWTBearer()
	{
		super(NVC_HTTP_AUTHORIZATION_JWT_BEARER, HTTPAuthScheme.BEARER);
	}

	/**
	 * Constructs an HTTPAuthorizationJWTBearer with the specified encoder, key, and JWT.
	 *
	 * @param encoder the JWT encoder used to sign and encode the token
	 * @param key     the signing key for the JWT
	 * @param jwt     the JWT token to be encoded
	 */
	public HTTPAuthorizationJWTBearer(JWTEncoder encoder, byte[] key, JWT jwt)
	{
		this();
		setJWTEncoder(encoder);
		setKey(key);
		setJWT(jwt);

	}

	/**
	 * Returns the signing key for the JWT.
	 *
	 * @return the signing key as a byte array
	 */
	public byte[] getKey()
	{
		return lookupValue(NVC_KEY);
	}

	/**
	 * Sets the signing key for the JWT.
	 *
	 * @param key the signing key as a byte array
	 */
	public void setKey(byte[] key)
	{
		setValue(NVC_KEY, key);
	}
	
	/**
	 * Returns the JWT encoder used to sign and encode the token.
	 *
	 * @return the JWT encoder
	 */
	public JWTEncoder getJWTEncoder() {
		return jwtEncoder;
	}

	/**
	 * Sets the JWT encoder used to sign and encode the token.
	 *
	 * @param jwtEncoder the JWT encoder to set
	 */
	public void setJWTEncoder(JWTEncoder jwtEncoder) {
		this.jwtEncoder = jwtEncoder;
	}

	/**
	 * Returns the JWT token.
	 *
	 * @return the JWT token
	 */
	public JWT getJWT()
	{
		return lookupValue(NVC_JWT);
	}

	/**
	 * Sets the JWT token.
	 *
	 * @param jwt the JWT token to set
	 */
	public void setJWT(JWT jwt)
	{
		setValue(NVC_JWT, jwt);
	}

	/**
	 * Converts this JWT Bearer authorization to an HTTP Authorization header.
	 * The result is in the format: "Bearer encoded_jwt_token".
	 *
	 * @return the Authorization header as a name-value pair
	 */
	public GetNameValue<String> toHTTPHeader()
	{
		return getAuthScheme().toHTTPHeader(jwtEncoder.encode(getKey(), getJWT()));
	}
	
}
