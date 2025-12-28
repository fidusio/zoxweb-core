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
 * The search criteria interface.
 * @author mzebib
 */
public interface SearchCriteria 
{
	/**
     * Returns the text.
	 * @return the text
	 */
	public String getText();
	
	/**
     * Sets the text.
	 * @param text the text to set
	 */
	public void setText(String text);
	
	/**
     * Checks if recursive.
	 * @return the recursive
	 */
	public boolean isRecursive();
	
	/**
     * Sets recursive.
	 * @param recursive the recursive to set
	 */
	public void setRecursive(boolean recursive);
	
	/**
     * Checks if case sensitive.
	 * @return the caseSensitive
	 */
	public boolean isCaseSensitive();
	
	/**
     * Sets case sensitive.
	 * @param caseSensitive the caseSensitive to set
	 */
	public void setCaseSensitive(boolean caseSensitive);
	
	/**
     * Checks if exact match.
	 * @return the exactMatch
	 */
	public boolean isExactMatch();
	
	/**
     * Sets exact match.
	 * @param exactMatch the exactMatch to set
	 */
	public void setExactMatch(boolean exactMatch);
	
}