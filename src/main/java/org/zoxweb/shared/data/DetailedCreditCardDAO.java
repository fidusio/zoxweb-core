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

import java.util.Date;

import org.zoxweb.shared.filters.ChainedFilter;
import org.zoxweb.shared.filters.CreditCardNumberFilter;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.EmailID;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityPortable;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

@SuppressWarnings("serial")
public class DetailedCreditCardDAO
	extends CanonicalIDDAO
	implements EmailID
{
	
	public enum Param
		implements GetNVConfig
	{
		EMAIL(NVConfigManager.createNVConfig("email", "The issuer's email address.", "Email", false, true, false, String.class, FilterType.EMAIL)),
		PHONE_NUMBER(NVConfigManager.createNVConfigEntity("phone_number", "Phone number.", "Phone Number", false, true, PhoneDAO.class, ArrayType.NOT_ARRAY)),

		CARD_HOLDER_NAME(NVConfigManager.createNVConfig("card_holder_name", "Name of card holder", "Cardholder Name", true, true, String.class)),
		CARD_TYPE(NVConfigManager.createNVConfig("credit_card_type", "Type of credit card", "Credit Card Type", true, true, CreditCardType.class)),
	
		CARD_NUMBER(NVConfigManager.createNVConfig("card_number", "Credit card number", "CardNumber", true, true, false, String.class, new ChainedFilter(CreditCardNumberFilter.SINGLETON, FilterType.ENCRYPT))),
		EXPIRATION_DATE(NVConfigManager.createNVConfig("expiration_date", "Card expiration date", "Expiration Date", true, true, Date.class)),
		SECURITY_CODE(NVConfigManager.createNVConfig("security_code", "Card security code", "Security Code", true, true, String.class)),
		
		FRONTEND_IMAGE(NVConfigManager.createNVConfigEntity("frontend_image", "Frontend image of credit card.", "Frontend Image", false, true, FileInfoDAO.class, ArrayType.NOT_ARRAY)),		
		BACKEND_IMAGE(NVConfigManager.createNVConfigEntity("backend_image", "Backend image of credit card.", "Backend Image", false, true, FileInfoDAO.class, ArrayType.NOT_ARRAY)),		
		
		BILLING_ADDRESS(NVConfigManager.createNVConfigEntity("billing_address", "The credit card billing address.", "Billing Address", true, true, AddressDAO.class, ArrayType.NOT_ARRAY)),		

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
	
	public static final NVConfigEntity NVC_DETAILED_CREDIT_CARD_DAO = new NVConfigEntityPortable
																							(
																								"detailed_credit_card_dao", 
																								null , 
																								"Detailed Credit Card", 
																								true, 
																								false, 
																								false, 
																								false, 
																								DetailedCreditCardDAO.class, 
																								SharedUtil.extractNVConfigs(Param.values()),
																								null, 
																								false, 
																								CanonicalIDDAO.NVC_CANONICAL_ID_DAO
																							);

	public DetailedCreditCardDAO()
	{
		super(NVC_DETAILED_CREDIT_CARD_DAO);
	}
	
	
	public String getEmailID() 
	{
		return lookupValue(Param.EMAIL);
	}
	
	public void setEmailID(String email) 
	{
		setValue(Param.EMAIL, email);
	}
	
	
	public PhoneDAO getPhoneNumber() 
	{
		return lookupValue(Param.PHONE_NUMBER);
	}
	
	public void setPhoneNumber(PhoneDAO phone) 
	{
		setValue(Param.PHONE_NUMBER, phone);
	}
	
	
	public String getCardHolderName() 
	{
		return lookupValue(Param.CARD_HOLDER_NAME);
	}
	
	public void setCardHolderName(String name) 
	{
		setValue(Param.CARD_HOLDER_NAME, name);
	}
	
	
	public CreditCardType getCardType() 
	{
		return lookupValue(Param.CARD_TYPE);
	}
	
	public void setCardType(CreditCardType type) 
	{
		setValue(Param.CARD_TYPE, type);
	}
	
	
	public String getCardNumber()
	{	
		return lookupValue(Param.CARD_NUMBER);
	}
	
	public void setCardNumber(String number)
	{
		setValue(Param.CARD_NUMBER, number);
	}
	
	
	public long getExpirationDate()
	{
		return lookupValue(Param.EXPIRATION_DATE);
	}
	
	public void setExpirationDate(long date) 
	{
		setValue(Param.EXPIRATION_DATE, date);
	}
	
	
	public String getSecurityCode()
	{
		return lookupValue(Param.SECURITY_CODE);
	}

	public void setSecurityCode(String code)
	{
		setValue(Param.SECURITY_CODE, CreditCardNumberFilter.validateCVV(getCardNumber(), code));
	}


	public FileInfoDAO getFrontEndImage() 
	{
		return lookupValue(Param.FRONTEND_IMAGE);
	}
	
	public void setFrontEndImage(FileInfoDAO file) 
	{
		setValue(Param.FRONTEND_IMAGE, file);
	}

	
	public FileInfoDAO getBackEndImage() 
	{
		return lookupValue(Param.BACKEND_IMAGE);
	}
	
	public void setBackEndImage(FileInfoDAO file) 
	{
		setValue(Param.BACKEND_IMAGE, file);
	}
	
	
	public AddressDAO getBillingAddress() 
	{
		return lookupValue(Param.BILLING_ADDRESS);
	}
	
	public void setBillingAddress(AddressDAO address) 
	{
		setValue(Param.BILLING_ADDRESS, address);
	}

}