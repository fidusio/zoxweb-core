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
import org.zoxweb.shared.util.*;

@SuppressWarnings("serial")
public class EncryptedKey
    extends EncryptedData
    implements DoNotExpose
{

	protected enum Param
        implements GetNVConfig
    {

		// represent the GUID of NVEntity that this key will be used
		REFERENCE_GUID(NVConfigManager.createNVConfig(MetaToken.REFERENCE_GUID.getName(), "The reference guid", "ReferenceGUID", true, true, String.class)),
		// represent the class name type of the NVEntity
		REFERENCE_TYPE(NVConfigManager.createNVConfig(MetaToken.REFERENCE_TYPE.getName(), "Class name of the object reference", "ReferenceType", true, true, String.class)),
		KEY_LOCK_TYPE(NVConfigManager.createNVConfig("key_lock_type", "Key lock type", "KeyLockType", true, true, KeyLockType.class)),
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

	public final static NVConfigEntity NVCE_ENCRYPTED_KEY = new NVConfigEntityLocal("encrypted_key", null, "EncryptedKey", false, true, false, false, EncryptedKey.class, SharedUtil.extractNVConfigs(Param.values()), null, false, EncryptedData.NVCE_ENCRYPTED_DATA);
	
	
	public EncryptedKey()
    {
		super(NVCE_ENCRYPTED_KEY);
	}

	protected EncryptedKey(NVConfigEntity nvce)
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
        if (nve.getReferenceID() == null || nve.getGUID() == null) {
			throw new AccessException("NVEntity reference ID not set.");
		}
		setReferenceGUID(nve.getGUID());
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
	
	public String getReferenceGUID()
	{
		return lookupValue(Param.REFERENCE_GUID);
	}
	
	public void setReferenceGUID(String resourceGUID)
	{
		setValue(Param.REFERENCE_GUID, resourceGUID);
	}



}