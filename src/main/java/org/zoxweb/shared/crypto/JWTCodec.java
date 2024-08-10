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

import org.zoxweb.shared.security.AccessSecurityException;
import org.zoxweb.shared.security.JWTDecoder;
import org.zoxweb.shared.security.JWTEncoder;

public interface JWTCodec
	extends JWTEncoder, JWTDecoder
{

	byte[] hash(String mdAlgo, byte[]... tokens)
		throws AccessSecurityException;
	
	byte[] hash(String mdAlgo, String... tokens)
		throws AccessSecurityException;
	
	
	byte[] hmacSHA256(byte[] key, byte[] data)
		throws AccessSecurityException;
	
	byte[] hmacSHA512(byte[] key, byte[] data)
			throws AccessSecurityException;


}