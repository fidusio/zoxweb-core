/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AddressFilterTypeTest {

	@Test
	public void testValidUSZipCode() {
		assertTrue(AddressFilterType.US_ZIP_CODE.isValid("90025"));
		assertTrue(AddressFilterType.US_ZIP_CODE.isValid("48202"));
	}
	
	@Test
	public void testInvalidUSZipCode() {
		assertFalse(AddressFilterType.US_ZIP_CODE.isValid("0"));
		assertFalse(AddressFilterType.US_ZIP_CODE.isValid("48202555"));
	}
	
	@Test
	public void testValidCanadaPostalCode() {
		assertTrue(AddressFilterType.CANADA_POSTAL_CODE.isValid("N9G2E1"));
	}
	
	@Test
	public void testInvalidCanadaPostalCode() {
		assertFalse(AddressFilterType.CANADA_POSTAL_CODE.isValid("9N23G1"));
		assertFalse(AddressFilterType.CANADA_POSTAL_CODE.isValid("90066"));
	}
	
}