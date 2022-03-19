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



import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.zoxweb.shared.util.SharedStringUtil;



public class FilterTypeTest {

    @Test
    public void testEmailFilterIsValid() {
        String[] validEmails = {"johnsmith@example.com ", "JohnSmith@example.com", "podbarista@podbarista.cafe", "podbarista@podbarista.coffe"};

        for (String email : validEmails) {
            assertTrue(FilterType.EMAIL.isValid(email));
        }

        String[] invalidEmails = {"johnsmith@example", "johnsmith", null,""};

        for (String email : invalidEmails) {
            assertFalse(FilterType.EMAIL.isValid(email));
        }
    }

    @Test
    public void testIntegerFilterIsValid() {
        String[] validIntegers = {"100", "5", "44005"};

        for (String value : validIntegers) {
            assertTrue(FilterType.INTEGER.isValid(value));
        }

        String[] invalidIntegers = {"1.0", "400.50"};

        for (String value : invalidIntegers) {
            assertFalse(FilterType.INTEGER.isValid(value));
        }
    }

    @Test
    public void testLongFilterIsValid() {
        String[] validLongs = {"12589461", "-100"};

        for (String value : validLongs) {
            assertTrue(FilterType.LONG.isValid(value));
        }

        String[] invalidLongs= {"1.0", "1,200,000", null, "23.587"};

        for (String value : invalidLongs) {
            assertFalse(FilterType.LONG.isValid(value));
        }
    }

    @Test
    public void testDoubleFilterIsValid() {
        String[] validDoubles = {"9.99", "4.0"};


        for (String value : validDoubles) {
            assertTrue(FilterType.DOUBLE.isValid(value));
        }

        String[] invalidDoubles = {"44..44"};

        for (String value : invalidDoubles) {
            assertFalse(FilterType.DOUBLE.isValid(value));
        }
    }

    @Test
    public void testPasswordFilterIsValid() {
        String[] validPasswords = {"Password14", "p1ssw2rdS", "Password1", "Password2014",
				"Password14Password14Password14Password14", "Password14Password14Password14Password14Password14", "Password14$"};

        for (String password : validPasswords) {
            assertTrue(FilterType.PASSWORD.isValid(password));
        }

        String[] invalidPasswords = {"p1ssw2rd", "password", "password14", "Password"};

        for (String password : invalidPasswords) {
            assertFalse(FilterType.PASSWORD.isValid(password));
        }
    }

    @Test
    public void testHiddenFilterIsValid() {
        String[] validHiddenTexts = {null, ""};

        for (String str : validHiddenTexts) {
            assertTrue(FilterType.HIDDEN.isValid(str));
        }

        String[] invalidHiddenTexts = {"https://www.yahoo.com "};

        for (String str : invalidHiddenTexts) {
            assertFalse(FilterType.HIDDEN.isValid(str));
        }
    }

    @Test
    public void testDomainIDFilterIsValid() {
        String[] validAccountIDs = {"www.yahoo.com", "yahoo.com", "1258058", "999", "20485"};

		for (String accountID : validAccountIDs) {
            assertTrue(FilterType.DOMAIN_ACCOUNT_ID.isValid(accountID));
        }

        String[] invalidAccountIDs = {"https://www.yahoo.com ", "http://.www.yahoo.com/", "www.yahoo.com/",
                "yahoo.com/", "https://10.0.1.1/marwan?param=1&p1=2"};

        for (String accountID : invalidAccountIDs) {
            assertFalse(FilterType.DOMAIN_ACCOUNT_ID.isValid(accountID));
        }
    }

    @Test
    public void testURLFilterIsValid() {
        String[] validURLs = {"https://www.google.com ", "https://10.0.1.1/hello_world?param=1&p1=2"};

        for (String url : validURLs) {
            assertTrue(FilterType.URL.isValid(url));
        }

        String[] invalidURLs = {"http://.www.google.com/", "www.google.com/", "google.com/", "google"};

        for (String url : invalidURLs) {
            assertFalse(FilterType.URL.isValid(url));
        }
    }

    @Test
    public void testCanonicalFilenameFilterIsValid() {
        String[] validFilenames = {"/", "/Users/johnsmith/Desktop"};

        for (String filename : validFilenames) {
            System.out.println("File Name: " + filename + " Filtered: \"" + CanonicalFilenameFilter.SINGLETON.validate(filename) + "\"");
            assertTrue(CanonicalFilenameFilter.SINGLETON.isValid(filename));
        }

        String[] invalidFilenames = {null, "/////", "\\file\\hello//world/123;", "/////ta", "start///////", ""};

        System.out.println("Invalid Canonical File Names:");

        for (String filename : invalidFilenames) {
            System.out.println("File Name: " + filename + " Filtered: \"" + CanonicalFilenameFilter.SINGLETON.validate(filename) + "\"");
            assertFalse(CanonicalFilenameFilter.SINGLETON.isValid(filename));
		}
    }

    @Test
    public void testFilters() {
        String test = "testuser@example.com";

		for (FilterType ft : FilterType.values()) {
			System.out.println(ft.toCanonicalID() + " " + ft.isValid(test));
		}

		String macs[] = {
			"00:11:22:33:44:55",
			"01-23-45-67-89-0A",
			"AA.BB.CC.DD.EE.FF"
		};

		for (String mac: macs) {
			String str = SharedStringUtil.filterString(mac, new String[]{"-", ":", "."});
			System.out.println(str);
			byte[] address = SharedStringUtil.hexToBytes(str);
			System.out.println(SharedStringUtil.bytesToHex(address));
		}
    }

}