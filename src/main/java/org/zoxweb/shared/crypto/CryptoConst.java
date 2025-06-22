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

    /**
     * AES 256 bits key size in bytes(32)
     */
    public static final int AES_256_KEY_SIZE = 32;
	/**
	 * AES block size in bits 128 (16 bytes);
	 */
	public static final int AES_BLOCK_SIZE = 16;
	public static final String PEM_KEY_FILTER = "-----BEGIN [^-]+-----|-----END [^-]+-----|\\s+";

	public static final String PKCS12 = "PKCS12";;
	public static final String KEY_STORE_TYPE = "JCEKS";

	public enum PKInfo
		implements GetName, CanonicalID
	{
		EC_224(CryptoAlgo.EC, "secp224r1"),
		EC_256(CryptoAlgo.EC, "secp256r1"),
		EC_384(CryptoAlgo.EC, "secp384r1"),
		EC_521(CryptoAlgo.EC, "secp521r1"),
		RSA_2048(CryptoAlgo.RSA, "2048"),
		RSA_3072(CryptoAlgo.RSA, "3072"),
		RSA_4096(CryptoAlgo.RSA, "4096"),
		KYBER_512(CryptoAlgo.CRYSTALS_KYBER, "kyber512"),
		KYBER_768(CryptoAlgo.CRYSTALS_KYBER, "kyber768"),
		KYBER_1024(CryptoAlgo.CRYSTALS_KYBER, "kyber1024"),

		;


		private final String type;
		private final String name;
		PKInfo(CryptoAlgo cryptoAlgo, String name)
		{
			this.type = cryptoAlgo.getName();
			this.name = name;
		}
		PKInfo(String type, String name)
		{
			this.type = type.toUpperCase();
			this.name = name;
		}

		/**
		 * @return the name of the object
		 */
		@Override
		public String getName()
		{
			return name;
		}

		public String getType()
		{
			return type;
		}

		public String toCanonicalID()
		{
			return SharedUtil.toCanonicalID(':', type, name);
		}

		public static PKInfo parse(String keyCanonicalID)
		{
			for(PKInfo pkInfo : PKInfo.values())
			{
				if(pkInfo.getName().equalsIgnoreCase(keyCanonicalID) ||
						pkInfo.name().equalsIgnoreCase(keyCanonicalID) ||
						pkInfo.toCanonicalID().equalsIgnoreCase(keyCanonicalID))
					return pkInfo;
			}

			String[] parsed = keyCanonicalID.split("[ ,:]");
			if (parsed.length != 2)
			{
				throw new IllegalArgumentException("invalid key " + keyCanonicalID + " ie:  rsa 2048 or ec:secp256r1 or use , as separator");
			}
			String keyType = parsed[0];
			String keyName = parsed[1];

			for(PKInfo pkInfo : PKInfo.values())
			{
				if(pkInfo.getType().equalsIgnoreCase(keyType) && pkInfo.getName().equalsIgnoreCase(keyName))
					return pkInfo;
			}

			return null;
		}
	}



	public static final String AES_ENCRYPTION_CBC_NO_PADDING = "AES/CBC/NoPadding";


	public enum CryptoAlgo
		implements GetName
	{
		AES("AES"),
		DSA("DSA"),
		EC("EC"),
		RSA("RSA"),
		CRYSTALS_KYBER("Kyber"),
		CRYSTALS("CRYSTALS"),
		;


		private final String javaName;
		CryptoAlgo(String javaName)
		{
			this.javaName = javaName;
		}

		@Override
		public String getName() {
			return javaName;
		}

		public String toString()
		{
			return getName();
		}
	}

	public enum HashProperty
		implements GetName
	{
		ITERATIONS("iterations"),
		LOG("log"),
		MEMORY("memory"),
		PARALLELISM("parallelism"),
		ROUNDS("rounds")



		;
		private final String name;
		HashProperty(String name)
		{
			this.name = name;
		}

		public String getName()
		{
			return name;
		}
	}




	public enum HashType
            implements GetName
    {
		MD5("MD5"),
		SHA_1("SHA_1"),
		SHA_224("SHA-224"),
		SHA_256("SHA-256"),
		SHA_384("SHA-384"),
		SHA_512("SHA-512"),

		BCRYPT("BCRYPT"),
		ARGON2("ARGON2"),
		PBKDF2("PBKDF2"),
        ;

		private final String name;

		HashType(String name)
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
		
		public static HashType lookup(String mdName)
        {
			return (HashType) SharedUtil.lookupEnum(mdName, HashType.values());
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
		
		public static HashType toMDType(String name)
        {
			return toMDType((DataMDType)SharedUtil.lookupEnum(name, DataMDType.values()));
		}
		
		public static HashType toMDType(DataMDType dmdt)
        {
			HashType ret = null;
			switch(dmdt)
			{
			case MD5_ENCRYPTED:
				
			case MD5_ORIGINAL:
				ret = HashType.MD5;
				break;
			case SHA_256_ENCRYPTED:
			
			case SHA_256_ORIGINAL:
				ret = HashType.SHA_256;
				break;
			}
			
			return ret;
		}
	}

	public enum SignatureAlgo
			implements GetName
	{

		HMAC_SHA_256(CryptoAlgo.AES, "HmacSHA256"),
		HMAC_SHA_384(CryptoAlgo.AES,"HmacSHA384"),
		HMAC_SHA_512(CryptoAlgo.AES,"HmacSHA512"),
		SHA1_DSA(CryptoAlgo.DSA, "SHA1withDSA"),
		SHA1_RSA(CryptoAlgo.RSA,"SHA1withRSA"),
		SHA256_RSA(CryptoAlgo.RSA,"SHA256withRSA"),
		SHA384_RSA(CryptoAlgo.RSA,"SHA384withRSA"),
		SHA512_RSA(CryptoAlgo.RSA,"SHA512withRSA"),
		SHA256_EC(CryptoAlgo.EC,"SHA256withECDSA"),
		SHA384_EC(CryptoAlgo.EC,"SHA384withECDSA"),
    	SHA512_EC(CryptoAlgo.EC,"SHA512withECDSA"),
		CRYSTALS_DILITHIUM(CryptoAlgo.CRYSTALS, "Dilithium")
		
		;
		private final String name;
		private final CryptoAlgo cryptoAlgo;
		SignatureAlgo(CryptoAlgo cryptoAlgo, String name)
		{
			this.cryptoAlgo = cryptoAlgo;
			this.name = name;
		}

		public String getName()
		{
			return name;
		}

		public CryptoAlgo getCryptoAlgo()
		{
			return cryptoAlgo;
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
			return SharedUtil.lookupEnum(mdName, SecureRandomType.values());
		}
	}

	public enum AuthenticationType
	implements GetName
	{
		ALL("All"),
		API_KEY("ApiKey"), // custom authentication like opaque key SSWS etc
		BASIC("Basic"),
		BEARER("Bearer"),
		DIGEST("Digest"),
		DOMAIN("Domain"),
		JWT("JWT"),
		LDAP("LDAP"),
		HOBA("HOBA"),
		NONE("None"),
		OAUTH("OAuth"),
		//SSWS("SSWS"),
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

		final private NVConfig nvConfig;

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

	public enum JWTAlgo
		implements GetName
	{

		none(null, null),
		HS256(SignatureAlgo.HMAC_SHA_256, null),
		HS384(SignatureAlgo.HMAC_SHA_384, null),
		HS512(SignatureAlgo.HMAC_SHA_512, null),
		RS256(SignatureAlgo.SHA256_RSA, null),
		RS384(SignatureAlgo.SHA384_RSA, null),
		RS512(SignatureAlgo.SHA512_RSA, null),
		ES256(SignatureAlgo.SHA256_EC,  "secp256r1", "P-256"),
		ES384(SignatureAlgo.SHA384_EC, "secp256r1", "P-256"),
		ES512(SignatureAlgo.SHA512_EC, "secp521r1", "P-521"),
		;

		private final SignatureAlgo signatureAlgo;
		private final String paramSpec;
		private final String altName;

		JWTAlgo(SignatureAlgo signatureAlgo, String paramSpec)
		{
			this(signatureAlgo, paramSpec, null);
		}

		JWTAlgo(SignatureAlgo signatureAlgo, String paramSpec, String altName)
		{
			this.signatureAlgo = signatureAlgo;
			this.paramSpec = paramSpec;
			this.altName = altName;
		}



		public String getName()
		{
			return name();
		}
		public SignatureAlgo getSignatureAlgo()
		{
			return signatureAlgo;
		}

		public String getAltName()
		{
			return altName;
		}

		public String getParamSpec()
		{
			return paramSpec;
		}


		public static JWTAlgo lookup(String algo)
		{
			JWTAlgo ret = SharedUtil.lookupEnum(algo, JWTAlgo.values());
			if (ret == null && algo != null)
			{
				for (JWTAlgo jwta : JWTAlgo.values())
				{
					if (algo.equalsIgnoreCase(jwta.getParamSpec()) || algo.equalsIgnoreCase(jwta.getAltName()))
					{
						ret = jwta;
						break;
					}
				}
			}
			return ret;
		}

	}

	/**
	 * This enum contains user status with a specified status
	 * expiration time.
	 */
	public enum SubjectStatus
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

		SubjectStatus(long time)
	    {
			EXPIRATION_TIME = time;
		}

		@Override
		public Long getValue()
	    {
			return EXPIRATION_TIME;
		}
	}

	public static String applyPemFilters(String pemToken)
	{
		/* Regex pieces the split() will REMOVE:
		 *   1)  -----BEGIN …-----      (header)
		 *   2)  -----END …-----        (footer)
		 *   3)  all whitespace (line breaks, spaces, tabs)
		 */
		String[] chunks = pemToken.split(PEM_KEY_FILTER);

		// concatenate the non-empty parts: that’s the pure Base-64 payload
		StringBuilder sb = new StringBuilder();
		for (String c : chunks) {
			if (!c.isEmpty()) sb.append(c);
		}
		pemToken = sb.toString();   // <- final result


		return pemToken;
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
		 * @return true if valid false  not
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