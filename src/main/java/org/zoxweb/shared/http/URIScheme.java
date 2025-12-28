/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
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

import org.zoxweb.shared.util.GetNameValue;

public enum URIScheme
    implements GetNameValue<Integer>
{
	
	// WARNING: it is crucial to define https before http otherwise the match will never detect https
	HTTPS("https", 443),
	HTTP("http", 80),
	FTP("ftp", 23),
	FILE("file", -1),
	MAIL_TO("mailto", -1),
	DATA("data", -1),
	WSS("wss", 443),
	WS("ws", 80),
	
	;

	private final String name;
	private final int defaultPort;
	
	URIScheme(String name, int port)
	{
		this.name= name;
		this.defaultPort = port;
	}

	public static URIScheme match(String uri)
	{
		if (uri != null)
		{
			uri = uri.toLowerCase().trim();

			for (URIScheme us: URIScheme.values())
			{
				if (uri.startsWith(us.getName()))
                {
                    return us;
                }
			}
		}
		
		return null;
	}

    @Override
    public String getName()
    {
        return name;
    }
    
    public String toString()
    {
    	return getName();
    }
    /**
     * @return the default port -1 not defined
     */
	@Override
	public Integer getValue()
    {
		return defaultPort;
	}
}
