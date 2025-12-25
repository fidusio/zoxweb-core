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
package org.zoxweb.shared.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.GetNameValue;

import java.util.Date;

public class AppVersionDAOTest {

    @Test
    public void testDefaultConstructor() {
        AppVersionDAO appVersion = new AppVersionDAO();
        Assertions.assertNotNull(appVersion);
        Assertions.assertEquals(0, appVersion.getMajor());
        Assertions.assertEquals(0, appVersion.getMinor());
        Assertions.assertEquals(0, appVersion.getNano());
    }

    @Test
    public void testStringConstructorVersionOnly() {
        AppVersionDAO appVersion = new AppVersionDAO("1.2.3");
        Assertions.assertEquals(1, appVersion.getMajor());
        Assertions.assertEquals(2, appVersion.getMinor());
        Assertions.assertEquals(3, appVersion.getNano());
    }

    @Test
    public void testStringConstructorNameAndVersion() {
        AppVersionDAO appVersion = new AppVersionDAO("MyApp::1.2.3");
        Assertions.assertEquals("MyApp", appVersion.getName());
        Assertions.assertEquals(1, appVersion.getMajor());
        Assertions.assertEquals(2, appVersion.getMinor());
        Assertions.assertEquals(3, appVersion.getNano());
    }

    @Test
    public void testStringConstructorNameDescriptionAndVersion() {
        AppVersionDAO appVersion = new AppVersionDAO("MyApp::A test application::2.5.10");
        Assertions.assertEquals("MyApp", appVersion.getName());
        Assertions.assertEquals("A test application", appVersion.getDescription());
        Assertions.assertEquals(2, appVersion.getMajor());
        Assertions.assertEquals(5, appVersion.getMinor());
        Assertions.assertEquals(10, appVersion.getNano());
    }

    @Test
    public void testSetAndGetMajor() {
        AppVersionDAO appVersion = new AppVersionDAO();
        appVersion.setMajor(5);
        Assertions.assertEquals(5, appVersion.getMajor());

        appVersion.setMajor(100);
        Assertions.assertEquals(100, appVersion.getMajor());
    }

    @Test
    public void testSetAndGetMinor() {
        AppVersionDAO appVersion = new AppVersionDAO();
        appVersion.setMinor(0);
        Assertions.assertEquals(0, appVersion.getMinor());

        appVersion.setMinor(9);
        Assertions.assertEquals(9, appVersion.getMinor());
    }

    @Test
    public void testMinorValidationBelowRange() {
        AppVersionDAO appVersion = new AppVersionDAO();
        Assertions.assertThrows(IllegalArgumentException.class, () -> appVersion.setMinor(-1));
    }

    @Test
    public void testMinorValidationAboveRange() {
        AppVersionDAO appVersion = new AppVersionDAO();
        Assertions.assertThrows(IllegalArgumentException.class, () -> appVersion.setMinor(10));
    }

    @Test
    public void testSetAndGetNano() {
        AppVersionDAO appVersion = new AppVersionDAO();
        appVersion.setNano(0);
        Assertions.assertEquals(0, appVersion.getNano());

        appVersion.setNano(99);
        Assertions.assertEquals(99, appVersion.getNano());

        appVersion.setNano(50);
        Assertions.assertEquals(50, appVersion.getNano());
    }

    @Test
    public void testNanoValidationBelowRange() {
        AppVersionDAO appVersion = new AppVersionDAO();
        Assertions.assertThrows(IllegalArgumentException.class, () -> appVersion.setNano(-1));
    }

    @Test
    public void testNanoValidationAboveRange() {
        AppVersionDAO appVersion = new AppVersionDAO();
        Assertions.assertThrows(IllegalArgumentException.class, () -> appVersion.setNano(100));
    }

    @Test
    public void testToCanonicalID() {
        AppVersionDAO appVersion = new AppVersionDAO();
        appVersion.setName("TestApp");
        appVersion.setMajor(1);
        appVersion.setMinor(2);
        appVersion.setNano(3);

        Assertions.assertEquals("TestApp-1.2.3", appVersion.toCanonicalID());
    }

    @Test
    public void testToString() {
        AppVersionDAO appVersion = new AppVersionDAO();
        appVersion.setName("TestApp");
        appVersion.setMajor(2);
        appVersion.setMinor(0);
        appVersion.setNano(15);

        Assertions.assertEquals("TestApp-2.0.15", appVersion.toString());
    }

    @Test
    public void testSetAndGetReleaseDate() {
        AppVersionDAO appVersion = new AppVersionDAO();
        Date now = new Date();
        appVersion.setReleaseDate(now);

        Date retrieved = appVersion.getReleaseDate();
        Assertions.assertNotNull(retrieved);
        Assertions.assertEquals(now.getTime(), retrieved.getTime());
    }

    @Test
    public void testInvalidVersionFormat() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AppVersionDAO("1.2"));
    }

    @Test
    public void testInvalidVersionFormatTooMany() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AppVersionDAO("1.2.3.4"));
    }

    @Test
    public void testInvalidVersionFormatNonNumeric() {
        Assertions.assertThrows(NumberFormatException.class, () -> new AppVersionDAO("a.b.c"));
    }

    @Test
    public void testJsonSerialization() {
        AppVersionDAO appVersion = new AppVersionDAO();
        appVersion.setName("SerialApp");
        appVersion.setDescription("Test serialization");
        appVersion.setMajor(3);
        appVersion.setMinor(5);
        appVersion.setNano(25);
        appVersion.setReleaseDate(new Date());

        String json = GSONUtil.toJSONDefault(appVersion);
        Assertions.assertNotNull(json);
        System.out.println("JSON: " + json);

        AppVersionDAO deserialized = GSONUtil.fromJSON(json, AppVersionDAO.class);
        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals(appVersion.getName(), deserialized.getName());
        Assertions.assertEquals(appVersion.getDescription(), deserialized.getDescription());
        Assertions.assertEquals(appVersion.getMajor(), deserialized.getMajor());
        Assertions.assertEquals(appVersion.getMinor(), deserialized.getMinor());
        Assertions.assertEquals(appVersion.getNano(), deserialized.getNano());
    }

    @Test
    public void testMinorBoundaryValues() {
        AppVersionDAO appVersion = new AppVersionDAO();

        // Test boundary value 0
        appVersion.setMinor(0);
        Assertions.assertEquals(0, appVersion.getMinor());

        // Test boundary value 9
        appVersion.setMinor(9);
        Assertions.assertEquals(9, appVersion.getMinor());
    }

    @Test
    public void testNanoBoundaryValues() {
        AppVersionDAO appVersion = new AppVersionDAO();

        // Test boundary value 0
        appVersion.setNano(0);
        Assertions.assertEquals(0, appVersion.getNano());

        // Test boundary value 99
        appVersion.setNano(99);
        Assertions.assertEquals(99, appVersion.getNano());
    }

    @Test
    public void testCanonicalIDWithNullName() {
        AppVersionDAO appVersion = new AppVersionDAO();
        appVersion.setMajor(1);
        appVersion.setMinor(0);
        appVersion.setNano(0);

        String canonicalID = appVersion.toCanonicalID();
        Assertions.assertEquals("null-1.0.0", canonicalID);
    }

    @Test
    public void testStringConstructorWithZeroVersion() {
        AppVersionDAO appVersion = new AppVersionDAO("0.0.0");
        Assertions.assertEquals(0, appVersion.getMajor());
        Assertions.assertEquals(0, appVersion.getMinor());
        Assertions.assertEquals(0, appVersion.getNano());
    }

    @Test
    public void testStringConstructorWithMaxValidValues() {
        AppVersionDAO appVersion = new AppVersionDAO("TestApp::999.9.99");
        Assertions.assertEquals("TestApp", appVersion.getName());
        Assertions.assertEquals(999, appVersion.getMajor());
        Assertions.assertEquals(9, appVersion.getMinor());
        Assertions.assertEquals(99, appVersion.getNano());
    }

    @Test
    public void testToNameValue() {
        AppVersionDAO appVersion = new AppVersionDAO();
        appVersion.setName("MyApp");
        appVersion.setMajor(1);
        appVersion.setMinor(2);
        appVersion.setNano(3);

        GetNameValue<String> nameValue = appVersion.toNVVersion();
        Assertions.assertNotNull(nameValue);
        Assertions.assertEquals("VERSION", nameValue.getName());
        Assertions.assertEquals("MyApp-1.2.3", nameValue.getValue());
    }

    @Test
    public void testToNameValueFromStringConstructor() {
        AppVersionDAO appVersion = new AppVersionDAO("TestApp::Description::2.5.10");

        GetNameValue<String> nameValue = appVersion.toNVVersion();
        Assertions.assertNotNull(nameValue);
        Assertions.assertEquals("VERSION", nameValue.getName());
        Assertions.assertEquals("TestApp-2.5.10", nameValue.getValue());
    }
}
