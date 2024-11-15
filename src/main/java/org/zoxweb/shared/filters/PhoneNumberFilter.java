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
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedStringUtil.MatchToken;
import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class PhoneNumberFilter
	implements ValueFilter<String, String>
{
	
	private static final String PATTERN = "[0-9]+";
	private static final String PLUS_SIGN = "+";
	private static final String OPEN_PARENTHESIS = "(";
	private static final String CLOSE_PARENTHESIS = ")";
	private static final String SPACE = " ";
	private static final String HYPHEN = "-";
	private static final String EXTENSION = "x";

	/**
	 * This variable declares that only one instance of this class can be 
	 * created.
	 */
	public static final PhoneNumberFilter SINGLETON = new PhoneNumberFilter();
	
	/**
	 * The default constructor is declared private to prevent
	 * outside instantiation of this class.
	 */
	private PhoneNumberFilter()
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

		SUS.checkIfNulls("Number is null.", in);
	
		String number = in;

		//	Check "+"
		//		- Can occur zero or once
		//		- If occurs, must be at beginning
		MatchToken plusSignToken = SharedStringUtil.matchToken(number, PLUS_SIGN, false);
		
		if (plusSignToken != null)
		{
			if (plusSignToken.getCount() != 1 || plusSignToken.getIndex() != 0)
			{
				throw new IllegalArgumentException("Invalid number: " +  in);
			}
		}
		
		//	Check "(" and ")"
		//		- BOTH or NEITHER can occur (NOT one or the other)
		//		- Only one "(" and only one ")" can occur
		//		- "(" must come before ")"
		//		- Cannot come right after each other (must be separated by at least one character)
		MatchToken openParenthesisToken = SharedStringUtil.matchToken(number, OPEN_PARENTHESIS, false);
		MatchToken closeParenthesisToken = SharedStringUtil.matchToken(number, CLOSE_PARENTHESIS, false);
		
		if (openParenthesisToken != null && closeParenthesisToken != null)
		{
			if (openParenthesisToken.getCount() != 1
					|| closeParenthesisToken.getCount() != 1
                    || closeParenthesisToken.getIndex() <= (openParenthesisToken.getIndex() + 1))
			{
				throw new IllegalArgumentException("Invalid number: " +  in);
			}
		}
		else if (openParenthesisToken == null && closeParenthesisToken == null)
		{
			
		}
		else if (openParenthesisToken == null || closeParenthesisToken == null)
		{
			throw new IllegalArgumentException("Invalid number: " +  in);
		}

		//	Check "-"
		//		- Can occur zero or more times
		//		- Cannot occur at beginning
		//		- Cannot come right after each other (must be separated by at least one character)
		MatchToken hyphenToken = SharedStringUtil.matchToken(number, HYPHEN, false);
		
		if (hyphenToken != null)
		{
			if (hyphenToken.getIndex() == 0 || number.charAt(hyphenToken.getIndex() + 1) == '-')
			{
				throw new IllegalArgumentException("Invalid number: " +  in);
			}
		}
		
		
		//	Check "x"
		//		- Can occur zero or once
		//		- Cannot occur at beginning
		MatchToken extensionToken = SharedStringUtil.matchToken(number, EXTENSION, false);
		
		if (extensionToken != null)
		{
			if (extensionToken.getCount() != 1 || extensionToken.getIndex() == 0)
			{
				throw new IllegalArgumentException("Invalid number: " +  in);
			}
		}
		
		//	Remove allowed characters before validation
		number = number.replace(PLUS_SIGN, "");
		number = number.replace(OPEN_PARENTHESIS, "");
		number = number.replace(CLOSE_PARENTHESIS, "");
		number = number.replace(SPACE, "");
		number = number.replace(HYPHEN, "");
		number = number.replace(EXTENSION, "");
		
		//	Validate NUMBERS only
		if (number.matches(PATTERN))
		{
			return in;
		}
		else
        {
			throw new IllegalArgumentException("Invalid number: " +  in);
		}
	}
	
	/**
	 * Checks if the number is valid.
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