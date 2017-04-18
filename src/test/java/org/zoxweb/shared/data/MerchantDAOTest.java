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

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MerchantDAOTest {

	private MerchantDAO merchant;

	@Before
	public void setUp() throws Exception {
		merchant = new MerchantDAO();
	}

	@After
	public void tearDown() throws Exception {
		merchant = null;
	}

	@Test
	public void testMerchantDAO() {
		MerchantDAO merchant1 = new MerchantDAO();
		assertFalse(merchant.equals(merchant1));
	}

	@Test
	public void testGetDomainName() {
		merchant.setName("Google");
		assertEquals("Name check: ", "Google", merchant.getName());
	}


	@Test(expected = NullPointerException.class)
	public void testSetDomainName() {
		merchant.setName(null);
	}

	@Test
	public void testGetDomainID() {
		merchant.setAccountID("www.google.com");
		assertEquals("Domain ID: ", "www.google.com", merchant.getAccountID());
	}

	@Test
	public void testSetDomainID() {
		merchant.setAccountID("www.yahoo.com");
	}

	@Test(expected = Error.class)
	public void testGetCompanyType() {
		merchant.setCompanyType("computer software");
		assertEquals("CompanyType: ", "internet", "computer software");
	}

	@Test
	public void testSetCompanyType() {
		merchant.setCompanyType(null);
	}

	@Test
	public void testListOfAddresses() {
		ArrayList<AddressDAO> addresslist= new ArrayList<AddressDAO>();
		AddressDAO address = new AddressDAO();
		address.setStreet("123 Main St.");
		address.setCity("Los Angeles");
		address.setStateOrProvince("CA");
		address.setCountry("USA");
		address.setZIPOrPostalCode("90000");
		addresslist.add(address);
		merchant.setListOfAddresses(addresslist);

		assertNotNull(merchant.getListOfAddresses());
	}

	@Test
	public void testListOfPhones() {
		ArrayList<PhoneDAO> phonelist = new ArrayList<PhoneDAO>();
		PhoneDAO phone = new PhoneDAO();
		phone.setPhoneType("MOBILE");
		phone.setCountryCode("+1");
		phone.setAreaCode("212");
		phone.setNumber("5551234");
		phonelist.add(phone);
		merchant.setListOfPhones(phonelist);
		
		assertNotNull(merchant.getListOfPhones());
	}


	@Test
	public void testSetListOfDomainEmails() {
		merchant.setListOfDomainEmails(null);
	}

	@Test(expected = Error.class)
	public void testGetListOfPaymentInfos() {
		assertEquals(0, merchant.getListOfPaymentInfos());
	}

	@Test
	public void testSetListOfPaymentInfos() {
		merchant.setListOfPaymentInfos(null);
	}

	@Test(expected = Error.class)
	public void testGetAdditonalInfo() {
		assertEquals(null, merchant.getAdditonalInfos());
	}

	@Test
	public void testSetAdditionalInfo() {
		merchant.setAdditionalInfos(null);
	}

}