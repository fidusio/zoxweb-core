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

import org.zoxweb.shared.data.PhoneDAO;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

/**
 * This class is used to filter North American phone numbers into acceptable format.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class PhoneNumberFilterOLD
	implements ValueFilter<String, PhoneDAO>
{
	
	private static final  String PATTERN = "[+]*[1][0-9-()]*[ext x 0-9]+";
	private static final int MAX_LENGTH = 11;
	
	public PhoneNumberFilterOLD()
    {
		
	}
	
	@Override
	public String toCanonicalID()
    {
		return null;
	}

	/**
	 * Validates the given phone number.
	 * @param num
	 * @return phone dao
	 * @throws NullPointerException
	 * @throws IllegalArgumentException
	 */
	public PhoneDAO validate(String num)
        throws NullPointerException, IllegalArgumentException
    {
		num = SharedStringUtil.trimOrNull(num);
		SUS.checkIfNulls("Phone number empty or null", num);
		
		String number = num.replaceAll("[\\s-()]", "");
		String[] code = number.split("[+]");
		String[] numberArray = null;

		if (code.length == 2)
		{
			numberArray = code[1].split("[ext x]+"); 
		}
		else
        {
			numberArray = code[0].split("[ext x]+");
		}
		
		PhoneDAO phone = new PhoneDAO();
		
		if (number.matches(PATTERN))
		{
			if (number.length() > MAX_LENGTH && (numberArray[0].startsWith("+") || numberArray[0].startsWith("1"))) {
				phone.setCountryCode(numberArray[0].substring(0, 1));
				phone.setAreaCode(numberArray[0].substring(1, 4));
				phone.setNumber(numberArray[0].substring(4, 11));
				
				if (numberArray.length > 1)
				{
					phone.setExtension(numberArray[1]);
				}
				
				return phone;
			}
			else
            {
				phone.setAreaCode(numberArray[0].substring(0, 3));
				phone.setNumber(numberArray[0].substring(3, 10));
				
				if (numberArray.length > 1)
				{
					phone.setExtension(numberArray[1]);
				}
				
				return phone;
			}		
				
		}
		else
		    {
			throw new IllegalArgumentException("Invalid Phone Number " + number);
		}
		
	}
		
	/**
	 * Checks if the phone number is valid.
	 * @param num
	 * @return true if valid
	 */
	public boolean isValid(String num)
    {
		try
        {
			validate(num);
		}
		catch (Exception e)
        {
			return false;
		}
		
		return true;
	}
	
}