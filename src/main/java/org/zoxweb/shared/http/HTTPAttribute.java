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

public enum HTTPAttribute
implements GetValue<String>
{
	
	BOUNDARY("boundary"),
	BOUNDARY_EDGE("--"),
	CHUNKED("chunked"),

	CONTENT_ENCODING_GZIP("gzip"),
	CONTENT_ENCODING_LZ("lz"),
	CHARSET_UTF8("charset=utf-8"),
	FORM_DATA("form-data"),
	FILENAME("filename"),
	KEEP_ALIVE("keep-alive"),
	NAME("name"),
	
	;

	private final String value;
	
	HTTPAttribute(String v)
	{
		value = v;
	}
	
	@Override
	public String getValue() 
	{
		return value;
	}
	
	public String toString()
	{
		return value;
	}
	
}
