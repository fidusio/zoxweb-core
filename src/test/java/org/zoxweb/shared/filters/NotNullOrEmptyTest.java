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

public class NotNullOrEmptyTest {

    @Test
    public void testIsValidForValidValue() {
        assertTrue(NotNullOrEmpty.SINGLETON.isValid("example"));
        assertTrue(NotNullOrEmpty.SINGLETON.isValid("test"));
    }

    @Test
    public void testIsValidForInvalidValue() {
        assertFalse(NotNullOrEmpty.SINGLETON.isValid(null));
        assertFalse(NotNullOrEmpty.SINGLETON.isValid(" "));
    }

    @Test
    public void testValidateForValidValue() {
        assertEquals("example", NotNullOrEmpty.SINGLETON.validate("example"));
    }

    @Test
    public void testValidateForInvalidValue() {
        assertThrows(IllegalArgumentException.class, ()->NotNullOrEmpty.SINGLETON.validate(" "));
    }
}