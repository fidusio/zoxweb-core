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
package org.zoxweb.shared.protocol;

import java.io.Serializable;

import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class MessageFirstLine
    implements Serializable
{

	private String tokens[] = null;

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
	
	public String getFirstToken()
    {
		return tokens[0];
	}

	public void setFirstToken(String tok1)
    {
		tokens[0] = tok1;
	}

	public String getSecondToken()
    {
		return tokens[1];
	}

	public void setSecondToken(String tok2)
    {
		tokens[1] = tok2;
	}

	public String getThirdToken()
    {
		return tokens[2];
	}

	public void setThirdToken(String tok3)
    {
		tokens[2] = tok3;
	}
	
	public String toString()
    {
		return tokens[0] + " " + tokens[1] + " " + tokens[2];
	}
	
}