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

import java.util.List;

/**
 * This class declares NVBase as a list of double type.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class NVDoubleList
	extends NVBase<List<Double>> 
{

    /**
     * The default constructor (Java Bean compliant).
     */
	public NVDoubleList()
	{
		
	}
	
	/**
	 * This constructor which instantiates NVDoubleList object based on name and value.
	 * @param name
	 * @param value
	 */
	public NVDoubleList(String name, List<Double> value)
	{
		super(name, value);
	}

}