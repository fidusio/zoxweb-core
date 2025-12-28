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

public enum HTTPVersion
	implements GetValue<String>
{
	HTTP_1_0("HTTP/1.0"),
	HTTP_1_1("HTTP/1.1")
	;
	
	private final String value;
	HTTPVersion(String value)
	{
		this.value = value;
	}
	
	@Override
	public String getValue() 
	{
		return value;
	}
	
	public String toString()
	{
		return getValue();
	}
	
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
