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
package org.zoxweb.shared.data;

import org.zoxweb.shared.filters.ChainedFilter;
import org.zoxweb.shared.filters.CreditCardNumberFilter;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.*;

import java.util.Date;

/**
 * This class defines the credit card data access object used to create 
 * credit card information.
 * @author mzebib
 */
@SuppressWarnings("serial")
public class CreditCardDAO 
	extends SetNameDescriptionDAO
{
	

	public enum Param
		implements GetNVConfig
	{
        FIRST_NAME(NVConfigManager.createNVConfig("first_name", "First name", "FirstName", true, true, String.class)),
        LAST_NAME(NVConfigManager.createNVConfig("last_name", "Last name", "LastName", true, true, String.class)),

        CARD_TYPE(NVConfigManager.createNVConfig("credit_card_type", "Type of credit card", "CreditCardType", true, true, CreditCardType.class)),
//		CARD_HOLDER_NAME(NVConfigManager.createNVConfig("card_holder_name", "Name of card holder", "CardHolderName", true, true, String.class)),
//		CARD_NUMBER(NVConfigManager.createNVConfig("card_number", "Credit card number", "CardNumber", true, true, false, String.class, CreditCardNumberFilter.SINGLETON)),
//		@SuppressWarnings("unchecked")
		CARD_NUMBER(NVConfigManager.createNVConfig("card_number", "Credit card number", "CardNumber", true, true, false, String.class, new ChainedFilter(CreditCardNumberFilter.SINGLETON, FilterType.ENCRYPT_MASK))),
		EXPIRATION_DATE(NVConfigManager.createNVConfig("expiration_date", "Card expiration date", "ExpirationDate", true, true, Date.class)),
		SECURITY_CODE(NVConfigManager.createNVConfig("security_code", "Card security code", "SecurityCode", true, true, String.class)),
		
		;

        private final NVConfig nvc;

        Param(NVConfig nvc)
        {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig()
        {
            return nvc;
        }
	}
	
	public static final NVConfigEntity NVC_CREDIT_CARD_DAO = new NVConfigEntityPortable(
        "credit_card_dao",
        null ,
        "CreditCardDAO",
        true,
        false,
        false,
        false,
        CreditCardDAO.class,
        SharedUtil.extractNVConfigs(Param.values()),
        null,
        false,
        SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );
	
	
	/**
	 * This is the default constructor.
	 */
	public CreditCardDAO()
	{
		super( NVC_CREDIT_CARD_DAO);
	}

	/**
	 * Returns the credit card type.
	 * @return the credit card type
	 */
	public CreditCardType getCardType() 
	{
		return lookupValue(Param.CARD_TYPE);
	}
	
	/**
	 * Sets the credit card type.
	 * @param cardType amex, visa, mc ...
	 */
	public void setCardType(CreditCardType cardType)
	{
		setValue(Param.CARD_TYPE, cardType);
	}

    /**
     * Returns the first name.
     * @return
     */
    public String getFirstName()
    {
        return lookupValue(Param.FIRST_NAME);
    }

    /**
     * Sets the first name.
     * @param firstName of the cardholder
     */
    public void setFirstName(String firstName)
    {
        setValue(Param.FIRST_NAME, firstName);
    }

    /**
     * Returns the last name.
     * @return the cardholder lastname
     */
    public String getLastName()
    {
        return lookupValue(Param.LAST_NAME);
    }

    /**
     * Sets the last name.
     * @param lastName of the cardholder
     */
    public void setLastName(String lastName)
    {
        setValue(Param.LAST_NAME, lastName);
    }


	/**
	 * Returns the cardholder name.
	 * @return name of cardholder
	 */
	public String getCardHolderName() 
	{
		return getFirstName() + " " + getLastName();
	}

	
	/**
	 * Returns the card number.
	 * @return the card number
	 */
	public String getCardNumber()
	{	
		return lookupValue(Param.CARD_NUMBER);
	}

	/**
	 * Sets the card number.
	 * @param number
	 */
	public void setCardNumber(String number)
	{
		setValue(Param.CARD_NUMBER, number);
	}
		
	/**
	 * Returns the expiration date.
	 * @return the expiration date in millis since 1970-01-01
	 */
	public long getExpirationDate()
	{
		return lookupValue(Param.EXPIRATION_DATE);
	}
	
	/**
	 * Sets the expiration date.
	 * @param date
	 */
	public void setExpirationDate(long date) 
	{
		setValue(Param.EXPIRATION_DATE, date);
	}
	
	/**
	 * Returns the security code.
	 * @return the security code
	 */
	public String getSecurityCode()
	{
		return lookupValue(Param.SECURITY_CODE);
	}
	
	/**
	 * Sets the security code.
	 * @param code
	 */
	public void setSecurityCode(String code)
	{
		setValue(Param.SECURITY_CODE, CreditCardNumberFilter.validateCVV(getCardNumber(), code));
	}
	
	
	public void setCardHolderName(String fullName)
	{
		setFirstName(SharedStringUtil.valueBeforeLeftToken(fullName, " ").trim());
		setLastName(SharedStringUtil.valueAfterLeftToken(fullName, " ").trim());
	}
}