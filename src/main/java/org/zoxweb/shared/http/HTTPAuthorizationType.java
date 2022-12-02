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
package org.zoxweb.shared.http;

import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.SharedBase64;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

/**
 * The authorization the enum currently support Basic and Bearer
 * @author mnael
 *
 */
public enum HTTPAuthorizationType
	implements GetName
{
	
	BASIC("Basic")
	{
		public GetNameValue<String> toHTTPHeader(String ...args)
		{
			int index = 0;
			String username = args.length > index ? args[index++] : null;
			String password = args.length > index ? args[index++] : null;
			
			if(!SharedStringUtil.isEmpty(username) && !SharedStringUtil.isEmpty(password))
			{
			
				return new NVPair(HTTPHeader.AUTHORIZATION,
					BASIC + " " + new String(SharedBase64.encode(SharedStringUtil.getBytes(SharedUtil.toCanonicalID(':', username, password)))));
			}
			
			if(!SharedStringUtil.isEmpty(username))
			{
				return new NVPair(HTTPHeader.AUTHORIZATION,
					BASIC + " " + new String(SharedBase64.encode(SharedStringUtil.getBytes(username+":"))));
			}
			
			if(!SharedStringUtil.isEmpty(password))
			{
				return new NVPair(HTTPHeader.AUTHORIZATION,
					BASIC + " " + new String(SharedBase64.encode(SharedStringUtil.getBytes(":"+password))));
			}
			
			return null;
		}

		@Override
		public HTTPAuthentication toHTTPAuthentication(String value) 
		{
			String fullToken = SharedStringUtil.toString((SharedBase64.decode(SharedStringUtil.getBytes(value))));
			
			
			int columnIndex = fullToken.indexOf(':');
			if (columnIndex == -1)
			{
				return null;
			}
			
			
			//String parsed[] = SharedStringUtil.parseString(fullToken, ":");
			
			String user = fullToken.substring(0, columnIndex);//parsed.length > index ? parsed[index++] : null;
			String password = fullToken.substring(columnIndex+1);//parsed.length > index ? parsed[index++] : null;
			
			
			
			// TODO Auto-generated method stub
			return new HTTPAuthenticationBasic(SharedStringUtil.trimOrNull(user), SharedStringUtil.trimOrNull(password));
		}
	},
	BEARER("Bearer")
	{
		@Override
		public GetNameValue<String> toHTTPHeader(String ...args)
		{
			if (args.length == 1)
			// TODO Auto-generated method stub
				return new NVPair(HTTPHeader.AUTHORIZATION, BEARER+ " " + args[0]);
			else if (args.length > 1)
				return new NVPair(HTTPHeader.AUTHORIZATION, args[0]+ " " + args[1]);
			
			return null;
		}

		@Override
		public HTTPAuthentication toHTTPAuthentication(String value)
		{
			// TODO Auto-generated method stub
			return new HTTPAuthenticationBearer(value);
		}
		
	}
	;

	
	private final String name;
	
	
	
	HTTPAuthorizationType(String name)
	{
		this.name = name;
	}
	/**
	 * @see org.zoxweb.shared.util.GetName#getName()
	 */
	@Override
	public String getName()
	{
		// TODO Auto-generated method stub
		return name;
	}
	
	
	public String toString()
	{
		return getName();
	}
	
	abstract public GetNameValue<String> toHTTPHeader(String ...args);
	
	abstract public HTTPAuthentication toHTTPAuthentication(String value);
	
	
	
	
	

	public static HTTPAuthentication parse(GetNameValue<String> gnv)
	{ 
		if (gnv == null)
			return null;
		return parse(gnv.getValue());
	}
	
	
	public static HTTPAuthentication parse(String value)
	{ 
		if (value == null)
		{
			return null;
		}
		String tokens[] = SharedStringUtil.parseString(value, " ");
		if (tokens == null || tokens.length == 0)
		{
			throw new IllegalArgumentException("Invalid authentication value " + value );
		}
		
		int index = 0;
		String typeStr =  tokens[index++];
		HTTPAuthorizationType type = (HTTPAuthorizationType) SharedUtil.lookupEnum(typeStr, HTTPAuthorizationType.values());
		if (type == null)
		{
			throw new IllegalArgumentException("Invalid authentication value " + value );
		}
		
		return type.toHTTPAuthentication(tokens[index++]);
		
	}

}
