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
package org.zoxweb.shared.filters;



import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

//import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BigDecimalFilterTest {

	@Test
	public void testValidBigDecimal() {
		BigDecimal value = BigDecimalFilter.SINGLETON.validate("100.00");
		assertEquals("100.00", value.toString());

		value = BigDecimalFilter.SINGLETON.validate("1000");
		assertEquals("1000", value.toString());

		value = BigDecimalFilter.SINGLETON.validate(".99");
		assertEquals("0.99", value.toString());
	}

	
	@Test //(expected = IllegalArgumentException.class)
	public void testInvalidBigDecimal() {
		Assertions.assertThrows(IllegalArgumentException.class, ()->BigDecimalFilter.SINGLETON.validate("John"));
	}
	
	@Test
	public void testEmptyBigDecimal() {
		Assertions.assertThrows(IllegalArgumentException.class, ()->BigDecimalFilter.SINGLETON.validate(""));
	}
	
	@Test
	public void testNullBigDecimal() {
		Assertions.assertThrows(IllegalArgumentException.class, ()->BigDecimalFilter.SINGLETON.validate(null));
	}

}