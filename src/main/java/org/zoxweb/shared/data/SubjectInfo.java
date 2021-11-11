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
package org.zoxweb.shared.data;

import org.zoxweb.shared.util.SubjectID;

/**
 * The subject info interface.
 */
public interface SubjectInfo
	extends SubjectID<String>
{

    /**
     * Returns the title.
     * @return title
     */
    public String getTitle();

    /**
     * Sets the title.
     * @param title
     */
    public void setTitle(String title);

	/**
	 * Returns the first name.
	 * @return fist name 
	 */
	public String getFirstName();
	
	/**
	 * Sets the first name.
	 * @param firstName
	 */
	public void setFirstName(String firstName);

	/**
	 * Returns the middle name.
	 * @return middle name
	 */
	public String getMiddleName();
	
	/**
	 * Sets the middle name.
	 * @param middleName
	 */
	public void setMiddleName(String middleName);
	
	/**
	 * Returns the last name.
	 * @return last name
	 */
	public String getLastName();
	
	/**
	 * Sets the last name.
	 * @param lastName
	 */
	public void setLastName(String lastName);
	
	/**
	 * Returns the primary email.
	 * @return primary email
	 */
	public String getPrimaryEmail();
	
	/**
	 * Sets the primary email.
	 * @param email
	 */
	public void setPrimaryEmail(String email);
		
}