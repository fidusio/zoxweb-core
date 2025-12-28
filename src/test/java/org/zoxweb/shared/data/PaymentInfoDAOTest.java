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

import java.text.ParseException;
import java.text.SimpleDateFormat;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.accounting.PaymentInfoDAO;

public class PaymentInfoDAOTest {

	private PaymentInfoDAO paymentInfo;

	public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("MM-yyyy");

	@BeforeEach
	public void setUp() throws Exception {
		paymentInfo = new PaymentInfoDAO();
	}

	@AfterEach
	public void tearDown() throws Exception {
		paymentInfo = null;
	}

	@Test
	public void testGetCreditCard() throws ParseException {
		CreditCardDAO card = new CreditCardDAO();
		card.setCardType(CreditCardType.VISA);
		card.setFirstName("John");
		card.setLastName("Smith");
		card.setCardNumber("4400-0000-1111-2222");
		card.setSecurityCode("999");
		card.setExpirationDate(DEFAULT_DATE_FORMAT.parse("06-2015").getTime());

		paymentInfo.setCreditCard(card);
		
		System.out.println("Credit Card: " + paymentInfo.getCreditCard().getAttributes());
		
	}

	@Test//(expected = IllegalArgumentException.class)
	public void testSetCreditCard() throws ParseException {
		CreditCardDAO card = new CreditCardDAO();
		card.setCardType(CreditCardType.VISA);
        card.setFirstName("John");
        card.setLastName("Smith");
		card.setCardNumber("4400-0000-1111-2222");
		Assertions.assertThrows(IllegalArgumentException.class, ()->card.setSecurityCode(null));
		card.setExpirationDate(DEFAULT_DATE_FORMAT.parse("06-2015").getTime());

		paymentInfo.setCreditCard(card);
		
	}

	
	@Test
	public void testGetAddress() 
	{
		AddressDAO address = new AddressDAO();
		address.setStreet("123 Main St.");
		address.setCity("Los Angeles");
		address.setStateOrProvince("CA");
		address.setCountry("USA");
		address.setZIPOrPostalCode("90000");
		
		paymentInfo.setBillingAddress(address);
		
		System.out.println("Address: " + paymentInfo.getBillingAddress().getAttributes());
	}
	
	@Test
	public void testSetAddress() {
		AddressDAO address = new AddressDAO();
		address.setStreet("123 Main St.");
		address.setCity("Los Angeles");
		address.setStateOrProvince("CA");
		address.setCountry("USA");
		address.setZIPOrPostalCode("90000");
		
		paymentInfo.setBillingAddress(address);
	}
	

}