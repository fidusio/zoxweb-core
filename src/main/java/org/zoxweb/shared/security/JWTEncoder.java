package org.zoxweb.shared.security;

import org.zoxweb.shared.util.DataEncoder;

public interface JWTEncoder 
	extends DataEncoder<JWTEncoderData, String>
{
	String encode(byte[] key, JWT jwt)
			throws AccessSecurityException;
}
