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

import org.zoxweb.shared.util.GetValue;
import org.zoxweb.shared.util.SharedStringUtil;

/**
 * Enumeration of HTTP protocol versions.
 * Currently supports HTTP/1.0 and HTTP/1.1.
 *
 * @author mnael
 */
public enum HTTPVersion
	implements GetValue<String>
{
	/** HTTP version 1.0 */
	HTTP_1_0("HTTP/1.0"),
	/** HTTP version 1.1 */
	HTTP_1_1("HTTP/1.1")
	;

	private final String value;

	/**
	 * Constructs an HTTPVersion with the specified version string.
	 *
	 * @param value the version string
	 */
	HTTPVersion(String value)
	{
		this.value = value;
	}

	/**
	 * Returns the HTTP version string.
	 *
	 * @return the version string (e.g., "HTTP/1.1")
	 */
	@Override
	public String getValue()
	{
		return value;
	}

	/**
	 * Returns the version string.
	 *
	 * @return the version string
	 */
	public String toString()
	{
		return getValue();
	}

	/**
	 * Looks up an HTTPVersion by its string representation.
	 *
	 * @param val the version string to look up
	 * @return the matching HTTPVersion, or null if not found
	 */
	public static HTTPVersion lookup(String val)
	{
		val = SharedStringUtil.trimOrNull(val);
		
		if (val != null)
		{
			val = val.toUpperCase();

			for (HTTPVersion ret : HTTPVersion.values())
			{
				if(ret.getValue().equals(val))
				{
					return ret;
				}
			}
		}
		
		return null;
	}
	
}
