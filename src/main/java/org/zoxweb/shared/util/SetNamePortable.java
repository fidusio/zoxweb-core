/*
 * Copyright (c) 2012-2014 ZoxWeb.com LLC.
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
 * 
 */
@SuppressWarnings("serial")
public class SetNamePortable
	implements SetName, Serializable
{
	private String name;
	public SetNamePortable()
	{
		
	}
	public SetNamePortable(String name)
	{
		setName(name);
	}
	

	/**
	 * @see org.zoxweb.shared.util.GetName#getName()
	 */
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}


	/**
	 * @see org.zoxweb.shared.util.SetName#setName(java.lang.String)
	 */
	@Override
	public void setName(String name)
	{
		SharedUtil.checkIfNulls("Null name", name);
		this.name = name;
	}
}
