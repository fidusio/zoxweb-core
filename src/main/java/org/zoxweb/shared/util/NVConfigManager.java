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

import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

/**
 * This class includes utility methods of NVConfig type with 
 * attributes declared for NVConfig.
 * @author mzebib
 *
 */
public class NVConfigManager 
{
	/**
	 * The constructor is declared private to prevent instantiation of this class.
	 */
	private NVConfigManager()
	{
		
	}
	
	/**
	 * Creates an NVConfig object based on the NVConfig parameters:
	 * @param name
	 * @param description
	 * @param displayName
	 * @param mandatory
	 * @param editable
	 * @param type
	 * @return NVConfig
	 */
	public static NVConfig createNVConfig(String name, String description, String displayName, boolean mandatory, boolean editable, Class<?> type)
	{
		return createNVConfig(name, description, displayName, mandatory, editable, false, type, null);
	}
	
	/**
	 * Creates an NVConfig object based on the following parameters:
	 * @param name
	 * @param description
	 * @param displayName
	 * @param mandatory
	 * @param editable
	 * @param type
	 * @param vf
	 * @return NVConfig
	 */
	public static NVConfig createNVConfig(String name, String  description, String displayName, boolean mandatory, boolean editable, boolean isUnique, Class<?> type, ValueFilter<?, ?> vf)
	{
		return createNVConfig(name, description, displayName, mandatory, editable, isUnique, false, type, vf);
	}
	
	/**
	 * Creates an NVConfig object based on the following parameters:
	 * @param name
	 * @param description
	 * @param displayName
	 * @param mandatory
	 * @param editable
	 * @param isUnique
	 * @param isHidden
	 * @param type
	 * @param vf
	 * @return NVConfig
	 */
	public static NVConfig createNVConfig(String name,
										  String  description,
										  String displayName,
										  boolean mandatory,
										  boolean editable,
										  boolean isUnique,
										  boolean isHidden, 
										  Class<?> type,
										  ValueFilter<?, ?> vf)
	{
		return new NVConfigPortable(name, description, displayName, mandatory, editable, isUnique, isHidden, false, type, vf);
	}

    /**
     *
     * @param name
     * @param description
     * @param displayName
     * @param mandatory
     * @param editable
     * @param isUnique
     * @param isHidden
     * @param isRefID
     * @param type
     * @param vf
     * @return
     */
	public static NVConfig createNVConfig(String name,
										  String  description,
										  String displayName,
										  boolean mandatory,
										  boolean editable,
										  boolean isUnique,
										  boolean isHidden,
										  boolean isRefID,
										  Class<?> type,
										  ValueFilter<?, ?> vf)
	{
		return new NVConfigPortable(name, description, displayName, mandatory, editable, isUnique, isHidden, isRefID, type, vf);
	}

    /**
     * Creates an NVConfig object based on the following parameters.
     * @param name
     * @param description
     * @param displayName
     * @param mandatory
     * @param edit
     * @param type
     * @param at
     * @return NVConfig
     */
	public static NVConfig createNVConfigEntity(String name, 
											    String description,
											    String displayName,
											    boolean mandatory,
											    boolean edit,
											    Class<?> type,
											    ArrayType at)
	{
		return new NVConfigEntityLocal(name, description, displayName, mandatory, edit, type, at);
	}

    /**
     * Creates an NVConfigEntity object based on the following parameters.
     * @param name
     * @param description
     * @param displayName
     * @param mandatory
     * @param edit
     * @param nvce
     * @return
     */
	public static NVConfig createNVConfigEntity(String name, String  description, String displayName, boolean mandatory, boolean edit, NVConfigEntity nvce)
	{
		return new NVConfigEntityLocal(name, description, displayName, mandatory, edit, false, false, nvce, ArrayType.NOT_ARRAY);
	}

    /**
     * Creates an NVConfigEntity object based on the following parameters.
     * @param name
     * @param description
     * @param displayName
     * @param mandatory
     * @param edit
     * @param nvce
     * @param at
     * @return
     */
	public static NVConfig createNVConfigEntity(String name, 
												String description,
												String displayName, 
												boolean mandatory, 
												boolean edit, 
												NVConfigEntity nvce, 
												ArrayType at)
	{
		return new NVConfigEntityLocal(name, description, displayName, mandatory, edit, false, false, nvce, at);
	}


    /**
     *
     * @param name
     * @param description
     * @param displayName
     * @param mandatory
     * @param edit
     * @param unique
     * @param hidden
     * @param nvce
     * @param at
     * @return
     */
	public static NVConfig createNVConfigEntity(String name,
												String description,
												String displayName, 
												boolean mandatory, 
												boolean edit, 
												boolean unique, 
												boolean hidden,
												NVConfigEntity nvce,
												ArrayType at)
	{
		return new NVConfigEntityLocal(name, description, displayName, mandatory, edit, unique, hidden, nvce, at);
	}
	
	
	
}
