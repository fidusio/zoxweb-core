package org.zoxweb.shared.security;

import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedUtil;

public final class JWTDecoderData
{
	private final byte[] key;
	private final String jwtToken;
	public JWTDecoderData(byte[] key, String jwtToken)
	{
		SUS.checkIfNulls("Null constructor parameter", key, jwtToken);
		this.key = key;
		this.jwtToken = jwtToken;
	}
	
	public byte[] getKey()
	{
		return key;
	}
	
	public String getToken()
	{
		return jwtToken;
	}
}
