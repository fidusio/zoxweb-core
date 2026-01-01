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

/**
 * Enumeration of common HTTP attribute values used in HTTP message processing.
 * These attributes are typically used in HTTP headers for multipart form data,
 * content encoding, and connection management.
 *
 * @author mnael
 */
public enum HTTPAttribute
implements GetValue<String>
{
	
	/** Multipart boundary parameter name */
	BOUNDARY("boundary"),
	/** Multipart boundary edge marker (--) */
	BOUNDARY_EDGE("--"),
	/** Chunked transfer encoding value */
	CHUNKED("chunked"),
	/** GZIP content encoding value */
	CONTENT_ENCODING_GZIP("gzip"),
	/** LZ content encoding value */
	CONTENT_ENCODING_LZ("lz"),
	/** UTF-8 charset parameter */
	CHARSET_UTF8("charset=utf-8"),
	/** Form-data content disposition value */
	FORM_DATA("form-data"),
	/** Filename parameter name for file uploads */
	FILENAME("filename"),
	/** Keep-alive connection value */
	KEEP_ALIVE("keep-alive"),
	/** Name parameter for form fields */
	NAME("name"),

	;

	private final String value;

	/**
	 * Constructs an HTTPAttribute with the specified string value.
	 *
	 * @param v the string value of this attribute
	 */
	HTTPAttribute(String v)
	{
		value = v;
	}

	/**
	 * Returns the string value of this HTTP attribute.
	 *
	 * @return the attribute value
	 */
	@Override
	public String getValue()
	{
		return value;
	}

	/**
	 * Returns the string representation of this attribute.
	 *
	 * @return the attribute value as a string
	 */
	public String toString()
	{
		return value;
	}
	
}
