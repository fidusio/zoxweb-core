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

import org.zoxweb.shared.util.GetValue;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class MatchPatternFilter
    implements ValueFilter<String, String>
{

	/**
	 * Match literal characters.
	 * @author mzebib
	 *
	 */
	public enum MatchLiteral
		implements GetValue<String>
    {

		ASTERISK("*"),
		CASE_INSENSITIVE("-i"),
		RECURSIVE("-r"),
		QUESTION_MARK("?")
		
		;
		
		private String value;
		
		MatchLiteral(String value)
        {
			this.value = value;
		}
	
		@Override
		public String getValue()
        {
			return value;
		}
	}
	
	private boolean isRecursive = false;
	private boolean isCaseSensitive = true;
	private List<String> matchPatterns = new ArrayList<String>();
//	private static final String DEFAULT_PATTERN_PREFIX = "([\\\\]?[a-zA-Z0-9!#$%&'()+,-.;=@^_'~{}\\]\\[]+[\\\\]{0,1})*";
//	private static final String DEFAULT_PATTERN_PREFIX = "([\u0000-\uFFFFa-zA-Z0-9\\s!#$%&'()+,-.;=@^_'~{}\\]\\[\"\\.\\*:<>?|////\\\\])*";
//	private static final String SINGLE_PATTERN_OCCURANCE_VALUE = "{1}";
//	private static final String SINGLE_CHARACTER_PATTERN = "[\u0000-\uFFFFa-zA-Z0-9\\s!#$%&'()+,-.;=@^_'~{}\\[\\]]{1}";
//	private static final String ASTERIK_CHARACTER_PATTERN = "[\u0000-\uFFFFa-zA-Z0-9\\s!#$%&'()+,-.;=@^_'~{}\\[\\]]*[*]{0,1}";
	
	public static final String DEFAULT_PATTERN_PREFIX = "([\u0020-\u0021\u0023-\u0029\u002B-\u0039\u003B\u003D\u0040-\u005A\\u005C\u005E-\u007B\u007D-\uFFFF]\\[\"\\.\\*:<>?|////\\\\])*";
	public static final String SINGLE_PATTERN_OCCURRENCE_VALUE = "{1}";
	public static final String SINGLE_CHARACTER_PATTERN = "[\u0020-\u0021\u0023-\u0029\u002B-\u0039\u003B\u003D\u0040-\u005A\\u005C\u005E-\u007B\u007D-\uFFFF]{1}";
	public static final String ASTERISK_CHARACTER_PATTERN = "[\u0020-\u0021\u0023-\u0029\u002B-\u0039\u003B\u003D\u0040-\u005A\\u005C\u005E-\u007B\u007D-\uFFFF]*[*]{0,1}";
	
	/**
	 * The constructor is declared private to prevent outside instantiation of this class.
	 */
	private MatchPatternFilter()
    {
		
	}
	
	/**
	 * Checks whether pattern is recursive.
	 * @return true if recursive
	 */
	public boolean isRecursive()
    {
		return isRecursive;
	}
	
	/**
	 * Checks whether pattern is casesensitive.
	 * @return true if casesensitive
	 */
	public boolean isCaseSensitive()
    {
		return isCaseSensitive;
	}
	
	/**
	 * Matches the given string value to all match patterns.
	 * @param value to match
	 * @return true if match found
	 */
	public boolean match(String value)
    {
	    if (!isCaseSensitive())
	    {
			value = value.toLowerCase();
		}
		
		for (String pattern : getMatchPatterns())
		{
			if (value.matches(pattern))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns the list of match patterns.
	 * @return match pattern
	 */
	public String[] getMatchPatterns()
    {
		return matchPatterns.toArray(new String[0]);
	}
	
	/**
	 * Checks if the given value occurs consecutively in the given pattern.
	 * @param pattern
	 * @param val
	 * @return
	 */
	private String preFilter(String pattern, String val)
    {
		int currentIndex = 0;
		
		if (pattern.indexOf(MatchLiteral.ASTERISK.getValue()) != -1)
		{
			currentIndex = pattern.indexOf(MatchLiteral.ASTERISK.getValue());
		}
		
		while (currentIndex != -1)
        {
			int index = pattern.indexOf(MatchLiteral.ASTERISK.getValue(), currentIndex + 1);
			
			if (currentIndex + 1 == index)
			{
				throw new IllegalArgumentException("The given string contains consecutive index with same character: " + pattern);
			}
			
			currentIndex = index;
		}
		
		return pattern;
	}
	
	/**
	 * Adds the given the pattern to the list of match patterns.
	 * @param pattern
	 */
	private void addPattern(String pattern)
    {
		if (!isCaseSensitive())
		{
			pattern = pattern.toLowerCase();
		}
		
		if (pattern.contains(MatchLiteral.ASTERISK.getValue()))
		{
			pattern = preFilter(pattern, MatchLiteral.ASTERISK.getValue());
			pattern = SharedStringUtil.embedText(pattern, MatchLiteral.ASTERISK.getValue(), ASTERISK_CHARACTER_PATTERN);

		}
		
		if (pattern.contains(MatchLiteral.QUESTION_MARK.getValue()))
		{
			pattern = SharedStringUtil.embedText(pattern, MatchLiteral.QUESTION_MARK.getValue(), SINGLE_CHARACTER_PATTERN);
		}
		
		pattern = DEFAULT_PATTERN_PREFIX + "(" + pattern + ")" + SINGLE_PATTERN_OCCURRENCE_VALUE;
		
		matchPatterns.add(pattern);
	}

	
	/**
	 * Creates a MatchPatternFilter object based on the given match criteria.
	 * @param matchCriteria
	 * @return pattern filter
	 */
	public static MatchPatternFilter createMatchFilter(String... matchCriteria)
	{

		if (matchCriteria.length == 1)
		{
			matchCriteria = matchCriteria[0].split(" ");
		}
		MatchPatternFilter mpf = new MatchPatternFilter();
		
		for (String str : matchCriteria)
		{
			if (!SUS.isEmpty(str))
			{
				if (str.equals(MatchLiteral.RECURSIVE.getValue()))
				{
					mpf.isRecursive = true;
				}
				else if (str.equals(MatchLiteral.CASE_INSENSITIVE.getValue()))
				{
					mpf.isCaseSensitive = false;
				}
			}
		}
		
		for (String str : matchCriteria)
		{
			if (!SUS.isEmpty(str)
                    || !str.equals(MatchLiteral.RECURSIVE.getValue())
                    || !str.equals(MatchLiteral.CASE_INSENSITIVE.getValue()))
			{
				mpf.addPattern(str);
			}
		}
		
		return mpf;
	}

	@Override
	public String toCanonicalID()
    {
		return null;
	}

	@Override
	public String validate(String in) 
        throws NullPointerException, IllegalArgumentException
    {

	    if (isValid(in))
	    {
			return in;
		}
		
		throw new IllegalArgumentException("Invalid input: " + in);
	}

	@Override
	public boolean isValid(String in)
    {
		return match(in);
	}
	
}