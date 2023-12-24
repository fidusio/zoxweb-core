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

/**
 * The unit value interface.
 * @param <V>
 */
public interface ValueUnit<V, U>
    extends SetNameValue<V>
{

	String getSeparator();
	void setSeparator(String sep);

	/**
	 * Returns the unit.
	 * @return unit
	 */
	 U getUnit();

	/**
	 * Sets the unit.
	 * @param unit
	 */
	 void setUnit(U unit);

}