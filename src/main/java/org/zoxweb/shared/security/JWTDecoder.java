package org.zoxweb.shared.security;

import org.zoxweb.shared.util.DataDecoder;

public interface JWTDecoder
	extends DataDecoder<JWTDecoderData, JWT>
{
	JWT decode(byte key[], String b64URLToken)
			throws AccessSecurityException;
}
