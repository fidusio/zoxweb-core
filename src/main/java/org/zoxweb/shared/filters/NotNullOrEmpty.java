/*
 * Copyright (c) 2012-Jul 24, 2014 ZoxWeb.com LLC.
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
package org.zoxweb.shared.filters;

import org.zoxweb.shared.util.SharedStringUtil;

@SuppressWarnings("serial")
public class NotNullOrEmpty 
	implements ValueFilter<String,String>
{

	/**
	 * This variable declares that only one instance of this class can be 
	 * created.
	 */
	public static final NotNullOrEmpty SINGLETON = new NotNullOrEmpty();
	
	/**
	 * The default constructor is declared private to prevent
	 * outside instantiation of this class.
	 */
	private NotNullOrEmpty()
    {
		
	}

	@Override
	public String toCanonicalID()
    {
		return NotNullOrEmpty.class.getName();
	}

	@Override
	public String validate(String in) 
        throws NullPointerException, IllegalArgumentException
    {
		if (!SharedStringUtil.isEmpty(in))
		{
			return in;
		}
		else
        {
			throw new IllegalArgumentException("Empty or null value: " + in);
		}
	}

	@Override
	public boolean isValid(String in)
    {
		return !SharedStringUtil.isEmpty(in);
	}

}