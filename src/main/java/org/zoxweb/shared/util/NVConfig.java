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

import org.zoxweb.shared.filters.SetValueFilter;

import java.io.Serializable;

/**
 * The NVConfig interface declares methods to set and return the meta type
 * and the filter value. This interface extends interfaces which include 
 * methods declared to set the following properties: name, description, 
 * mandatory, editable, and display name. The Serialization interface is 
 * extended in inventory to enable serialization for NVConfig.
 * @author mzebib
 *
 */
@SuppressWarnings("rawtypes")
public interface NVConfig
	extends	SetName,
			SetDescription,
			SetMandatory,
			SetMetaType,
			SetEditable,
			SetDisplayName,
			SetUnique,
			SetHidden,
			SetValueFilter,
			Serializable
{

	
	/**
	 * @return base meta type if the meta type is an array
	 */
	Class<?> getMetaTypeBase();
	
	
	void setArray(boolean array);
	boolean isArray();
	
	boolean isEnum();
	
	boolean isTypeReferenceID();
	
	boolean isStatic();
	
	void setTypeReferenceID(boolean type);

}