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

public enum DisplayUnit
    implements GetName
{

	EM("EM"),
	PERCENT("%"),
	PIXEL("px")
	
	;

	private String name;
	
	DisplayUnit(String name)
    {
		this.name = name;
	}
	
	@Override
	public String getName()
    {
		return name;
	}


	public static DisplayUnit parseUnit(String str) {
		if (!SharedStringUtil.isEmpty(str)) {
			str = str.toLowerCase();

			for (DisplayUnit unit : values()) {
				if (str.endsWith(unit.getName())) {
					return unit;
				}
			}
		}

		return null;
	}

}