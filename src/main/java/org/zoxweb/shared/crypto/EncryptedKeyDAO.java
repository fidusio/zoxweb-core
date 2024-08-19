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

import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.DoNotExpose;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class EncryptedKeyDAO
    extends EncryptedDAO
    implements DoNotExpose
{

	protected enum Param
        implements GetNVConfig
    {
		KEY_LOCK_TYPE(NVConfigManager.createNVConfig("key_lock_type", "Key lock type", "KeyLockType", true, true, KeyLockType.class)),
		REFERENCE_TYPE(NVConfigManager.createNVConfig("reference_type", "Class name of the object reference", "ReferenceType", true, true, String.class)),
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

	public final static NVConfigEntity NVCE_ENCRYPTED_KEY_DAO = new NVConfigEntityLocal("encrypted_key_dao", null, "EncryptedKeyDAO", false, true, false, false, EncryptedKeyDAO.class, SharedUtil.extractNVConfigs(Param.values()), null, false, EncryptedDAO.NVCE_ENCRYPTED_DAO);
	
	
	public EncryptedKeyDAO()
    {
		super(NVCE_ENCRYPTED_KEY_DAO);
	}

	protected EncryptedKeyDAO(NVConfigEntity nvce)
    {
		super(nvce);
	}

	public KeyLockType getKeyLockType()
    {
		return lookupValue(Param.KEY_LOCK_TYPE);
	}
	
	public void setKeyLockType(KeyLockType klt)
    {
		setValue(Param.KEY_LOCK_TYPE, klt);
	}
	

	
	public void setObjectReference(NVEntity nve)
    {
		//setValue(Params.OBJECT_REFERENCE, nve);

        if (nve.getReferenceID() == null || nve.getGUID() == null) {
			throw new AccessException("NVEntity reference ID not set.");
		}

		setReferenceID(nve.getReferenceID());
		setGUID(nve.getGUID());
		setReferenceType(nve.getClass().getName());
	}

	public String getReferenceType()
    {
		return lookupValue(Param.REFERENCE_TYPE);
	}
	
	public void setReferenceType(String classMame)
    {
		setValue(Param.REFERENCE_TYPE, classMame);
	}



}