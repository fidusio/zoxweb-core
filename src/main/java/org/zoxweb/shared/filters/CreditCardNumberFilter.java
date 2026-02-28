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

import org.zoxweb.shared.data.CreditCardType;
import org.zoxweb.shared.util.SUS;

/**
 * This class filters the credit card number based on the credit card type.
 * @author mzebib
 */
@SuppressWarnings("serial")
public class CreditCardNumberFilter
    implements ValueFilter<String, String>
{
	/**
	 * This variable declares that only one instance of this class can be 
	 * created.
	 */
	public static final CreditCardNumberFilter SINGLETON = new CreditCardNumberFilter();
	
	/**
	 * The default constructor is declared private to prevent
	 * outside instantiation of this class.
	 */
	private CreditCardNumberFilter()
    {
		
	}
	
	/**
	 * Validates the credit card number.
	 * @param creditCardNumber
	 */
	@Override
	public String validate(String creditCardNumber)
        throws NullPointerException, IllegalArgumentException
    {
		CreditCardType type = CreditCardType.lookup(creditCardNumber);
		
		if (type != null)
		{
            creditCardNumber = creditCardNumber.trim();
            creditCardNumber = creditCardNumber.replaceAll("[ -]", "");
			
			return creditCardNumber;
		}	

		throw new IllegalArgumentException("Invalid credit card number: " + creditCardNumber);

	}

	/**
	 * Checks if the credit number is valid.
	 * @param creditCardNumber
	 */
	@Override
	public boolean isValid(String creditCardNumber)
    {
		try
		{
			validate(creditCardNumber);
			return true;
		}
		catch(Exception e)
        {
			return false;
		}
	
	}
	
	/**
	 * Returns string representation of this class.
	 */
	@Override
	public String toCanonicalID()
    {
		return "CreditCardNumberFilter";
	}
	
	/**
	 * Validates the security code (card verification value) based on
	 * the credit card type.
	 * @param ccNumber
	 * @param code
	 * @return validated CVV code
	 */
	public static String validateCVV(String ccNumber, String code)
    {
		CreditCardType type = CreditCardType.lookup(ccNumber);
		code = SUS.trimOrNull(code);
		
		if (type == null)
		{
			throw new IllegalArgumentException("Invalid card number.");
		}
		
		if (code == null)
		{
			throw new IllegalArgumentException("Security code is null.");
		}

		String patternAMEX = "[0-9]{4}";
		String patternOther = "[0-9]{3}";
		
		String ret = null;
		
		switch (type)
        {
		case AMEX:
			if (code.matches(patternAMEX))
			{
				ret = code;
			}
			break;
		case MASTER:
		case VISA:
			if (code.matches(patternOther))
			{
				ret = code;
			}
			break;
		default:
			break;
		}
		
		if (ret == null)
		{
			throw new IllegalArgumentException("Invalid card security code: " + code);
		}
		
		return ret;
	}
	
	/**
	 * Validates the security code (card verification value) based on
	 * the credit card type.
	 * @param type
	 * @param code
	 * @return validated CVV
	 * @throws IllegalArgumentException
	 */
	public static String validateCVVByType(CreditCardType type, String code)
		throws IllegalArgumentException
    {
		code = SUS.trimOrNull(code);
		
		if (type == null)
		{
			throw new IllegalArgumentException("Invalid card type: " + type);
		}
		
		if (code == null)
		{
			throw new IllegalArgumentException("Invalid card security code.");
		}

		String patternAMEX = "[0-9]{4}";
		String patternOTHER = "[0-9]{3}";
		
		String ret = null;
		
		switch (type)
		{
		case AMEX:
			if (code.matches(patternAMEX))
			{
				ret = code;
			}
			break;
		case MASTER:
		case VISA:
			if (code.matches(patternOTHER))
			{
				ret = code;
			}
			break;
		default:
			break;
		}
		
		if (ret == null)
		{
			throw new IllegalArgumentException("Invalid card security code: " + code);
		}
		
		return ret;
	}
	
}