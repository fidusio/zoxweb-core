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

//import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;



public class AddressFilterTypeTest {

	@Test
	public void testValidUSZipCode() {
		Assertions.assertTrue(AddressFilterType.US_ZIP_CODE.isValid("90025"));
		Assertions.assertTrue(AddressFilterType.US_ZIP_CODE.isValid("48202"));
	}
	
	@Test
	public void testInvalidUSZipCode() {
		Assertions.assertFalse(AddressFilterType.US_ZIP_CODE.isValid("0"));
		Assertions.assertFalse(AddressFilterType.US_ZIP_CODE.isValid("48202555"));
	}
	
	@Test
	public void testValidCanadaPostalCode() {
		Assertions.assertTrue(AddressFilterType.CANADA_POSTAL_CODE.isValid("N9G2E1"));
	}
	
	@Test
	public void testInvalidCanadaPostalCode() {
		Assertions.assertFalse(AddressFilterType.CANADA_POSTAL_CODE.isValid("9N23G1"));
		Assertions.assertFalse(AddressFilterType.CANADA_POSTAL_CODE.isValid("90066"));
	}
	
}