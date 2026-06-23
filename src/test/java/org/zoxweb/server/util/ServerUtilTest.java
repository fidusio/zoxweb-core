/*
 * Copyright (c) 2012-Oct 29, 2015 ZoxWeb.com LLC.
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
package org.zoxweb.server.util;

import java.util.ArrayList;
import java.util.List;

//import org.junit.Assert;
//import org.junit.Test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.data.AddressDAO;
import org.zoxweb.shared.data.PhoneDAO;
import org.zoxweb.shared.util.NVEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerUtilTest {

	@Test
	public void test() {
		List<NVEntity> list1 = new ArrayList<NVEntity>();
		list1.add(new PhoneDAO());
		list1.add(new AddressDAO());

		Assertions.assertTrue(ServerUtil.areAllInstancesMatchingType(list1, NVEntity.class));
		Assertions.assertFalse(ServerUtil.areAllInstancesMatchingType(list1, AddressDAO.class));

		List<Object> list2 = new ArrayList<Object>();
		list2.add(new PhoneDAO());
		list2.add(new AddressDAO());
		list2.add("Hello");

		Assertions.assertFalse(ServerUtil.areAllInstancesMatchingType(list2, NVEntity.class));
		Assertions.assertTrue(ServerUtil.areAllInstancesMatchingType(list2, Object.class));

		List<Object> list3 = new ArrayList<Object>();
		list3.add("1");
		list3.add("2");
		list3.add("3");

		Assertions.assertFalse(ServerUtil.areAllInstancesMatchingType(list3, NVEntity.class));
		Assertions.assertTrue( ServerUtil.areAllInstancesMatchingType(list3, Object.class));
		Assertions.assertTrue(ServerUtil.areAllInstancesMatchingType(list3, String.class));

		List<Object> list4 = new ArrayList<Object>();
		list4.add("1");
		list4.add("2");
		list4.add("3");
		list4.add(new PhoneDAO());

		Assertions.assertFalse(ServerUtil.areAllInstancesMatchingType(list4, NVEntity.class));
		Assertions.assertTrue(ServerUtil.areAllInstancesMatchingType(list4, Object.class));
		Assertions.assertFalse(ServerUtil.areAllInstancesMatchingType(list4, String.class));
		Assertions.assertFalse(ServerUtil.areAllInstancesMatchingType(list4, PhoneDAO.class));
	}

    @Test
    void isMacOS() {
		System.out.println("Is mac: " + ServerUtil.isMacOS());
    }
}