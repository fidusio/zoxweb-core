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

import org.zoxweb.shared.crypto.CryptoConst.MDType;
import org.zoxweb.shared.data.TimeStampDAO;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;
/**
 * PasswordDAO
 * @author mnael
 *
 */
@SuppressWarnings("serial")
public class PasswordDAO
    extends TimeStampDAO
    implements CryptoDAO
{
	
	private enum Param
        implements GetNVConfig
    {
		HASH_INTERATION(NVConfigManager.createNVConfig("hash_iteration", "Hash interration", "HashIteration", false, true, Integer.class)),
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

	public final static NVConfigEntity NVCE_PASSWORD_DAO = new NVConfigEntityLocal("password_dao", null, "PasswordDAO", false, true, false, false, PasswordDAO.class, SharedUtil.extractNVConfigs(Param.values()), null, false, TimeStampDAO.NVC_TIME_STAMP_DAO);

    /**
     * The default constructor.
     */
    public PasswordDAO()
    {
		super(NVCE_PASSWORD_DAO);
	}

	public synchronized void setName(MDType mdt)
    {
		SharedUtil.checkIfNulls("Null Message Digest", mdt);
		
		setName(mdt.getName());
	}

	public synchronized void setName(String name)
    {
		SharedUtil.checkIfNulls("Null Message Digest", name);

		MDType mdt = MDType.lookup(name);

		if (mdt == null)
		{
			throw new IllegalArgumentException("Unssupported Message Digest:" + name);
		}
		
		super.setName(mdt.getName());
	}

	public synchronized int getHashIteration()
    {
		return lookupValue(Param.HASH_INTERATION);
	}

	public synchronized void setHashIteration(int salt_iteration)
    {
		if (salt_iteration < 0)
		{
			throw new IllegalArgumentException("Invalid iteration value:" + salt_iteration);
		}

		setValue(Param.HASH_INTERATION, salt_iteration);
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

	@Override
	public String toCanonicalID()
    {
		return SharedUtil.toCanonicalID(':', getName(),getHashIteration(), SharedStringUtil.bytesToHex(getSalt()), SharedStringUtil.bytesToHex( getPassword()));
	}

	public static PasswordDAO fromCanonicalID(String passwordCanonicalID)
		throws NullPointerException, IllegalArgumentException
    {
		if (SharedStringUtil.isEmpty(passwordCanonicalID))
		{
			throw new NullPointerException("Empty password");
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