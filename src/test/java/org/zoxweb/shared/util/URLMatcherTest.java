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

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.http.URLMatcher;

import static org.junit.jupiter.api.Assertions.*;

public class URLMatcherTest {

    @Test
    public void testWildcardBaseDomain() {
        URLMatcher validator = new URLMatcher("*.xlogistx.io");

        // Base domain should match
        assertTrue(validator.isValid("xlogistx.io"));
        assertTrue(validator.isValid("https://xlogistx.io"));
        assertTrue(validator.isValid("http://xlogistx.io"));
        assertTrue(validator.isValid("ftp://xlogistx.io"));
    }

    @Test
    public void testWildcardSubdomains() {
        URLMatcher validator = new URLMatcher("*.xlogistx.io");

        // Subdomains should match
        assertTrue(validator.isValid("api.xlogistx.io"));
        assertTrue(validator.isValid("www.xlogistx.io"));
        assertTrue(validator.isValid("https://api.xlogistx.io"));
        assertTrue(validator.isValid("https://api.xlogistx.io:8080"));
        assertTrue(validator.isValid("ftp://files.xlogistx.io"));
    }

    @Test
    public void testWildcardNestedSubdomains() {
        URLMatcher validator = new URLMatcher("*.xlogistx.io");

        // Nested subdomains should match
        assertTrue(validator.isValid("sub.api.xlogistx.io"));
        assertTrue(validator.isValid("https://deep.sub.api.xlogistx.io:443"));
    }

    @Test
    public void testWildcardWithPorts() {
        URLMatcher validator = new URLMatcher("*.xlogistx.io");

        assertTrue(validator.isValid("api.xlogistx.io:8080"));
        assertTrue(validator.isValid("https://api.xlogistx.io:8080"));
        assertTrue(validator.isValid("xlogistx.io:443"));
    }

    @Test
    public void testWildcardWithPaths() {
        URLMatcher validator = new URLMatcher("*.xlogistx.io");

        assertTrue(validator.isValid("https://api.xlogistx.io/path/to/resource"));
        assertTrue(validator.isValid("https://api.xlogistx.io:8080/path"));
        assertTrue(validator.isValid("xlogistx.io/some/path"));
    }

    @Test
    public void testWildcardNonMatching() {
        URLMatcher validator = new URLMatcher("*.xlogistx.io");

        // Non-matching domains
        assertFalse(validator.isValid("other.com"));
        assertFalse(validator.isValid("https://other.com"));
        assertFalse(validator.isValid("xlogistx.com"));
        assertFalse(validator.isValid("notxlogistx.io"));
        assertFalse(validator.isValid("fakexlogistx.io"));
    }

    @Test
    public void testStrictSingleDomain() {
        URLMatcher validator = new URLMatcher("xlogistx.io");

        assertTrue(validator.isValid("xlogistx.io"));
        assertTrue(validator.isValid("https://xlogistx.io"));
        assertTrue(validator.isValid("xlogistx.io:8080"));

        // Subdomains should NOT match in strict mode
        assertFalse(validator.isValid("api.xlogistx.io"));
        assertFalse(validator.isValid("www.xlogistx.io"));
    }

    @Test
    public void testStrictMultipleDomains() {
        URLMatcher validator = new URLMatcher("www.xlogistx.io,xlogistx.io");

        assertTrue(validator.isValid("xlogistx.io"));
        assertTrue(validator.isValid("www.xlogistx.io"));
        assertTrue(validator.isValid("https://xlogistx.io"));
        assertTrue(validator.isValid("https://www.xlogistx.io:443"));

        // Other subdomains should NOT match
        assertFalse(validator.isValid("api.xlogistx.io"));
        assertFalse(validator.isValid("other.com"));
    }

    @Test
    public void testStrictWithSpaces() {
        URLMatcher validator = new URLMatcher(" www.xlogistx.io , xlogistx.io , api.xlogistx.io ");

        assertTrue(validator.isValid("xlogistx.io"));
        assertTrue(validator.isValid("www.xlogistx.io"));
        assertTrue(validator.isValid("api.xlogistx.io"));

        assertFalse(validator.isValid("other.xlogistx.io"));
    }

    @Test
    public void testCaseInsensitive() {
        URLMatcher validator = new URLMatcher("*.XLOGISTX.IO");

        assertTrue(validator.isValid("xlogistx.io"));
        assertTrue(validator.isValid("XLOGISTX.IO"));
        assertTrue(validator.isValid("API.xlogistx.io"));
        assertTrue(validator.isValid("https://API.XLOGISTX.IO"));
    }

    @Test
    public void testNullAndEmptyInput() {
        URLMatcher validator = new URLMatcher("*.xlogistx.io");

        assertFalse(validator.isValid(null));
        assertFalse(validator.isValid(""));
        assertFalse(validator.isValid("   "));
    }

    @Test
    public void testNullPattern() {
        assertThrows(NullPointerException.class, () -> new URLMatcher(null));
    }

    @Test
    public void testEmptyPattern() {
        assertThrows(IllegalArgumentException.class, () -> new URLMatcher(""));
        assertThrows(IllegalArgumentException.class, () -> new URLMatcher("   "));
    }

    @Test
    public void testIsValidInterface() {
        URLMatcher validator = new URLMatcher("*.xlogistx.io");

        // Test the Validator interface method
        assertTrue(validator.isValid("https://api.xlogistx.io:8080"));
        assertFalse(validator.isValid("https://other.com"));
    }

    @Test
    public void testGetters() {
        URLMatcher wildcardValidator = new URLMatcher("*.xlogistx.io");
        assertEquals("*.xlogistx.io", wildcardValidator.getPattern());
        assertTrue(wildcardValidator.isWildcardMode());

        URLMatcher strictValidator = new URLMatcher("xlogistx.io,www.xlogistx.io");
        assertEquals("xlogistx.io,www.xlogistx.io", strictValidator.getPattern());
        assertFalse(strictValidator.isWildcardMode());
    }

    @Test
    public void testUserInfoInUrl() {
        URLMatcher validator = new URLMatcher("*.xlogistx.io");

        // URL with user info should still work
        assertTrue(validator.isValid("https://user:pass@api.xlogistx.io:8080/path"));
        assertTrue(validator.isValid("ftp://user@xlogistx.io"));
    }

    @Test
    public void testToString() {
        URLMatcher validator = new URLMatcher("*.xlogistx.io");
        String str = validator.toString();

        assertTrue(str.contains("*.xlogistx.io"));
        assertTrue(str.contains("wildcardMode=true"));
    }

    @Test
    public void testVariousSchemes() {
        URLMatcher validator = new URLMatcher("*.xlogistx.io");

        assertTrue(validator.isValid("http://api.xlogistx.io"));
        assertTrue(validator.isValid("https://api.xlogistx.io"));
        assertTrue(validator.isValid("ftp://files.xlogistx.io"));
        assertTrue(validator.isValid("ws://socket.xlogistx.io"));
        assertTrue(validator.isValid("wss://socket.xlogistx.io"));
        assertTrue(validator.isValid("ssh://server.xlogistx.io"));
    }
}
