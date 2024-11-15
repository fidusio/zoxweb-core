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
package org.zoxweb.shared.filters;

import java.math.BigDecimal;

import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedUtil;

@SuppressWarnings("serial")
public class BigDecimalFilter
    implements ValueFilter<String, BigDecimal>
{

	public static final BigDecimalFilter SINGLETON = new BigDecimalFilter();
	
	private BigDecimalFilter()
    {
		
	}

	@Override
	public String toCanonicalID()
    {
		return BigDecimalFilter.class.getName();
	}

	@Override
	public BigDecimal validate(String in) 
        throws NullPointerException, IllegalArgumentException
    {
		SUS.checkIfNulls("Input empty or null.", in);
		
		try
        {
			return new BigDecimal(in);
		}
		catch (NumberFormatException e)
        {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	@Override
	public boolean isValid(String in)
    {
		try
        {
			validate(in);
			return true;
		}
		catch (Exception e)
        {
			return false;
		}
	}

}