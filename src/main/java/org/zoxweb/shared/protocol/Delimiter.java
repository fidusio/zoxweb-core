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
package org.zoxweb.shared.protocol;

import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.SharedStringUtil;

/**
 * Enumeration of common protocol delimiters and punctuation marks.
 * Provides both string and byte array representations for efficient
 * protocol parsing and formatting.
 *
 * @author mnael
 * @see GetNameValue
 */
public enum Delimiter
    implements GetNameValue<String>
{
	/** Colon separator (:) */
	COLON(":"),
	/** URL scheme separator (://) */
	COLON_PATH("://"),
	/** Carriage return and line feed */
	CRLF("\r\n"),
	/** Double CRLF - end of HTTP headers */
	CRLFCRLF("\r\n\r\n"),
	/** Double quote character */
	DOUBLE_QUOTE("\""),
	/** Equals sign for key-value pairs */
	EQUAL("="),
	/** Question mark for query string start */
	QUESTION_MARK("?"),
	/** Semicolon separator */
	SEMICOLON(";"),
	/** Space character */
	SPACE(" "),

	;

	private final String value;
	private final byte[] bytes;

	Delimiter(String val)
    {
		value = val;
		bytes = SharedStringUtil.getBytes(value);
	}

    /**
     * Returns the delimiter name.
     *
     * @return the enum constant name
     */
	public String getName()
	{
		return name();
	}

    /**
     * Returns the delimiter string value.
     *
     * @return the delimiter string
     */
	public String getValue()
    {
		return value;
	}

    /**
     * Returns the delimiter as a byte array.
     *
     * @return the delimiter bytes
     */
	public final byte[] getBytes()
    {
		return bytes;
	}

	/**
	 * Returns the length of the delimiter in bytes.
	 *
	 * @return the byte length
	 */
	public int length()
	{
		return bytes.length;
	}

	/**
	 * Returns the delimiter string value.
	 *
	 * @return the delimiter string
	 */
	@Override
    public String toString()
    {
        return value;
    }

}