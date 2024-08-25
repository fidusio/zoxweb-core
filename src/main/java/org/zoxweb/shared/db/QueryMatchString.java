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
package org.zoxweb.shared.db;

import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.Const.RelationalOperator;

@SuppressWarnings("serial")
public class QueryMatchString
    extends QueryMatch<String>
{

	public QueryMatchString()
    {
		
	}
	
	public QueryMatchString(String name, String value, RelationalOperator operator)
    {
		super(name, value, operator);
	}
	
	public QueryMatchString(GetName gc, String value, RelationalOperator operator)
    {
		super(operator, value, gc);
	}
	
	public QueryMatchString(GetNameValue<String> gnv, RelationalOperator operator)
    {
		//super(gnv.getName(), gnv.getValue(), operator);
		super(operator, gnv.getValue(), gnv);
	}

	public QueryMatchString(RelationalOperator operator, String value, String... names)
    {
		super(operator, value, names);
	}
	
	public QueryMatchString(RelationalOperator operator, String value, GetName... names)
    {
		super(operator, value, names);
	}
	
	public QueryMatchString(RelationalOperator operator, String value, GetNVConfig... gnvs)
    {
		super(operator, value, gnvs);
	}


	public static QueryMatchString toQueryMatch(String query)
	{
		for(RelationalOperator ro : RelationalOperator.values())
		{
			NVPair nameValue = SharedUtil.toNVPair(query, ro.getValue(), false);
			if (nameValue != null && nameValue.getName() != null && nameValue.getValue() != null)
			{
				return new QueryMatchString(nameValue, ro);
			}
		}

		return null;
	}

}