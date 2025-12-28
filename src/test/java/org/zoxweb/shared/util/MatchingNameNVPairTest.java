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

import java.util.ArrayList;
import java.util.List;



public class MatchingNameNVPairTest {

	@Test
	public void test() {
		List<NVPair> list = new ArrayList<NVPair>();
		
		list.add(new NVPair("USA", "United States of America"));
		list.add(new NVPair("CAN", "Canada"));
		list.add(new NVPair("GBR", "United Kingdom"));
		list.add(new NVPair("FRA", "France"));

        Assertions.assertTrue(SharedUtil.doesNameExistNVList(list, "USA"));
        Assertions.assertFalse(SharedUtil.doesNameExistNVList(list, "CHN"));
        Assertions.assertTrue(SharedUtil.doesNameExistNVList(list, "FRA"));
        Assertions.assertFalse(SharedUtil.doesNameExistNVList(list, "JPN"));
	}

}