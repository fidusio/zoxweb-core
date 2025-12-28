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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.NVPair;

public class UserInfoDAOTest {

	public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	public static final SimpleDateFormat DEFAULT_EXPIRATION_DATE_FORMAT = new SimpleDateFormat("MM-yyyy");
	
	public static void main(String[] args) throws IOException {

		try {
            UserInfoDAO user1 = new UserInfoDAO();
            user1.setReferenceID("10205452");
            user1.setTitle("MR");
            user1.setFirstName("John");
            user1.setLastName("Smith");

            ArrayList<AddressDAO> addresslist= new ArrayList<AddressDAO>();
            AddressDAO address = new AddressDAO();
            address.setStreet("123 Main Street");
            address.setCity("Los Angeles");
            address.setStateOrProvince("CA");
            address.setCountry("USA");
            address.setZIPOrPostalCode("90000");
            addresslist.add(address);
            user1.setListOfAddresses(addresslist);
		
            ArrayList<PhoneDAO> phonelist = new ArrayList<PhoneDAO>();
            PhoneDAO phone = new PhoneDAO();
            phone.setPhoneType("MOBILE");
            phone.setCountryCode("+1");
            phone.setAreaCode("310");
            phone.setNumber("5551234");
            phonelist.add(phone);
            user1.setListOfPhones(phonelist);

            ArrayList<NVPair> emailList = new ArrayList<NVPair>();
            NVPair email1 = new NVPair();
            email1.setName("Work");
            email1.setValue("jsmith@companyxyz.com");
            emailList.add(email1);
            NVPair email2 = new NVPair();
            email2.setName("Personal");
            email2.setValue("jsmith@example.com");
            emailList.add(email2);
            user1.setListOfEmails(emailList);

            ArrayList<NVPair> aliasList = new ArrayList<NVPair>();
            NVPair alias1 = new NVPair();
            alias1.setName("Primary");
            alias1.setValue("jsmith");
            aliasList.add(alias1);
            NVPair alias2 = new NVPair();
            alias2.setName("Secondary");
            alias2.setValue("jsmith2014");
            aliasList.add(alias2);
            user1.setListOfAliases(aliasList);

            ArrayList<CreditCardDAO> creditcardlist= new ArrayList<>();
            CreditCardDAO card1 = new CreditCardDAO();
            card1.setCardType(CreditCardType.VISA);
            card1.setFirstName("John");
            card1.setLastName("Smith");
            card1.setCardNumber("4500 4000 0000 0000");
            card1.setExpirationDate(DEFAULT_EXPIRATION_DATE_FORMAT.parse("06-2014").getTime());
            card1.setSecurityCode("000");
            creditcardlist.add(card1);
            user1.setListOfCreditCards(creditcardlist);



            user1.getProperties().add("gender", "male");
            user1.getProperties().add("occupation", "engineer");



            UserIDDAO user1ID = new UserIDDAO();
            user1ID.setPrimaryEmail("johnsmith@example.com");
            user1ID.setUserInfo(user1);


            System.out.println(GSONUtil.toJSON(user1, true));
            System.out.println(GSONUtil.toJSON(user1ID, true));

            String json = GSONUtil.toJSON(user1, true);

            System.out.println(json);

            user1 = GSONUtil.fromJSON(json, UserInfoDAO.class);

            String json1 = GSONUtil.toJSON(user1, true);
            System.out.println("JSON Objects Comparison: "+ json1.equals(json));
            System.out.println(json1);

            System.out.println(user1.getReferenceID());
            System.out.println(user1.getTitle());
            System.out.println(user1.getFirstName());
            System.out.println(user1.getMiddleName());
            System.out.println(user1.getLastName());
            System.out.println(new Date(user1.getDOB()));
            System.out.println(user1.getListOfAddresses());
            System.out.println(user1.getListOfPhones());
            System.out.println(user1.getListOfEmails());
            System.out.println(user1.getListOfAliases());
            System.out.println(user1.getListOfCreditCards());
            System.out.println(user1.getProperties());
            System.out.println(user1ID.getPrimaryEmail());
            System.out.println(user1ID.getUserInfo());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
