package org.zoxweb.server.queue;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.api.APIException;
import org.zoxweb.shared.security.AccessException;
import org.zoxweb.shared.util.DataDecoder;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.SharedBase64.Base64Type;

public class NVEntityByteDecoder implements DataDecoder<byte[], NVEntityQueueEvent> {

	@Override
	public NVEntityQueueEvent decode(byte[] input) 
	{
		try 
		{
			NVEntity content = GSONUtil.fromJSON(input, Base64Type.URL);
			return new NVEntityQueueEvent(this, content);
		} catch (AccessException | APIException | NullPointerException
				| IllegalArgumentException e) {
			// TODO Auto-generated catch block
			throw new IllegalArgumentException(e.getMessage());
		}	
	}
}
