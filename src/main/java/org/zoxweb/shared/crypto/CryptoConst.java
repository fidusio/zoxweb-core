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
package org.zoxweb.shared.crypto;

import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.util.*;

public final class CryptoConst
{

	private CryptoConst()
    {

	}

	public enum HASHType
            implements GetName
    {
		MD5("md5"),
		SHA_1("sha-1"),
		SHA_224("sha-224"),
		SHA_256("sha-256"),
		SHA_384("sha-384"),
		SHA_512("sha-512"),

		BCRYPT("bcrypt")

        ;

		private final String name;

		HASHType(String name)
        {
			this.name = name;
		}
	
		@Override
		public String getName()
        {
			return name;
		}

		@Override
		public String toString()
        {
			return getName();
		}
		
		public static HASHType lookup(String mdName)
        {
			return (HASHType) SharedUtil.lookupEnum(mdName, HASHType.values());
		}
	}

	public enum DataMDType
        implements GetName
    {
		MD5_ENCRYPTED("MD5-ENCRYPTED"),
		MD5_ORIGINAL("MD5-ORIGINAL"),
		SHA_256_ENCRYPTED("SHA-256-ENCRYPTED"),
		SHA_256_ORIGINAL("SHA-256-ORIGINAL")

		;

		private final String name;

		DataMDType(String name)
        {
			this.name = name;
		}

		@Override
		public String getName()
        {
			return name;
		}
		
		public static HASHType toMDType(String name)
        {
			return toMDType((DataMDType)SharedUtil.lookupEnum(name, DataMDType.values()));
		}
		
		public static HASHType toMDType(DataMDType dmdt)
        {
			HASHType ret = null;
			switch(dmdt)
			{
			case MD5_ENCRYPTED:
				
			case MD5_ORIGINAL:
				ret = HASHType.MD5;
				break;
			case SHA_256_ENCRYPTED:
			
			case SHA_256_ORIGINAL:
				ret = HASHType.SHA_256;
				break;
			}
			
			return ret;
		}
	}

	public enum SignatureAlgo
			implements GetName
	{
		SHA1_DSA("SHA1withDSA"),
		SHA1_RSA("SHA1withRSA"),
		SHA256_RSA("SHA256withRSA"),
		SHA384_RSA("SHA384withRSA"),
		SHA512_RSA("SHA512withRSA"),
		SHA256_EC("SHA256withECDSA"),
		SHA384_EC("SHA384withECDSA"),
    	SHA512_EC("SHA512withECDSA"),
		
		;
		private final String name;
		SignatureAlgo(String name)
		{
			this.name = name;
		}

		public String getName()
		{
			return name;
		}
	}

	public enum SecureRandomType
        implements GetName
    {
		SECURE_RANDOM_VM_DEFAULT("DefaultVM"),
		SECURE_RANDOM_VM_STRONG("DefaultVMStrong"),
		NATIVE("NativePRNG"),
		SHA1PRNG("SHA1PRNG")

		;

		private final String name;

		SecureRandomType(String name)
        {
			this.name = name;
		}
	
		@Override
		public String getName()
        {
			return name;
		}

        @Override
		public String toString()
        {
			return getName();
		}
		
		public static SecureRandomType lookup(String mdName)
        {
			return (SecureRandomType) SharedUtil.lookupEnum(mdName, SecureRandomType.values());
		}
	}

	public enum AuthenticationType
	implements GetName
	{
		ALL("All"),
		BASIC("Basic"),
		BEARER("Bearer"),
		DIGEST("Digest"),
		DOMAIN("Domain"),
		JWT("JWT"),
		LDAP("LDAP"),
		HOBA("HOBA"),
		NONE("None"),
		OAUTH("OAuth"),
		SSWS("SSWS"),
		;

		private final String name;
		AuthenticationType(String val)
		{
			name = val;
		}

		@Override
		public String getName()
		{
			// TODO Auto-generated method stub
			return name;
		}
	}

	public enum OAuthParam
		implements GetName, GetNVConfig
	{
		ACCESS_TOKEN(NVConfigManager.createNVConfig("access_token", "Access token", "AccessToken", false, true, false, String.class, FilterType.ENCRYPT)),
		AUTHORIZATION(NVConfigManager.createNVConfig("authorization", "The Authorization", "Authorization", true, true, String.class)),
		AUTHORIZATION_CODE(NVConfigManager.createNVConfig("authorization_code", "The Authorization code", "AuthorizationCode", true, true, String.class)),
		BEARER(NVConfigManager.createNVConfig("bearer", "Bearer", "Bearer", true, true, String.class)),
		CLIENT_ID(NVConfigManager.createNVConfig("client_id", "OAUTH client identifier", "ClientID", true, true, String.class)),
		CLIENT_SECRET(NVConfigManager.createNVConfig("client_secret", "OAUTH client secret", "ClientSecret", true, true, String.class)),
		EXPIRES_IN(NVConfigManager.createNVConfig("expires_in", "Expiration time value", "ExpiresIn", false, true, Integer.class)),
		EXPIRATION_UNIT(NVConfigManager.createNVConfig("expiration_unit", "Expiration time unit", "ExpirationUnit", false, true, Const.TimeInMillis.class)),
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

	public static class SubjectIDFilter
		implements ValueFilter<String,String>
	{
		public static final SubjectIDFilter SINGLETON = new SubjectIDFilter();

		private SubjectIDFilter(){}

		/**
		 * Validate the object
		 *
		 * @param in value to be validated
		 * @return validated acceptable value
		 * @throws NullPointerException     if in is null
		 * @throws IllegalArgumentException if in is invalid
		 */
		@Override
		public String validate(String in) throws NullPointerException, IllegalArgumentException {
			return in;
		}

		/**
		 * Check if the value is valid
		 *
		 * @param in value to be checked
		 * @return true if valid false if not
		 */
		@Override
		public boolean isValid(String in) {
			return true;
		}

		/**
		 * Converts the implementing object in its canonical form.
		 *
		 * @return text identification of the object
		 */
		@Override
		public String toCanonicalID() {
			return "SUBJECT_ID_FILTER";
		}
	}
}