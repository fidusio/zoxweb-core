/*
 * Copyright (c) 2012-Jun 11, 2014 ZoxWeb.com LLC.
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
package org.zoxweb;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zoxweb.shared.accounting.PaymentInfoDAO;
import org.zoxweb.shared.data.AddressDAO;
import org.zoxweb.shared.data.CreditCardDAO;
import org.zoxweb.shared.data.CreditCardType;

/**
 * @author mzebib
 *
 */
public class JTestPaymentInfo 
{
	private PaymentInfoDAO paymentInfo;
	public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("MM-yyyy");
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception 
	{
		paymentInfo = new PaymentInfoDAO();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception 
	{
		paymentInfo = null;
	}

	/**
	 * Test method for {@link org.zoxweb.shared.accounting.PaymentInfoDAO#getCreditCard()}.
	 * @throws ParseException 
	 */
	@Test
	public void testGetCreditCard() throws ParseException 
	{
		CreditCardDAO card = new CreditCardDAO();
		card.setCardType(CreditCardType.VISA);
		card.setCardHolderName("Mustapha Zebib");
		card.setCardNumber("4400-0000-1111-2222");
		card.setSecurityCode("999");
		card.setExpirationDate(DEFAULT_DATE_FORMAT.parse("06-2015").getTime());

		paymentInfo.setCreditCard(card);
		
		System.out.println("Credit Card: " + paymentInfo.getCreditCard().getAttributes());
		
	}

	/**
	 * Test method for {@link org.zoxweb.shared.accounting.PaymentInfoDAO#setCreditCard(org.zoxweb.shared.data.CreditCardDAO)}.
	 * @throws ParseException 
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testSetCreditCard() throws ParseException 
	{
		CreditCardDAO card = new CreditCardDAO();
		card.setCardType(CreditCardType.VISA);
		card.setCardHolderName("Mustapha Zebib");
		card.setCardNumber("4400-0000-1111-2222");
		card.setSecurityCode(null);
		card.setExpirationDate(DEFAULT_DATE_FORMAT.parse("06-2015").getTime());

		paymentInfo.setCreditCard(card);
		
	}

	/**
	 * Test method for {@link org.zoxweb.shared.accounting.PaymentInfoDAO#getAddress()}.
	 */
	@Test
	public void testGetAddress() 
	{
		AddressDAO address = new AddressDAO();
		address.setStreet("1580 Purdue Ave. Suite A");
		address.setCity("Los Angeles");
		address.setStateOrProvince("CA");
		address.setCountry("USA");
		address.setZIPOrPostalCode("90025");
		
		paymentInfo.setBillingAddress(address);
		
		System.out.println("Address: " + paymentInfo.getBillingAddress().getAttributes());
		
		
	}

	/**
	 * Test method for {@link org.zoxweb.shared.accounting.PaymentInfoDAO#setAddress(org.zoxweb.shared.data.AddressDAO)}.
	 */
	@Test
	public void testSetAddress() 
	{
		AddressDAO address = new AddressDAO();
		address.setStreet("1580 Purdue Ave. Suite A");
		address.setCity("Los Angeles");
		address.setStateOrProvince("CA");
		address.setCountry("USA");
		address.setZIPOrPostalCode("90025");
		
		paymentInfo.setBillingAddress(address);
	}
	

}
