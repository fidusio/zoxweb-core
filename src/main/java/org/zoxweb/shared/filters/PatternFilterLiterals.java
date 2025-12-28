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
package org.zoxweb.shared.filters;

/**
 * This class is used to filter pattern literals.
 * @author mzebib
 */
@SuppressWarnings("serial")
public class PatternFilterLiterals
    implements ValueFilter<String, String>
{

	public static final String[][] PATTERN_LITERALS = {
		{"[", "\\["},
		{"]", "\\]"},
		{"(", "\\("},
		{")", "\\)"},
		{"{", "\\{"},
		{"}", "\\}"},
		{"*", "\\*"},
		{".", "\\."},
		{"^", "\\^"},
		{"|", "\\|"},
		{"+", "\\+"},
		{"?", "\\?"},
		//{"/", "\\/"},
		
		};

	/**
	 * This variable declares that only one instance of this class can be 
	 * created.
	 */
	public static final PatternFilterLiterals SINGLETON = new PatternFilterLiterals();
	
	/**
	 * The default constructor is declared private to prevent
	 * outside instantiation of this class.
	 */
	private PatternFilterLiterals()
    {
		
	}

	@Override
	public String toCanonicalID()
    {
		return null;
	}

	/**
	 * Validates the input expression based on the pattern.
	 * @param in
	 */
	@Override
	public String validate(String in)
        throws NullPointerException, IllegalArgumentException
    {
		for (String[] pattern : PATTERN_LITERALS)
		{
			in = in.replace(pattern[0], pattern[1]);
		}
	
		return in;
	}

	/**
	 * Checks if the given input expression is valid.
	 * @param in
	 */
	@Override
	public boolean isValid(String in)
    {
		return true;
	}

	/**
	 * Validates the input expression with the specified characters to be
	 * excluded from the literal pattern.
	 * @param in
	 * @param exclusionList
	 * @return validated string
	 */
	public String validateWithExclusion(String in, String... exclusionList)
    {
		for (String[] pattern : PATTERN_LITERALS)
		{
			if (exclusionList != null)
			{
				for (String exclude : exclusionList)
				{
					if (!pattern[0].equals(exclude))
					{
						in = in.replace(pattern[0], pattern[1]);
					}
				}
			}
		}
		
		return in;
	}
	
}