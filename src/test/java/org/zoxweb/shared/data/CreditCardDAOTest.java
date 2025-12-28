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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.zoxweb.server.util.GSONUtil;

public class CreditCardDAOTest {

	public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("MM-yyyy");
	
	public static void main(String[] args)
			throws IOException, ParseException {

		//Search for credit card type by card number.
		try {

			String[] cards= {"5412 3456 7891 1234", "4412 3456 7891 1234", "34 1234 5678 9112 3"};
			
			for (int i = 0; i < cards.length; i++) {
				System.out.println(CreditCardType.lookup(cards[i]));
			}
			
			//MasterCard
			CreditCardDAO card1 = new CreditCardDAO();
			card1.setCardType(CreditCardType.MASTER);
			
			card1.setCardHolderName("John      Adam Smith ");
            //card1.setFirstName("John");
            //card1.setLastName("Smith");
			card1.setCardNumber("5-1-1234-56789-11234");
			card1.setSecurityCode("999");
			card1.setExpirationDate(DEFAULT_DATE_FORMAT.parse("06-2015").getTime());
			String json1 = GSONUtil.toJSON(card1, true);
			System.out.println(json1);
			card1 = GSONUtil.fromJSON(json1, CreditCardDAO.class);
			System.out.println(card1.getCardType());
			System.out.println(card1.getCardHolderName());
			System.out.println(card1.getCardNumber());
			System.out.println(card1.getSecurityCode());
			System.out.println(new Date(card1.getExpirationDate()));
			System.out.println(GSONUtil.toJSON(card1, true));
			
			//VISA
			CreditCardDAO card2 = new CreditCardDAO();
			card2.setCardType(CreditCardType.VISA);
            card2.setFirstName("John");
            card2.setLastName("Smith");
			card2.setCardNumber("4400-0000-1111-2222");
			card2.setSecurityCode("000");
			card2.setExpirationDate(DEFAULT_DATE_FORMAT.parse("06-2015").getTime());
			String json2 = GSONUtil.toJSON(card2, true);
			System.out.println(json2);
			card2 = GSONUtil.fromJSON(json2, CreditCardDAO.class);
			System.out.println(card2.getCardType());
			System.out.println(card2.getCardHolderName());
			System.out.println(card2.getCardNumber());
			System.out.println(card2.getSecurityCode());
			System.out.println(new Date(card2.getExpirationDate()));
			System.out.println(GSONUtil.toJSON(card2, true));
			
			//AMEX
			CreditCardDAO card3 = new CreditCardDAO();
			card3.setCardType(CreditCardType.AMEX);
            card3.setFirstName("John");
            card3.setLastName("Smith");
			card3.setCardNumber("3-4-99999-99999-000");
			card3.setSecurityCode("2222");
			card3.setExpirationDate(DEFAULT_DATE_FORMAT.parse("06-2015").getTime());
			String json3 = GSONUtil.toJSON(card3, true);
			System.out.println(json3);
			card3 = GSONUtil.fromJSON(json3, CreditCardDAO.class);
			System.out.println(card3.getCardType());
			System.out.println(card3.getCardHolderName());
			System.out.println(card3.getCardNumber());
			System.out.println(card3.getSecurityCode());
			System.out.println(new Date(card3.getExpirationDate()));
			System.out.println(GSONUtil.toJSON(card3, true));
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		maskCreditCardNumber();
	}

	public static void maskCreditCardNumber() {
		String[] numbers = {"51-1234-56789-11234", "4400-0000 1111 2222", "3499 9999 9999 000"};
		
		for (String num : numbers) {
			System.out.println("Card Type: " + CreditCardType.lookup(num));
			System.out.println("Masked CC Number: " +  SharedDataUtil.maskCreditCardNumber(num));
		}
	}
	
}