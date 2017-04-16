/*
 * Copyright (c) 2012-2015 ZoxWeb.com LLC.
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
package org.zoxweb.shared.data.shiro;

import org.zoxweb.shared.util.GetValue;
import org.zoxweb.shared.util.SharedStringUtil;

public enum ShiroTokenReplacement
    implements GetValue<String>
{
	SUBJECT_ID("$$SUBJECT_ID$$")
	;
	
	private final String tokenValue;

	ShiroTokenReplacement(String tokenValue)
	{
		this.tokenValue = tokenValue;
	}

	@Override
	public String getValue()
    {
		return tokenValue;
	}
	
	public String replace(String text, String value)
	{
		return SharedStringUtil.embedText(text, SharedStringUtil.toLowerCase(tokenValue), SharedStringUtil.toLowerCase(value));
	}

}