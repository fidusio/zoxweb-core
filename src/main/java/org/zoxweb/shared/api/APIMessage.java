/*
 * Copyright (c) 2012-May 27, 2014 ZoxWeb.com LLC.
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
package org.zoxweb.shared.api;

import org.zoxweb.shared.util.AccountID;
import org.zoxweb.shared.util.ReferenceID;
import org.zoxweb.shared.util.UserID;

/**
 * The API message interface.
 * @author mzebib
 *
 */
public interface APIMessage
	extends ReferenceID<String>, 
			AccountID<String>, 
			UserID<String>
{
	/**
	 * This method returns the message type.
	 * @return
	 */
	public APIServiceType getMessageType();
	
	/**
	 * This method sets the message type.
	 * @param type
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 */
	public void setMessageType(APIServiceType type)
			throws NullPointerException, IllegalArgumentException;
	
}