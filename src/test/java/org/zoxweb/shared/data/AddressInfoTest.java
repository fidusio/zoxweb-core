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

//import org.junit.Test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.api.APIException;
import org.zoxweb.shared.security.AccessSecurityException;
import org.zoxweb.shared.util.SUS;

public class AddressInfoTest {

	@Test
	public void testA1()
            throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		AddressInfo address = new AddressInfo();
		
		address.setStreet("123 Main St.");
		address.setCity("Los Angeles");
		address.setStateOrProvince("CA");
		address.setCountry("USA");
		address.setZIPOrPostalCode("9000");
		
		
		System.out.println(address);
		String json = GSONUtil.toJSON(address, true);
		System.out.println( json);
		AddressInfo newAddress = GSONUtil.fromJSON(json, AddressInfo.class);
		System.out.println("new address from json:" + newAddress);
	}
		
	@Test
	public void testA2() {
		AddressInfo address = new AddressInfo();
		address.setCountry("USA");

		System.out.println(address);
	}

	public void testA3() {
		AddressInfo address = new AddressInfo();
		address.setCountry("Canada");
	}

	@Test //(expected = IllegalArgumentException.class)
	public void testA4() {
		AddressInfo address = new AddressInfo();
		Assertions.assertThrows(IllegalArgumentException.class, ()->address.setCountry("UK"));
	}
	
	public void testA5() {
		AddressInfo address = new AddressInfo();
		address.setCountry("France");
	}
	
	
	public void testA6() {
		AddressInfo address = new AddressInfo();
		address.setCountry("China");
	}

	@Test
	public void testA7() {
		AddressInfo address = new AddressInfo();
		address.setCountry("India");

		System.out.println(address);
	}

	@Test
    public void testA8() {
        try {
            AddressInfo address = new AddressInfo();

            address.setStreet("123 Main Street");
            address.setCity("Los Angeles");
            address.setStateOrProvince("CA");
            address.setCountry("USA");
            address.setZIPOrPostalCode("90000");

            System.out.println(address);
            String json = GSONUtil.toJSON(address, true);
            System.out.println( json);

            try {
                AddressInfo addressFromJSON = GSONUtil.fromJSON( json, AddressInfo.class);
                System.out.println( addressFromJSON);

                System.out.println(SUS.toDebugString(addressFromJSON));
            } catch (APIException | AccessSecurityException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
}