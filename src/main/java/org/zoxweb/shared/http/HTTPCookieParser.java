package org.zoxweb.shared.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.zoxweb.shared.util.DataDecoder;
import org.zoxweb.shared.util.SUS;

public class HTTPCookieParser
	implements DataDecoder<List<String>, List<String>>
{
	public static final HTTPCookieParser SINGLETON  = new HTTPCookieParser();
	
	private HTTPCookieParser(){}
	
	public List<String> decode(List<String> input) 
	{
		ArrayList<String> cookies = null;
		if (input != null)
		{
			for(String token: input)
			{
				cookies = new ArrayList<String>();
				String[] tokens = token.split(";");
				if(!SUS.isEmpty(tokens[0]))
				{
					cookies.add(tokens[0]);
				}
					
			}
		}
		// TODO Auto-generated method stub
		return cookies;
	}
	
	public List<String> decode(Map<String, List<String>> headers)
	{
		return decode(headers.get("Set-Cookie"));
	}
	
	
	

}
