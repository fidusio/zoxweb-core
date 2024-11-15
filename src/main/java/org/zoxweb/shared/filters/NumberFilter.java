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
package org.zoxweb.shared.filters;

import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedUtil;

/**
 * The number filter is used to validate given strings 
 * to whole numbers (only digits 0-9 are permitted).
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class NumberFilter
    implements ValueFilter<String, String>
{

	private static final String PATTERN = "[0-9]+";

	/**
	 * This variable declares that only one instance of this class can be 
	 * created.
	 */
	public static final NumberFilter SINGLETON = new NumberFilter();
	
	/**
	 * The default constructor is declared private to prevent
	 * outside instantiation of this class.
	 */
	private NumberFilter()
    {
		
	}

	@Override
	public String toCanonicalID()
    {
		return NumberFilter.class.getName();
	}

	/**
	 * Validates the given number.
	 * @param in
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 */
	@Override
	public String validate(String in) 
        throws NullPointerException, IllegalArgumentException
    {

		SUS.checkIfNulls("Given number is empty or null.", in);
		
		if (in.matches(PATTERN))
		{
			return in;
		}
		else
		    {
			throw new IllegalArgumentException("Invalid number: " +  in);
		}
	}

	/**
	 * Checks if the given number is valid.
	 * @param in
	 * @return true if valid
	 */
	@Override
	public boolean isValid(String in)
    {
		try
        {
			validate(in);
		}
		catch (Exception e)
        {
			return false;
		}
		
		return true;
	}

}