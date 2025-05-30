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

import org.zoxweb.shared.api.APIDataStore;
import org.zoxweb.shared.crypto.EncryptedKey;
import org.zoxweb.shared.util.NVEntity;

public interface KeyMaker 
{
	EncryptedKey createSubjectIDKey(SubjectIdentifier subjectID, final byte[]key)
		throws NullPointerException, IllegalArgumentException, AccessException;
	
	EncryptedKey createNVEntityKey(APIDataStore<?, ?> dataStore, NVEntity nve, final byte[] key)
		throws NullPointerException, IllegalArgumentException, AccessException;

	
	
	byte[] getKey(APIDataStore<?, ?> dataStore, final byte[] key, String ...chainedIDs)
		throws NullPointerException, IllegalArgumentException, AccessException;
	
	byte[] getMasterKey()
		throws NullPointerException, IllegalArgumentException, AccessException;


	EncryptedKey lookupEncryptedKeyDOA(APIDataStore<?, ?> dataStore, NVEntity nve)
		throws NullPointerException, IllegalArgumentException, AccessException;

	EncryptedKey lookupEncryptedKeyDOA(APIDataStore<?, ?> dataStore, String dataRefGUID)
			throws NullPointerException, IllegalArgumentException, AccessException;
	
	EncryptedKey lookupEncryptedKeyDOA(APIDataStore<?, ?> dataStore, String resourceRefGUID, String subjectGUID)
		throws NullPointerException, IllegalArgumentException, AccessException;
	
}
