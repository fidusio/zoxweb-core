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

/**
 * FilterType.DOMAIN validates a bare hostname, lower-cases it and strips a leading
 * www label. It never computes a registrable domain and never coerces a URL or an
 * email address into a domain.
 */
public class DomainFilterTest {

    @Test
    public void validatedValueIsReturnedAsGiven() {
        String[][] cases = {
                {"zoxweb.com", "zoxweb.com"},
                {"admin.zoxweb.com", "admin.zoxweb.com"},
                {"api.v2.xlogistx.io", "api.v2.xlogistx.io"},
                {"zoxweb.com.lb", "zoxweb.com.lb"},
                {"example.co.uk", "example.co.uk"},
                {"google.io", "google.io"},
                {"website.fr", "website.fr"},
                {"example.photography", "example.photography"},
                {"xn--bcher-kva.example", "xn--bcher-kva.example"},
                {"a-b.c-d.org", "a-b.c-d.org"},
        };
        for (String[] c : cases) {
            assertTrue(FilterType.DOMAIN.isValid(c[0]), c[0]);
            assertEquals(c[1], FilterType.DOMAIN.validate(c[0]), c[0]);
        }
    }

    @Test
    public void lowerCasesAndTrims() {
        assertEquals("zoxweb.com", FilterType.DOMAIN.validate("  ZoxWeb.COM  "));
        assertEquals("admin.zoxweb.com", FilterType.DOMAIN.validate("Admin.ZoxWeb.Com"));
    }

    @Test
    public void stripsOnlyALeadingWwwLabel() {
        assertEquals("zoxweb.com", FilterType.DOMAIN.validate("www.zoxweb.com"));
        assertEquals("zoxweb.com", FilterType.DOMAIN.validate("www2.zoxweb.com"));
        assertEquals("admin.zoxweb.com", FilterType.DOMAIN.validate("www.admin.zoxweb.com"));
        // a label that merely starts with www is a real label
        assertEquals("wwwhost.zoxweb.com", FilterType.DOMAIN.validate("wwwhost.zoxweb.com"));
        // www in the middle is untouched
        assertEquals("a.www.zoxweb.com", FilterType.DOMAIN.validate("a.www.zoxweb.com"));
    }

    @Test
    public void rejectsUrlsAndEmails() {
        String[] values = {
                "https://www.zoxweb.com/welcome",
                "http://www.zoxweb.com/main?&t=20&f=52",
                "http://www.admin.zoxweb.com",
                "www.zoxweb.com/",
                "bob@zoxweb.com",
        };
        for (String val : values) {
            assertFalse(FilterType.DOMAIN.isValid(val), val);
            assertThrows(IllegalArgumentException.class, () -> FilterType.DOMAIN.validate(val), val);
        }
    }

    @Test
    public void rejectsMalformedHostnames() {
        StringBuilder longLabel = new StringBuilder();
        for (int i = 0; i < 64; i++) longLabel.append('a');
        String[] values = {
                "zoxweb",                       // single label
                "www.com",                      // single label once www is stripped
                "-bad.com",                     // hyphen at label start
                "bad-.com",                     // hyphen at label end
                "zoxweb.c",                     // TLD too short
                "zoxweb.123",                   // numeric TLD
                "zoxweb..com",                  // empty label
                ".zoxweb.com",                  // leading dot
                "zoxweb.com.",                  // trailing dot
                "zox web.com",                  // space
                "zoxweb_x.com",                 // underscore
                longLabel + ".com",             // 64-char label
                "",
                "   ",
        };
        for (String val : values) {
            assertFalse(FilterType.DOMAIN.isValid(val), val);
        }
        assertFalse(FilterType.DOMAIN.isValid(null));
    }

    @Test
    public void rejectsHostnameOver253Chars() {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 250) sb.append("abcdefghij.");
        sb.append("com");
        assertTrue(sb.length() > 253);
        assertFalse(FilterType.DOMAIN.isValid(sb.toString()));
    }

    @Test
    public void emailDomainPartFollowsTheSameRule() {
        assertTrue(FilterType.EMAIL.isValid("bob@website.fr"));
        assertTrue(FilterType.EMAIL.isValid("bob@example.photography"));
        assertTrue(FilterType.EMAIL.isValid("Bob@Example.COM"));
        assertEquals("bob@example.com", FilterType.EMAIL.validate("Bob@Example.COM"));
        assertFalse(FilterType.EMAIL.isValid("bob@zoxweb"));
        assertFalse(FilterType.EMAIL.isValid("bob@-bad.com"));
    }
}
