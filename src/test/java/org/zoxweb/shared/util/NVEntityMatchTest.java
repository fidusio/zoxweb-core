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
import org.zoxweb.shared.data.UserInfoDAO;


public class NVEntityMatchTest {

	@Test
	public void test() {
		UserInfoDAO user1 = new UserInfoDAO();
		user1.setReferenceID("123456789");
		user1.setFirstName("John");
		user1.setLastName("Smith");
		
		UserInfoDAO user2 = new UserInfoDAO();
		user2.setReferenceID("123456789");
		user2.setFirstName("John");
		user2.setLastName("Smith");

		UserInfoDAO user3 = new UserInfoDAO();
		user3.setReferenceID("OZ309");
		user3.setFirstName("Wiliam");
		user3.setLastName("Brown");

		UserInfoDAO user4 = null;

		Assertions.assertTrue(SharedUtil.equals(user1, user2));
		Assertions.assertFalse(SharedUtil.equals(user1, user3));
		Assertions.assertFalse(SharedUtil.equals(user2, user3));
		Assertions.assertFalse(SharedUtil.equals(user3, user4));
	}

}
