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
package org.zoxweb.shared.filters;

import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

public enum AddressFilterType
    implements ValueFilter<String, String>
{

	CANADA_POSTAL_CODE
	{
		private static final String REGEX = "^([ABCEGHJKLMNPRSTVXY]\\d[ABCEGHJKLMNPRSTVWXYZ])\\ {0,1}(\\d[ABCEGHJKLMNPRSTVWXYZ]\\d)$";
		
		public String toCanonicalID()
        {
			return null;
		}

		public String validate(String in) 
            throws NullPointerException, IllegalArgumentException
        {

			in = SharedStringUtil.trimOrNull(in);
			SUS.checkIfNulls("Postal code is null.", in);
			
			in = in.toUpperCase();
			
			if (in.matches(REGEX))
			{
				return in;
			}
			else
            {
				throw new IllegalArgumentException("Invalid postal code: " + in);
			}
		}
		
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
	},
	US_ZIP_CODE
    {
		private static final String REGEX = "^\\d{5}";
		
		public String toCanonicalID()
        {
			return null;
		}

		public String validate(String in) 
            throws NullPointerException, IllegalArgumentException
        {
			in = SharedStringUtil.trimOrNull(in);

			SUS.checkIfNulls("ZIP code is null.", in);
			
			if (in.matches(REGEX))
			{
				return in;
			}
			else
            {
				throw new IllegalArgumentException("Invalid ZIP code: " + in);
			}
		}
		
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
	
	;

	@Override
	public String toCanonicalID()
    {
		return null;
	}

	@Override
	public String validate(String in) 
        throws NullPointerException, IllegalArgumentException
    {
		return null;
	}
	
	@Override
	public boolean isValid(String in)
    {
		return false;
	}

}