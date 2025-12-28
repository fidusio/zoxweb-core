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
package org.zoxweb.shared.db;

import org.zoxweb.shared.util.Const.RelationalOperator;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.GetName;

@SuppressWarnings("serial")
public class QueryMatchLong
    extends QueryMatch<Long>
{
	
	public QueryMatchLong()
    {
		
	}
	
	public QueryMatchLong(RelationalOperator operator, long value, String... names)
    {
		super(operator, value, names);
	}
	
	public QueryMatchLong(RelationalOperator operator, long value, GetName... names)
    {
		super(operator, value, names);
	}
	
	public QueryMatchLong(RelationalOperator operator, long value, GetNVConfig... gnvs)
    {
		super(operator, value, gnvs);
	}

}