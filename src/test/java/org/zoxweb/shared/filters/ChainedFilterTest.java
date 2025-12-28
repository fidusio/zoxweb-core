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


import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ChainedFilterTest {

	@Test
	public void testIsValidForValidValue()
    {
		ChainedFilter cf = new ChainedFilter(FilterType.CLEAR, FilterType.EMAIL);
        assertTrue(cf.isValid("johnsmith@zoxweb.com"));
        assertTrue(cf.isValid("  johnsmith@zoxweb.com  "));
        assertTrue(cf.isValid("johnsmith@zoxweb.com    "));
	}

    @Test
    public void testIsValidForInvalidValue()
    {
        ChainedFilter cf = new ChainedFilter(FilterType.CLEAR, FilterType.EMAIL);
        assertFalse(cf.isValid("54545"));
        assertFalse(cf.isValid(""));
        assertFalse(cf.isValid(null));
    }

    @Test
    public void testValidateForValidValue()
    {
        ChainedFilter cf = new ChainedFilter(FilterType.CLEAR, FilterType.EMAIL);
        assertEquals("johnsmith@zoxweb.com", cf.validate("johnsmith@zoxweb.com"));
        assertEquals("johnsmith@zoxweb.com", cf.validate("  johnsmith@zoxweb.com  "));
        assertEquals("johnsmith@zoxweb.com", cf.validate("johnsmith@zoxweb.com    "));
    }

    @Test
    public void testValidateForInvalidValue()
    {
        ChainedFilter cf = new ChainedFilter(FilterType.CLEAR, FilterType.EMAIL);
        assertThrows(IllegalArgumentException.class, ()->cf.validate("535355"));
    }
	
	@Test
	public void testIsFilterSupported()
    {
        ChainedFilter cf = new ChainedFilter(
                        FilterType.BIG_DECIMAL,
                        FilterType.BINARY,
                        FilterType.DOUBLE,
                        FilterType.FLOAT,
                        FilterType.INTEGER,
                        FilterType.LONG
                );

        assertTrue(cf.isFilterSupported(FilterType.BIG_DECIMAL));
        assertTrue(cf.isFilterSupported(FilterType.BINARY));
        assertTrue(cf.isFilterSupported(FilterType.DOUBLE));
        assertTrue(cf.isFilterSupported(FilterType.FLOAT));
        assertTrue(cf.isFilterSupported(FilterType.INTEGER));
        assertTrue(cf.isFilterSupported(FilterType.LONG));

        assertFalse(cf.isFilterSupported(FilterType.BOOLEAN));
		assertFalse(cf.isFilterSupported(FilterType.CLEAR));
        assertFalse(cf.isFilterSupported(FilterType.EMAIL));
        assertFalse(cf.isFilterSupported(FilterType.ENCRYPT_MASK));
	}

}
