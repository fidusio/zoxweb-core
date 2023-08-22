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
package org.zoxweb.shared.http;

import org.zoxweb.shared.security.JWT;
import org.zoxweb.shared.security.JWTEncoder;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 * @author mnael
 *
 */
@SuppressWarnings("serial")
public class HTTPAuthorizationJWTBearer
extends HTTPAuthorization
{
	
	
	public static final NVConfig NVC_KEY = NVConfigManager.createNVConfig("key", null,"Token", false, true, byte[].class);
	public static final NVConfig NVC_JWT = NVConfigManager.createNVConfigEntity("jwt", "jwt token", "JWT", false, true, JWT.class, ArrayType.NOT_ARRAY);
	
	public static final NVConfigEntity NVC_HTTP_AUTHORIZATION_JWT_BEARER = new NVConfigEntityLocal("http_authorization_jwt_bearer", null , null, true, false, false, false, HTTPAuthorizationJWTBearer.class, SharedUtil.toNVConfigList(NVC_KEY, NVC_JWT), null, false, HTTPAuthorization.NVC_HTTP_AUTHORIZATION);
	
	private transient JWTEncoder jwtEncoder = null;
	
	
	public HTTPAuthorizationJWTBearer()
	{
		super(NVC_HTTP_AUTHORIZATION_JWT_BEARER, HTTPAuthScheme.BEARER);
	}
	
	public HTTPAuthorizationJWTBearer(JWTEncoder encoder, byte[] key, JWT jwt)
	{
		this();
		setJWTEncoder(encoder);
		setKey(key);
		setJWT(jwt);
		
	}
	
	public byte[] getKey() 
	{
		return lookupValue(NVC_KEY);
	}
	
	public void setKey(byte[] key) 
	{
		setValue(NVC_KEY, key);
	}
	
//	public String toString()
//	{
//		return SharedUtil.toCanonicalID(' ', getType(), getToken());
//	}
	
	public JWTEncoder getJWTEncoder() {
		return jwtEncoder;
	}

	public void setJWTEncoder(JWTEncoder jwtEncoder) {
		this.jwtEncoder = jwtEncoder;
	}
	
	public JWT getJWT()
	{
		return lookupValue(NVC_JWT);
	}
	
	public void setJWT(JWT jwt) 
	{
		setValue(NVC_JWT, jwt);
	}
	
	

	public GetNameValue<String> toHTTPHeader()
	{
		return getAuthScheme().toHTTPHeader(jwtEncoder.encode(getKey(), getJWT()));
	}
	
}
