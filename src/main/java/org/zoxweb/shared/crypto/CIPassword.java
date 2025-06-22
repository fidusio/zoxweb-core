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

import org.zoxweb.shared.crypto.CryptoConst.HashType;
import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.security.CredentialInfo;
import org.zoxweb.shared.util.*;

/**
 * PasswordDAO
 * @author mnael
 *
 */
@SuppressWarnings("serial")
public class CIPassword
    extends PropertyDAO
    implements CryptoBase, CredentialInfo
{


	private enum Param
        implements GetNVConfig
    {
		//HASH_ITERATION(NVConfigManager.createNVConfig("hash_iteration", "Hash iteration", "HashIteration", false, true, Integer.class)),
		SALT(NVConfigManager.createNVConfig("salt", "The password salt", "Salt", false, true, byte[].class)),
		HASH(NVConfigManager.createNVConfig("hash", "The password hash", "Hash", false, true, byte[].class)),

        ;

		private final NVConfig nvc;

        Param(NVConfig nvc)
        {
			this.nvc = nvc;
		}
		
		@Override
		public NVConfig getNVConfig()
        {
			return nvc;
		}	
	}

	public final static NVConfigEntity NVCE_CI_PASSWORD = new NVConfigEntityLocal("ci_password", null, "Password", false, true, false, false, CIPassword.class, SharedUtil.extractNVConfigs(Param.values()), null, false, PropertyDAO.NVC_PROPERTY_DAO);

    /**
     * The default constructor.
     */
    public CIPassword()
    {
		super(NVCE_CI_PASSWORD);
	}

	public synchronized void setName(HashType mdt)
    {
		SUS.checkIfNulls("Null Message Digest", mdt);
		
		setName(mdt.getName());
	}

	public synchronized void setName(String name)
    {
		SUS.checkIfNulls("Null Message Digest", name);

		HashType mdt = HashType.lookup(name);

		if (mdt == null)
		{
			throw new IllegalArgumentException("Unsupported Message Digest:" + name);
		}
		
		super.setName(mdt.getName());
	}

	public int getHashIterations()
    {
		NVInt iterations =  getProperties().getNV(CryptoConst.HashProperty.ITERATIONS);
		if(iterations != null)
			return iterations.getValue();

		return -1;
	}

	public synchronized void setHashIterations(int iterations)
    {
		if (iterations < 0)
		{
			throw new IllegalArgumentException("Invalid iteration value:" + iterations);
		}

		getProperties().build(new NVInt(CryptoConst.HashProperty.ITERATIONS, iterations));
	}

	public synchronized byte[] getSalt()
    {
		return lookupValue(Param.SALT);
	}

	public synchronized void setSalt(byte[] salt)
    {
		setValue( Param.SALT, salt);
	}

	public synchronized byte[] getPasswordHash()
    {
		return lookupValue(Param.HASH);
	}

	public synchronized void setPasswordHash(byte[] password)
    {
		setValue(Param.HASH, password);
	}

	public void setPasswordHash(String password)
	{
		setPasswordHash(SharedStringUtil.getBytes(password));
	}

	@Override
	public String toCanonicalID()
    {
		if(getCanonicalID() != null)
			return getCanonicalID();
		return SharedUtil.toCanonicalID(':', getName(), getHashIterations(), SharedStringUtil.bytesToHex(getSalt()), SharedStringUtil.bytesToHex( getPasswordHash()));
	}

	public static CIPassword fromCanonicalID(String passwordCanonicalID)
		throws NullPointerException, IllegalArgumentException {
		if (SUS.isEmpty(passwordCanonicalID)) {
			throw new NullPointerException("Empty password");
		}


		try
		{
			// special case to process BCrypt
			BCryptHash bCryptHash = new BCryptHash(passwordCanonicalID);
			CIPassword ret = new CIPassword();
			ret.setSalt(SharedStringUtil.getBytes(bCryptHash.salt));
			ret.setPasswordHash(bCryptHash.hash);
			ret.setHashIterations(bCryptHash.logRound);
			ret.setCanonicalID(bCryptHash.toCanonicalID());
			ret.setName(HashType.BCRYPT);
			return ret;
		}
		catch (Exception e)
		{

		}
		
		String[] tokens = passwordCanonicalID.split(":");
		CIPassword ret = new CIPassword();
		
		switch(tokens.length)
		{
		case 3:
			ret.setHashIterations(Integer.parseInt(tokens[0]));
			ret.setSalt(SharedStringUtil.hexToBytes(tokens[1]));
			ret.setPasswordHash(SharedStringUtil.hexToBytes(tokens[2]));
			ret.setName("sha-256");
			break;
		case 4:
			ret.setName(tokens[0].toLowerCase());
			ret.setHashIterations(Integer.parseInt(tokens[1]));
			ret.setSalt(SharedStringUtil.hexToBytes(tokens[2]));
			ret.setPasswordHash(SharedStringUtil.hexToBytes(tokens[3]));
			break;
		default:
			throw new IllegalArgumentException("Invalid password format");	
		}
		
		return ret;
	}


	/**
	 * @return
	 */
	@Override
	public CredentialType getCredentialType() {
		return CredentialType.PASSWORD;
	}

}