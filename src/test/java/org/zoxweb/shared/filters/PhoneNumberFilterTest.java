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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PhoneNumberFilterTest
{
	@Test
	public void testValidPhoneNumber() {

	    String[] numbers = {"+1-415-555-5555", "415-555-5555", "+1 (415) 555-5555", "415 555 5555", "(415)555-5555",
                "4155555555", "4155555555x123"};

	    for (String number : numbers) {
            assertTrue(PhoneNumberFilter.SINGLETON.isValid(number));
        }
	}
	
	@Test
	public void testInvalidPhoneNumber() {

	    String[] numbers = {"415 555 5555 xx 123", "4155555555ext123", "415)5555555", "+1((415)-5555555",
                ")415(5555555", "415))5555555", "(415)5555555()", ")(5555555", "()5555555",
                "(+415)5555555", "+(415)++5555555", "--(415)5555555x", "+1(415)5555555xsdfdsf"};

	    for (String number : numbers) {
            assertFalse(PhoneNumberFilter.SINGLETON.isValid(number));
        }
	}
	
}