package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.security.CryptoUtil;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class TokenTest
{
	@Test
	public void uuidTest()
	{
		UUID uuid = UUID.randomUUID();
		uuid.getLeastSignificantBits();
		
		
		byte[] token = BytesValue.LONG.toBytes(null, 0, uuid.getLeastSignificantBits(), uuid.getMostSignificantBits());
		byte[] b64 = SharedBase64.encode(token);
		byte[] uuidString = SharedBase64.encode(SharedStringUtil.embedText(""+uuid, "-", ""));
		System.out.println(SharedStringUtil.toString(b64) + " " + uuid + " " +  SharedStringUtil.toString(uuidString));
		
		try 
		{
			Key key = CryptoUtil.generateKey("HmacSHA256", 384);
	
			b64 = SharedBase64.encode(key.getEncoded());
			System.out.println(SharedStringUtil.toString(b64) );
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
