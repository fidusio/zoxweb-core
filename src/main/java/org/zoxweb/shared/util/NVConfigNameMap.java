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
package org.zoxweb.shared.util;

import java.io.Serializable;

/**
 * The NVConfigNameMap interface.
 */
public interface NVConfigNameMap
	extends SetName, SetDisplayName, Serializable
{
	/**
	 * Returns the name of the NVConfig.
	 * This parameter is mandatory.
	 * @return name 
	 */
	public String getName();
	
	/**
	 * Sets the name of NVConfig.
	 * This parameter is mandatory.
	 * @param name 
	 */
	public void setName(String name);
	
	/**
	 * Returns the name of the mapped NVConfig.
	 * This parameter is optional. 
	 * @return mapped name 
	 */
	public String getMappedName();
	
	/**
	 * Sets the name of the NVConfig.
	 * This parameter is optional.
	 * @param mappedName
	 */
	public void setMappedName(String mappedName);
	
	/**
	 * Returns the display name of the NVConfig.
	 * @return display name
	 */
	public String getDisplayName();
	
	/**
	 * Sets the display name of the NVConfig.
	 * @param displayName
	 */
	public void setDisplayName(String displayName);
	
}