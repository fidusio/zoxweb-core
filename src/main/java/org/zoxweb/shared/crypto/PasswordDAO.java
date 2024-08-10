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

import org.zoxweb.shared.crypto.CryptoConst.HASHType;
import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.*;

/**
 * PasswordDAO
 * @author mnael
 *
 */
@SuppressWarnings("serial")
public class PasswordDAO
    extends PropertyDAO
    implements CryptoBase
{
	
	private enum Param
        implements GetNVConfig
    {
		HASH_ITERATION(NVConfigManager.createNVConfig("hash_iteration", "Hash iteration", "HashIteration", false, true, Integer.class)),
		SALT(NVConfigManager.createNVConfig("salt", "The password salt", "Salt", false, true, byte[].class)),
		PASSWORD(NVConfigManager.createNVConfig("password", "The password", "Password", false, true, byte[].class)),

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

	public final static NVConfigEntity NVCE_PASSWORD_DAO = new NVConfigEntityLocal("password_dao", null, "PasswordDAO", false, true, false, false, PasswordDAO.class, SharedUtil.extractNVConfigs(Param.values()), null, false, PropertyDAO.NVC_PROPERTY_DAO);

    /**
     * The default constructor.
     */
    public PasswordDAO()
    {
		super(NVCE_PASSWORD_DAO);
	}

	public synchronized void setName(HASHType mdt)
    {
		SharedUtil.checkIfNulls("Null Message Digest", mdt);
		
		setName(mdt.getName());
	}

	public synchronized void setName(String name)
    {
		SharedUtil.checkIfNulls("Null Message Digest", name);

		HASHType mdt = HASHType.lookup(name);

		if (mdt == null)
		{
			throw new IllegalArgumentException("Unsupported Message Digest:" + name);
		}
		
		super.setName(mdt.getName());
	}

	public synchronized int getHashIteration()
    {
		return lookupValue(Param.HASH_ITERATION);
	}

	public synchronized void setHashIteration(int salt_iteration)
    {
		if (salt_iteration < 0)
		{
			throw new IllegalArgumentException("Invalid iteration value:" + salt_iteration);
		}

		setValue(Param.HASH_ITERATION, salt_iteration);
	}

	public synchronized byte[] getSalt()
    {
		return lookupValue(Param.SALT);
	}

	public synchronized void setSalt(byte[] salt)
    {
		setValue( Param.SALT, salt);
	}

	public synchronized byte[] getPassword()
    {
		return lookupValue(Param.PASSWORD);
	}

	public synchronized void setPassword(byte[] password)
    {
		setValue(Param.PASSWORD, password);
	}

	public void setPassword(String password)
	{
		setPassword(SharedStringUtil.getBytes(password));
	}

	@Override
	public String toCanonicalID()
    {
		if(getCanonicalID() != null)
			return getCanonicalID();
		return SharedUtil.toCanonicalID(':', getName(),getHashIteration(), SharedStringUtil.bytesToHex(getSalt()), SharedStringUtil.bytesToHex( getPassword()));
	}

	public static PasswordDAO fromCanonicalID(String passwordCanonicalID)
		throws NullPointerException, IllegalArgumentException {
		if (SharedStringUtil.isEmpty(passwordCanonicalID)) {
			throw new NullPointerException("Empty password");
		}


		try
		{
			// special case to process BCrypt
			BCryptHash bCryptHash = new BCryptHash(passwordCanonicalID);
			PasswordDAO  ret = new PasswordDAO();
			ret.setSalt(SharedStringUtil.getBytes(bCryptHash.salt));
			ret.setPassword(bCryptHash.hash);
			ret.setHashIteration(bCryptHash.logRound);
			ret.setCanonicalID(bCryptHash.toCanonicalID());
			ret.setName(HASHType.BCRYPT);
			return ret;
		}
		catch (Exception e)
		{

		}
		
		String[] tokens = passwordCanonicalID.split(":");
		PasswordDAO  ret = new PasswordDAO();
		
		switch(tokens.length)
		{
		case 3:
			ret.setHashIteration(Integer.parseInt(tokens[0]));
			ret.setSalt(SharedStringUtil.hexToBytes(tokens[1]));
			ret.setPassword(SharedStringUtil.hexToBytes(tokens[2]));
			ret.setName("sha-256");
			break;
		case 4:
			ret.setName(tokens[0].toLowerCase());
			ret.setHashIteration(Integer.parseInt(tokens[1]));
			ret.setSalt(SharedStringUtil.hexToBytes(tokens[2]));
			ret.setPassword(SharedStringUtil.hexToBytes(tokens[3]));
			break;
		default:
			throw new IllegalArgumentException("Invalid password format");	
		}
		
		return ret;
	}

}