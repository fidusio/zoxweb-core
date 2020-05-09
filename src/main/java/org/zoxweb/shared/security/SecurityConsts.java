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
package org.zoxweb.shared.security;

import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.Const.TimeInMillis;

/**
 * Security constants.
 */
public final class SecurityConsts
{
	/**
	 * The constructor is declared private to prevent instantiation.
	 */
	private SecurityConsts()
	{

	}


	public enum AuthenticationType
	{
		ALL,
		BASIC,
		TOKEN,
		JWT,
		LDAP,
		DOMAIN,
		OATH,
		NONE
	}
	
	
//	public enum AuthType
//	implements GetName
//	{
//		BASIC("Basic"),
//		BEARER("Bearer"),
//		DIGEST("Digest"),
//		HOBA("HOBA"),
//		OAUTH("OAuth"),
//		NONE("None");
//
//		private final String name;
//		AuthType(String val)
//		{
//			name = val;
//		}
//
//		@Override
//		public String getName()
//		{
//			// TODO Auto-generated method stub
//			return name;
//		}
//	}
	
	public enum OAuthParam
		implements GetName,GetNVConfig
	{
		ACCESS_TOKEN(NVConfigManager.createNVConfig("access_token", "Access token", "AccessToken", false, true, false, String.class, FilterType.ENCRYPT)),
		AUTHORIZATION(NVConfigManager.createNVConfig("authorization", "The Authorization", "Authorization", true, true, String.class)),
		AUTHORIZATION_CODE(NVConfigManager.createNVConfig("authorization_code", "The Authorization code", "AuthorizationCode", true, true, String.class)),
		BEARER(NVConfigManager.createNVConfig("bearer", "Bearer", "Bearer", true, true, String.class)),
		CLIENT_ID(NVConfigManager.createNVConfig("client_id", "OAUTH client identifier", "ClientID", true, true, String.class)),
		CLIENT_SECRET(NVConfigManager.createNVConfig("client_secret", "OAUTH client secret", "ClientSecret", true, true, String.class)),
		EXPIRES_IN(NVConfigManager.createNVConfig("expires_in", "Expiration time value", "ExpiresIn", false, true, Integer.class)),
		EXPIRATION_UNIT(NVConfigManager.createNVConfig("expiration_unit", "Expiration time unit", "ExpirationUnit", false, true, TimeInMillis.class)),
		CODE(NVConfigManager.createNVConfig("code", "The code", "Code", true, true, String.class)),
		GRANT_TYPE(NVConfigManager.createNVConfig("grant_type", "The grant type", "GrantType", true, true, String.class)),
		REFRESH_TOKEN(NVConfigManager.createNVConfig("refresh_token", "Refresh token", "RefreshToken", false, true, false, String.class, FilterType.ENCRYPT)),
		TOKEN_TYPE(NVConfigManager.createNVConfig("token_type", "Token type", "TokenType", false, true, String.class))

        ;
		
		private NVConfig nvConfig;
		
		OAuthParam(NVConfig config)
		{
			nvConfig = config;
		}
		
		
		/* (non-Javadoc)
		 * @see org.zoxweb.shared.util.GetName#getName()
		 */
		@Override
		public String getName()
		{
			return getNVConfig().getName();
		}

		@Override
		public String toString()
		{
			return getName();
		}

		/* (non-Javadoc)
		 * @see org.zoxweb.shared.util.GetNVConfig#getNVConfig()
		 */
		@Override
		public NVConfig getNVConfig()
		{
			return nvConfig;
		}
	}

	public enum SystemURI
	    implements GetValue<String>
	{
		REGISTER(HTTPMethod.POST, "register"),
		DEREGISTER(HTTPMethod.POST, "deregister"),
		VALIDATE_ACCESS_CODE(HTTPMethod.POST, "validate-access-code"),
		GENERATE_ACCESS_CODE(HTTPMethod.POST, "generate-access-code"),
		;

		private final HTTPMethod method;
		private final String value;

		SystemURI(HTTPMethod method, String value)
		{
			this.method = method;
			this.value = value;
		}

		/* (non-Javadoc)
		 * @see org.zoxweb.shared.util.GetValue#getValue()
		 */
		@Override
		public String getValue()
		{
			return value;
		}
		
		public final HTTPMethod getHTTPMethod()
		{
			return method;
		}

	}
	
	public enum JWTAlgorithm
		implements GetName
	{
		none("none"),
		HS256("HS256"),
		HS512("HS512"),
		RS256("RS256"),
		RS512("RS512"),
		ES256("ES256"),
        ES512("ES512"),
		;
		
		private final String name;
		
		JWTAlgorithm(String name)
		{
			this.name = name;
		}
		
		public String toString()
		{
			return getName();
		}
		
		
		public String getName()
		{
			return name;
		}
	}



	/**
	 * This enum contains user status with a specified status
	 * expiration time.
	 */
	public enum UserStatus
	    implements GetValue<Long>
	{
		// Note: 
		//	0 = no expiration time
		// -1 = expiration time is irrelevant
		ACTIVE(0),
		DEACTIVATED(0),
		INACTIVE(-1),
		PENDING_RESET_PASSWORD(Const.TimeInMillis.DAY.MILLIS * 2),
		PENDING_ACCOUNT_ACTIVATION(Const.TimeInMillis.DAY.MILLIS * 2)		
		
		;
	
		private final long EXPIRATION_TIME;
		
		UserStatus(long time)
	    {
			EXPIRATION_TIME = time;
		}
	
		@Override
		public Long getValue()
	    {
			return EXPIRATION_TIME;
		}
	}

}