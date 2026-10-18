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
package org.zoxweb.shared.util;



import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.data.AddressDAO;


public class NVEntityMatchTest {

	@Test
	public void test() {
		AddressDAO user1 = new AddressDAO();
		user1.setReferenceID("123456789");
		user1.setName("John");
		user1.setDescription("Smith");
		
		AddressDAO user2 = new AddressDAO();
		user2.setReferenceID("123456789");
		user2.setName("John");
		user2.setDescription("Smith");

		AddressDAO user3 = new AddressDAO();
		user3.setReferenceID("OZ309");
		user3.setName("Wiliam");
		user3.setDescription("Brown");

		AddressDAO user4 = null;

		Assertions.assertTrue(SUS.equals(user1, user2));
		Assertions.assertFalse(SUS.equals(user1, user3));
		Assertions.assertFalse(SUS.equals(user2, user3));
		Assertions.assertFalse(SUS.equals(user3, user4));
	}

}
