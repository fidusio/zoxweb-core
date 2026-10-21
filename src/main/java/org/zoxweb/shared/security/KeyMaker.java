/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
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
import org.zoxweb.shared.crypto.EncapsulatedKey;
import org.zoxweb.shared.util.NVEntity;

public interface KeyMaker 
{
	EncapsulatedKey createSubjectIDKey(SubjectIdentifier subjectID, final byte[]key)
		throws NullPointerException, IllegalArgumentException, AccessSecurityException;
	
	EncapsulatedKey createNVEntityKey(APIDataStore<?, ?> dataStore, NVEntity nve, final byte[] key)
		throws NullPointerException, IllegalArgumentException, AccessSecurityException;

	/**
	 * The entity key of {@code nve} wrapped under {@code key}, the key of its owner {@code ownerGUID}:
	 * the row's {@code subject_guid} is that owner, not necessarily {@code nve.getSubjectGUID()} — for
	 * a row an app owns (dual ownership, 2026-10-20) the owner is the app's owner subject, whose key
	 * roots the chain, while {@code subject_guid} only records the creator. The default ignores
	 * {@code ownerGUID} and delegates to {@link #createNVEntityKey(APIDataStore, NVEntity, byte[])}.
	 */
	default EncapsulatedKey createNVEntityKey(APIDataStore<?, ?> dataStore, NVEntity nve, String ownerGUID, final byte[] key)
		throws NullPointerException, IllegalArgumentException, AccessSecurityException {
		return createNVEntityKey(dataStore, nve, key);
	}


	
	byte[] getKey(APIDataStore<?, ?> dataStore, final byte[] key, String ...chainedIDs)
		throws NullPointerException, IllegalArgumentException, AccessSecurityException;
	
	byte[] getMasterKey()
		throws NullPointerException, IllegalArgumentException, AccessSecurityException;


	EncapsulatedKey lookupEncapsulatedKey(APIDataStore<?, ?> dataStore, NVEntity nve)
		throws NullPointerException, IllegalArgumentException, AccessSecurityException;

	EncapsulatedKey lookupEncapsulatedKey(APIDataStore<?, ?> dataStore, String dataRefGUID)
			throws NullPointerException, IllegalArgumentException, AccessSecurityException;
	
	EncapsulatedKey lookupEncapsulatedKey(APIDataStore<?, ?> dataStore, String resourceRefGUID, String subjectGUID)
		throws NullPointerException, IllegalArgumentException, AccessSecurityException;
	
}
