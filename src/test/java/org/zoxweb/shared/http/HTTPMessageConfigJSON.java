package org.zoxweb.shared.http;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.NVPair;

public class HTTPMessageConfigJSON
{
	public static void main(String ...args)
	{
		try
		{
			int index = 0;
			HTTPMethod method = HTTPMethod.lookup(args[index++]);
			String url = args[index++];
			String uri = args[index++];
			HTTPMessageConfig hmc = (HTTPMessageConfig) HTTPMessageConfig.createAndInit(url, uri, method, false);
			NVPair nvp = new NVPair("email", null, FilterType.EMAIL);
			
			hmc.getParameters().add(nvp);
			hmc.getParameters().add(new NVPair("name", null, FilterType.TEXT_NOT_EMPTY));
			String json = GSONUtil.toJSON(hmc, true);
			System.out.println(json);
			hmc = GSONUtil.fromJSON(json);
			nvp = (NVPair) hmc.getParameters().get("email");
			System.out.println(nvp.getValueFilter().validate("Email@GMail.com"));
			System.out.println(GSONUtil.toJSON(hmc, true));
			
			nvp = (NVPair) hmc.getParameters().get("name");
			nvp.setValue(" \t");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
