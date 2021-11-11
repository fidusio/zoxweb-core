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

import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.SharedUtil;

public final class CryptoConst
{

	private CryptoConst()
    {

	}

	public enum MDType
            implements GetName
    {
		MD5("md5"),
		SHA_1("sha-1"),
		SHA_224("sha-224"),
		SHA_256("sha-256"),
		SHA_384("sha-384"),
		SHA_512("sha-512")

        ;

		private final String name;

		MDType(String name)
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
		
		public static MDType lookup(String mdName)
        {
			return (MDType) SharedUtil.lookupEnum(mdName, MDType.values());
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
		
		public static MDType toMDType(String name)
        {
			return toMDType((DataMDType)SharedUtil.lookupEnum(name, DataMDType.values()));
		}
		
		public static MDType toMDType(DataMDType dmdt)
        {
			MDType ret = null;
			switch(dmdt)
			{
			case MD5_ENCRYPTED:
				
			case MD5_ORIGINAL:
				ret = MDType.MD5;
				break;
			case SHA_256_ENCRYPTED:
			
			case SHA_256_ORIGINAL:
				ret = MDType.SHA_256;
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

}