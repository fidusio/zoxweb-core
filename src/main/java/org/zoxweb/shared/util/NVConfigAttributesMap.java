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
import java.util.List;

/**
 * The NVConfig attributes map interface.
 */
public interface NVConfigAttributesMap
    extends Serializable, ReferenceID<String>
{
	/**
	 * This method returns the attributes map.
	 * @return  the attributes map.
	 */
	List<NVConfigNameMap> getAttribuesMap();
	
	/**
	 * This method sets the attributes map.
	 * @param list
	 */
	void setAttributesMap(List<NVConfigNameMap> list);
	
	/**
	 * This method looks up the attributes map based on given string input.
	 * @param attributeNameOrAttributeMappedName
	 * @return matching  attributes map
	 */
	NVConfigNameMap lookup(String attributeNameOrAttributeMappedName);
	
}