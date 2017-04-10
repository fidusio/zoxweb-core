/*
 * Copyright (c) 2012-Sep 9, 2014 ZoxWeb.com LLC.
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
package org.zoxweb.shared.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.Assert;
import org.junit.Test;

import org.zoxweb.shared.data.CreditCardDAO;
import org.zoxweb.shared.data.CreditCardType;
import org.zoxweb.shared.util.NVConfigMapUtil;

public class StringMappingUtilTest {

	public static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("MM-yyyy");

	@Test
	public void testA1() throws ParseException {
		CreditCardDAO card = new CreditCardDAO();
		card.setCardType(CreditCardType.MASTER);
		card.setCardHolderName("John Smith");
		card.setCardNumber("5-1-1234-56789-11234");
		card.setSecurityCode("999");
		card.setExpirationDate(DEFAULT_DATE_FORMAT.parse("06-2015").getTime());
	
		Assert.assertNotNull(card);

		card = new CreditCardDAO();
		Assert.assertEquals("0", NVConfigMapUtil.toString(card, null));

		Assert.assertNull(NVConfigMapUtil.toString(null, null));
	}
	
}