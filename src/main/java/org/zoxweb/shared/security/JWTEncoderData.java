package org.zoxweb.shared.security;

import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedUtil;

public final class JWTEncoderData 
{  
	private final byte[] key;
	private final JWT jwt;
	
	public JWTEncoderData(byte[] key, JWT jwt)
	{
		SUS.checkIfNulls("Null constructor parameter", key, jwt);
		this.key = key;
		this.jwt = jwt;
	}
	
	public byte[] getKey()
	{
		return key;
	}
	
	public JWT getJWT()
	{
		return jwt;
	}
	
}
