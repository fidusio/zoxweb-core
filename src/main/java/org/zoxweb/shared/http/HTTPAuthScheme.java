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

import org.zoxweb.shared.crypto.CryptoConst;
import org.zoxweb.shared.util.*;

/**
 * The authorization the enum currently support Basic and Bearer
 * @author mnael
 *
 */
public enum HTTPAuthScheme
	implements GetName
{
	
	BASIC(CryptoConst.AuthenticationType.BASIC.getName())
	{
		public GetNameValue<String> toHTTPHeader(String ...args)
		{
			int index = 0;
			String usernameAndMaybePassword = args.length > index ? args[index++] : null;
			String password = args.length > index ? args[index++] : null;
			
			if(SUS.isNotEmpty(usernameAndMaybePassword) && SUS.isNotEmpty(password))
			{
			
				return new NVPair(HTTPHeader.AUTHORIZATION,
					BASIC.getName() + " " + new String(SharedBase64.encode(SharedStringUtil.getBytes(SharedUtil.toCanonicalID(':', usernameAndMaybePassword, password)))));
			}
			
			if(SUS.isNotEmpty(usernameAndMaybePassword))
			{
				String authToken;
				// if the username has the password like user:password
				if (usernameAndMaybePassword.indexOf(':') != -1)
				{
					authToken = usernameAndMaybePassword;
				}
				else
				{
					authToken = usernameAndMaybePassword + ":";
				}

				return new NVPair(HTTPHeader.AUTHORIZATION,
					BASIC.getName() + " " + new String(SharedBase64.encode(SharedStringUtil.getBytes(authToken))));
			}
			
			if(SUS.isNotEmpty(password))
			{
				return new NVPair(HTTPHeader.AUTHORIZATION,
					BASIC.getName() + " " + new String(SharedBase64.encode(SharedStringUtil.getBytes(":"+password))));
			}
			
			return null;
		}

		@Override
		public HTTPAuthorization toHTTPAuthentication(String value)
		{
			String[] tokens = SharedStringUtil.parseString(value, " ", true);
			if(tokens.length > 1)
			{
				if (!BASIC.name().equalsIgnoreCase(tokens[0]))
					throw new IllegalArgumentException("Not a basic authentication type " + tokens[0]);

				value = tokens[1];
			}

			String fullToken = SharedStringUtil.toString((SharedBase64.decode(SharedBase64.Base64Type.DEFAULT, SharedStringUtil.getBytes(value))));
			
			
			int columnIndex = fullToken.indexOf(':');
			if (columnIndex == -1)
			{
				return null;
			}
			
			
			//String parsed[] = SharedStringUtil.parseString(fullToken, ":");
			
			String user = fullToken.substring(0, columnIndex);//parsed.length > index ? parsed[index++] : null;
			String password = fullToken.substring(columnIndex+1);//parsed.length > index ? parsed[index++] : null;
			
			
			
			// TODO Auto-generated method stub
			return new HTTPAuthorizationBasic(SharedStringUtil.trimOrNull(user), SharedStringUtil.trimOrNull(password));
		}
	},
	BEARER(CryptoConst.AuthenticationType.BEARER.getName())
	{
		@Override
		public GetNameValue<String> toHTTPHeader(String ...args)
		{
			if (args.length == 1) {
				// TODO Auto-generated method stub
				String token = args[0];
				String[] tokens = SharedStringUtil.parseString(token, " ", true);
				if(tokens.length > 1)
				{
					if (!BEARER.name().equalsIgnoreCase(tokens[0]))
						throw new IllegalArgumentException("Not a bearer authentication type " + tokens[0]);

					token = tokens[1];
				}
				return new NVPair(HTTPHeader.AUTHORIZATION, BEARER.getName() + " " + token);
			}
			else if (args.length > 1)
				return new NVPair(HTTPHeader.AUTHORIZATION, args[0]+ " " + args[1]);
			
			return null;
		}

		@Override
		public HTTPAuthorization toHTTPAuthentication(String value)
		{
			// TODO Auto-generated method stub
			return new HTTPAuthorization(HTTPAuthScheme.BEARER, value);
		}
		
	},
	GENERIC("GENERIC")
			{
				@Override
				public GetNameValue<String> toHTTPHeader(String... args)
				{

					StringBuilder value = new StringBuilder();
					for(String t : args)
					{
						t = SharedStringUtil.trimOrNull(t);
						if (t != null)
						{
							if (value.length() > 0)
							{
								value.append(" ");
							}
							value.append(t);
						}
					}
					if(value.length() > 0)
						return new NVPair(HTTPHeader.AUTHORIZATION, value.toString());

					return null;
				}

				@Override
				public HTTPAuthorization toHTTPAuthentication(String token) {
					return new HTTPAuthorization(token);
				}
			},

	;

	
	private final String name;
	
	
	
	HTTPAuthScheme(String name)
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
	
	abstract public HTTPAuthorization toHTTPAuthentication(String value);
	
	
	
	
	

	public static HTTPAuthorization parse(GetNameValue<String> gnv)
	{ 
		if (gnv == null)
			return null;
		return parse(gnv.getValue());
	}
	
	
	public static HTTPAuthorization parse(String value)
	{ 
		if (value == null)
		{
			return null;
		}
		String[] tokens = SharedStringUtil.parseString(value, " ");
		if (tokens == null || tokens.length == 0)
		{
			throw new IllegalArgumentException("Invalid authentication value " + value );
		}
		
		int index = 0;
		String typeStr =  tokens[index++];
		HTTPAuthScheme type = SharedUtil.lookupEnum(typeStr, HTTPAuthScheme.values());
		if (type == null)
		{
			return new HTTPAuthorization(typeStr, tokens[index++]);
		}
		
		return type.toHTTPAuthentication(tokens[index++]);
		
	}

}
