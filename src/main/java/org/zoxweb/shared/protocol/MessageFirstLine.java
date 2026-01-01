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

import java.io.Serializable;

import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedUtil;

/**
 * Represents the first line of a protocol message.
 * Parses and stores a message line as three space-separated tokens,
 * commonly used for HTTP request/response lines (e.g., "GET /path HTTP/1.1").
 *
 * @author mnael
 */
@SuppressWarnings("serial")
public class MessageFirstLine
    implements Serializable
{

	private String tokens[] = null;

	/**
	 * Constructs a MessageFirstLine by parsing the given line.
	 * The line is split by spaces into three tokens.
	 *
	 * @param fullRequestLine the full line to parse
	 * @throws NullPointerException if the line is null
	 * @throws IllegalArgumentException if less than three tokens are found
	 */
	public MessageFirstLine(String fullRequestLine)
    {
		SUS.checkIfNulls("Can't parse a null line", fullRequestLine);
		String[] tokensTemp = fullRequestLine.split(" ");

		if (tokensTemp == null || tokensTemp.length < 3)
		{
			throw new IllegalArgumentException("illegal tokens");
		}

		tokens = new String[3];
		int index = 0;

		for (int i = 0; i < tokens.length; i++)
		{
			tokens[index] = tokensTemp[index++];
		}

		if (tokensTemp.length > 3)
		{
			for (int i = index; i < tokensTemp.length; i++)
			{
				tokens[tokens.length -1] = " " + tokensTemp[i];
			}
		}
	}

	/**
	 * Returns the first token.
	 *
	 * @return the first token (e.g., HTTP method or version)
	 */
	public String getFirstToken()
    {
		return tokens[0];
	}

	/**
	 * Sets the first token.
	 *
	 * @param tok1 the first token to set
	 */
	public void setFirstToken(String tok1)
    {
		tokens[0] = tok1;
	}

	/**
	 * Returns the second token.
	 *
	 * @return the second token (e.g., URI or status code)
	 */
	public String getSecondToken()
    {
		return tokens[1];
	}

	/**
	 * Sets the second token.
	 *
	 * @param tok2 the second token to set
	 */
	public void setSecondToken(String tok2)
    {
		tokens[1] = tok2;
	}

	/**
	 * Returns the third token.
	 *
	 * @return the third token (e.g., HTTP version or reason phrase)
	 */
	public String getThirdToken()
    {
		return tokens[2];
	}

	/**
	 * Sets the third token.
	 *
	 * @param tok3 the third token to set
	 */
	public void setThirdToken(String tok3)
    {
		tokens[2] = tok3;
	}

	/**
	 * Returns the reconstructed first line.
	 *
	 * @return the three tokens joined by spaces
	 */
	public String toString()
    {
		return tokens[0] + " " + tokens[1] + " " + tokens[2];
	}

}