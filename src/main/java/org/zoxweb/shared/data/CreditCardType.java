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
package org.zoxweb.shared.data;

import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

/**
 * This enum includes credit card types and validates
 * the credit card type based on credit card number.
 * @author mzebib
 *
 */
public enum CreditCardType 
	implements ValueFilter<String, CreditCardType>
{
	
	// "[4]\\d{12}|[4]\\d{15}"
	// "^4[0-9]{12}(?:[0-9]{3})?$"
	VISA("^4[0-9]{12}(?:[0-9]{3})?$", "V", "Visa"),
	
	MASTER("[5][12345]\\d{14}", "M", "MasterCard"),

	AMEX("[3][47]\\d{13}", "A", "American Express"),
		    
//	DISCOVER("6011\\d{12}", "D", "Discover"),
//	
//	DINERS("[3][068]\\d{12}", "?", "Diners"),
//	
//	ENROUTE("2014\\d{11}|2149\\d{11}"),
//	
//	JCB("3088\\d{12}|3096\\d{12}|3112\\d{12}|3158\\d{12}|3337\\d{12}|3528\\d{12}")
	;

    private CreditCardType(String pattern)
    {
           this(pattern, "?", null);
    }
    
    private CreditCardType(String pattern, String verisign, String display)
    {
    	this.pattern  = pattern;
        this.verisign = verisign;
        this.display = display;
    }
    
    public String getPayPalCode()
    {
           return verisign;
    }
    
    public String getDisplay()
    {
    	return display;
    }
    
    private String pattern;
    private String verisign;
    private String display;

	/**
	 * This method returns Canonical ID.
	 */
	@Override
	public String toCanonicalID() 
	{
		return (display != null) ? display : name() ;
	}

	/**
	 * Looks up the credit card type given the credit card number.
	 * @param ccNumber
	 * @return the credit card type
	 */
	public static CreditCardType lookup(String ccNumber)
	{
		if (!SUS.isEmpty(ccNumber))
		{
			for (CreditCardType type: CreditCardType.values())
			{
				if (type.isValid(ccNumber))
				{
					return type;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Validates the credit card type given the credit card number.
	 * @param ccNumber
	 */
	@Override
	public CreditCardType validate(String ccNumber) 
			throws NullPointerException, IllegalArgumentException 
	{	
		SharedUtil.checkIfNulls("Null credit card number", ccNumber);
		ccNumber = SharedStringUtil.trimOrNull(ccNumber);
		ccNumber = ccNumber.replaceAll("[ -]", "");
		
    	if (ccNumber.matches(pattern))
    	{
    		return this;
    	}
    		
    	throw new IllegalArgumentException("Invalid Credit Card Type " + ccNumber);
		
	}

	/**
	 * Checks if the credit card type is valid.
	 * @param ccNumber
	 */
	@Override
	public boolean isValid(String ccNumber) 
	{
		try
		{
			validate(ccNumber);
			return true;
		}
		
		catch (Exception e)
		{
			return false;
		}
	}

}