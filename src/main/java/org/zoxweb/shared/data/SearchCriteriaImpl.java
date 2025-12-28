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
package org.zoxweb.shared.data;

/**
 * The SearchCriteriaImpl class is an implementation of the SearchCriteria interface.
 * @author mzebib
 */
public class SearchCriteriaImpl
	implements SearchCriteria
{
	
	private String text;
	private boolean recursive;
	private boolean caseSensitive;
	private boolean exactMatch;
	
	/**
	 * The default constructor.
	 */
	public SearchCriteriaImpl()
	{

	}
	
	/**
	 * Returns text.
	 * @return text
	 */
	@Override
	public String getText() 
	{
		return text;
	}

	/**
	 * Sets text.
	 * @param text the text to set
	 */
	@Override
	public void setText(String text)
	{
		this.text = text;
	}

	/**
	 * Checks if recursive.
	 * @return true if recursive
	 */
	@Override
	public boolean isRecursive() 
	{
		return recursive;
	}

	/**
	 * Sets recursive.
	 * @param recursive the recursive to set
	 */
	@Override
	public void setRecursive(boolean recursive) 
	{
		this.recursive = recursive;
	}

	/**
	 * Checks if case sensitive.
	 * @return true if case sensitive
	 */
	@Override
	public boolean isCaseSensitive() 
	{
		return caseSensitive;
	}

	/**
	 * Sets case sensitive.
	 * @param caseSensitive the caseSensitive to set
	 */
	@Override
	public void setCaseSensitive(boolean caseSensitive)
	{
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Checks if exact match.
	 * @return true if exact match
	 */
	@Override
	public boolean isExactMatch() 
	{
		return exactMatch;
	}

	/**
	 * Sets exact match.
	 * @param exactMatch the exactMatch to set
	 */
	@Override
	public void setExactMatch(boolean exactMatch) 
	{
		this.exactMatch = exactMatch;
	}
	
}