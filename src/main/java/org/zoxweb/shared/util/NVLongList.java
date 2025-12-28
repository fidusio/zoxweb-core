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
package org.zoxweb.shared.util;

import java.util.ArrayList;
import java.util.List;

/**
 * This class declares NVBase as a list of long type.
 */
@SuppressWarnings("serial")
public class NVLongList
    extends NVBase<List<Long>>
{
	/**
	 * The default constructor (Java Bean compliant).
	 */
	public NVLongList()
	{
		
	}
	
	
	public NVLongList(String name)
	{
		super(name, new ArrayList<Long>());
	}
	
	/**
	 * This constructor instantiates NVLongList based on name value.
	 * @param name
	 * @param value
	 */
	public NVLongList(String name, List<Long> value)
	{
		super(name, value);
	}

}