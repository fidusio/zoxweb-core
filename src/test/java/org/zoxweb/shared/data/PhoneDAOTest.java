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

import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.filters.PhoneNumberFilterOLD;

public class PhoneDAOTest {

	public static void main(String[] args) throws IOException {
		PhoneDAO phone = new PhoneDAO();
		
		phone.setPhoneType("Mobile");
		phone.setCountryCode("+1");
		
		phone.setAreaCode("212");
		phone.setNumber("5551234");
		
		System.out.println(GSONUtil.toJSON(phone, true));


	
//		String str = "+1(310)   -302-7883";
//		String p = str.replaceAll("[\\s-()+]", "");
//		System.out.println(p);
//		
//		
//		
//		String ext = "3103027883x12";
//		String[] n = ext.split("[extx]+");
//		System.out.println(n[1]);
		
		
		String[] numbers = {"+1-212-5551234xt55", "+650-444-1234", "1-444-4441234x22", null, "", "+1-44489", ""};
		PhoneNumberFilterOLD filter = new PhoneNumberFilterOLD();
		PhoneDAO phone2 = new PhoneDAO();

		for (int i = 0; i < numbers.length; i++ ) {
			try {
				phone2 = filter.validate(numbers[i]);
				System.out.println(filter.isValid(numbers[i]));
				System.out.println(GSONUtil.toJSON(phone2, true));
				System.out.println(phone2.toCanonicalID());
			} catch(Exception e) {
				System.err.println(numbers[i]);
				e.printStackTrace();
			}
		}
	}

}
