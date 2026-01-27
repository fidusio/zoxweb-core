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
package org.zoxweb.shared.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SecTag} class.
 */
public class SecTagTest {

    @Test
    public void testConstructorWithProviderAndTagID() {
        SecTag tag = new SecTag("provider", "tagId");

        // providerID is case-sensitive (not uppercased)
        assertEquals("provider", tag.getProviderID());
        // tagID and tagValue are uppercased
        assertEquals("TAGID", tag.getTagID());
        assertEquals("TAGID", tag.getTagValue());
    }

    @Test
    public void testConstructorWithProviderAndTagIDEnum() {
        SecTag tag = new SecTag("myProvider", SecTag.TagID.TLS);

        // providerID preserves case
        assertEquals("myProvider", tag.getProviderID());
        assertEquals("TLS", tag.getTagID());
        assertEquals("TLS", tag.getTagValue());
    }

    @Test
    public void testConstructorWithProviderTagIDEnumAndValue() {
        SecTag tag = new SecTag("myProvider", SecTag.TagID.X509, "customValue");

        // providerID preserves case
        assertEquals("myProvider", tag.getProviderID());
        assertEquals("X509", tag.getTagID());
        assertEquals("CUSTOMVALUE", tag.getTagValue());
    }

    @Test
    public void testConstructorWithAllStrings() {
        SecTag tag = new SecTag("provider", "tagId", "tagValue");

        // providerID preserves case
        assertEquals("provider", tag.getProviderID());
        // tagID and tagValue are uppercased
        assertEquals("TAGID", tag.getTagID());
        assertEquals("TAGVALUE", tag.getTagValue());
    }

    @Test
    public void testCaseConversion() {
        SecTag tag = new SecTag("LowerCase", "MixedCase", "UPPERCASE");

        // providerID preserves original case
        assertEquals("LowerCase", tag.getProviderID());
        // tagID and tagValue are uppercased
        assertEquals("MIXEDCASE", tag.getTagID());
        assertEquals("UPPERCASE", tag.getTagValue());
    }

    @Test
    public void testKey() {
        SecTag tag = new SecTag("provider", "tagId", "tagValue");
        String key = tag.key();

        // Key format: providerID:TAGID (providerID preserves case, tagID uppercased)
        assertEquals("provider:TAGID", key);
    }

    @Test
    public void testStaticKeyWithStrings() {
        String key = SecTag.key("provider", "tagId");

        // providerID preserves case, tagID is uppercased
        assertEquals("provider:TAGID", key);
    }

    @Test
    public void testStaticKeyWithGetName() {
        String key = SecTag.key("provider", SecTag.TagID.TLS);

        // providerID preserves case
        assertEquals("provider:TLS", key);
    }

    @Test
    public void testToString() {
        SecTag tag = new SecTag("provider", "tagId", "tagValue");
        String str = tag.toString();

        // Format: providerID:TAGID:TAGVALUE
        assertEquals("provider:TAGID:TAGVALUE", str);
    }

    @Test
    public void testTagIDEnum() {
        assertEquals("TLS", SecTag.TagID.TLS.getName());
        assertEquals("X509", SecTag.TagID.X509.getName());
        assertEquals("X500", SecTag.TagID.X500.getName());
    }

    @Test
    public void testSunJsseConstant() {
        assertEquals("SunJSSE", SecTag.SUN_JSSE);
    }

    @Test
    public void testNullProviderIDThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            new SecTag(null, "tagId", "tagValue");
        });
    }

    @Test
    public void testNullTagIDThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            new SecTag("provider", (String) null, "tagValue");
        });
    }

    @Test
    public void testNullTagValueThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            new SecTag("provider", "tagId", null);
        });
    }

    @Test
    public void testRegistrarAddAndLookup() {
        SecTag tag = new SecTag("testProvider", SecTag.TagID.TLS);
        String key = tag.key();
        SecTag.REGISTRAR.register(key, tag);

        SecTag retrieved = SecTag.REGISTRAR.lookup(key);

        assertNotNull(retrieved);
        assertEquals(tag.getProviderID(), retrieved.getProviderID());
        assertEquals(tag.getTagID(), retrieved.getTagID());
        assertEquals(tag.getTagValue(), retrieved.getTagValue());
    }

    @Test
    public void testToKeyDecoder() {
        SecTag tag = new SecTag("decoderProvider", "decoderTag", "decoderValue");
        String decoded = SecTag.ToKey.decode(tag);

        // providerID preserves case, tagID is uppercased
        assertEquals("decoderProvider:DECODERTAG", decoded);
    }

    @Test
    public void testMultipleTagsWithDifferentProviders() {
        SecTag tag1 = new SecTag("provider1", SecTag.TagID.TLS);
        SecTag tag2 = new SecTag("provider2", SecTag.TagID.TLS);

        assertNotEquals(tag1.key(), tag2.key());
        assertEquals("provider1:TLS", tag1.key());
        assertEquals("provider2:TLS", tag2.key());
    }

    @Test
    public void testMultipleTagsWithDifferentTagIDs() {
        SecTag tag1 = new SecTag("provider", SecTag.TagID.TLS);
        SecTag tag2 = new SecTag("provider", SecTag.TagID.X509);

        assertNotEquals(tag1.key(), tag2.key());
        assertEquals("provider:TLS", tag1.key());
        assertEquals("provider:X509", tag2.key());
    }

    @Test
    public void testLookupNonExistentKeyReturnsNull() {
        SecTag retrieved = SecTag.REGISTRAR.lookup("NON_EXISTENT:KEY");

        assertNull(retrieved);
    }

    @Test
    public void testLookupUsingStaticKeyMethod() {
        SecTag tag = new SecTag("lookupProvider", SecTag.TagID.X509);
        SecTag.REGISTRAR.registerValue(tag);

        // Lookup using static key method with strings
        SecTag retrieved = SecTag.REGISTRAR.lookup(SecTag.key("lookupProvider", "X509"));

        assertNotNull(retrieved);
        // providerID preserves case
        assertEquals("lookupProvider", retrieved.getProviderID());
        assertEquals("X509", retrieved.getTagID());
    }

    @Test
    public void testLookupUsingStaticKeyMethodWithEnum() {
        SecTag tag = new SecTag("enumLookup", SecTag.TagID.X500);
        SecTag.REGISTRAR.register(tag.key(), tag);

        // Lookup using static key method with GetName (enum)
        SecTag retrieved = SecTag.REGISTRAR.lookup(SecTag.key("enumLookup", SecTag.TagID.X500));

        assertNotNull(retrieved);
        // providerID preserves case
        assertEquals("enumLookup", retrieved.getProviderID());
        assertEquals("X500", retrieved.getTagID());
    }

    @Test
    public void testLookupMultipleRegisteredTags() {
        SecTag tag1 = new SecTag("multiProvider", SecTag.TagID.TLS, "value1");
        SecTag tag2 = new SecTag("multiProvider", SecTag.TagID.X509, "value2");
        SecTag tag3 = new SecTag("otherProvider", SecTag.TagID.TLS, "value3");

        SecTag.REGISTRAR.register(tag1.key(), tag1);
        SecTag.REGISTRAR.register(tag2.key(), tag2);
        SecTag.REGISTRAR.register(tag3.key(), tag3);

        SecTag retrieved1 = SecTag.REGISTRAR.lookup(tag1.key());
        SecTag retrieved2 = SecTag.REGISTRAR.lookup(tag2.key());
        SecTag retrieved3 = SecTag.REGISTRAR.lookup(tag3.key());

        assertNotNull(retrieved1);
        assertNotNull(retrieved2);
        assertNotNull(retrieved3);

        assertEquals("VALUE1", retrieved1.getTagValue());
        assertEquals("VALUE2", retrieved2.getTagValue());
        assertEquals("VALUE3", retrieved3.getTagValue());
    }

    @Test
    public void testProviderIDIsCaseSensitive() {
        SecTag tag1 = new SecTag("CaseTest", "TagName", "TagVal");
        SecTag tag2 = new SecTag("casetest", "TagName", "TagVal");

        // Different case in providerID produces different keys
        assertNotEquals(tag1.key(), tag2.key());
        assertEquals("CaseTest:TAGNAME", tag1.key());
        assertEquals("casetest:TAGNAME", tag2.key());
    }

    @Test
    public void testLookupWithGeneratedKeyMatchesDirectLookup() {
        SecTag tag = new SecTag("keyMatch", SecTag.TagID.TLS);
        String generatedKey = SecTag.ToKey.decode(tag);
        SecTag.REGISTRAR.registerValue(tag);

        // Lookup using instance key() method should match
        SecTag retrieved = SecTag.REGISTRAR.lookup(tag.key());

        assertNotNull(retrieved);
        assertSame(tag, retrieved);
    }

    @Test
    public void testStaticLookupWithTagIDEnum() {
        SecTag tag = new SecTag("staticLookup", SecTag.TagID.TLS);
        SecTag.REGISTRAR.registerValue(tag);

        SecTag retrieved = SecTag.lookup("staticLookup", SecTag.TagID.TLS);

        assertNotNull(retrieved);
        assertEquals("staticLookup", retrieved.getProviderID());
        assertEquals("TLS", retrieved.getTagID());
    }

    @Test
    public void testStaticLookupWithStringTagID() {
        SecTag tag = new SecTag("stringLookup", "customTag", "customValue");
        SecTag.REGISTRAR.registerValue(tag);

        SecTag retrieved = SecTag.lookup("stringLookup", "customTag");

        assertNotNull(retrieved);
        assertEquals("stringLookup", retrieved.getProviderID());
        assertEquals("CUSTOMTAG", retrieved.getTagID());
    }

    @Test
    public void testLookupTagValueWithTagIDEnum() {
        SecTag tag = new SecTag("valueProvider", SecTag.TagID.X509, "certValue");
        SecTag.REGISTRAR.registerValue(tag);

        String tagValue = SecTag.lookupTagValue("valueProvider", SecTag.TagID.X509);

        assertEquals("CERTVALUE", tagValue);
    }

    @Test
    public void testLookupTagValueWithStringTagID() {
        SecTag tag = new SecTag("strValueProvider", "myTag", "myValue");
        SecTag.REGISTRAR.registerValue(tag);

        String tagValue = SecTag.lookupTagValue("strValueProvider", "myTag");

        assertEquals("MYVALUE", tagValue);
    }

    @Test
    public void testLookupTagValueReturnsNullWhenNotFound() {
        String tagValue = SecTag.lookupTagValue("nonExistent", SecTag.TagID.TLS);

        assertNull(tagValue);
    }

    @Test
    public void testLookupWithNullProviderDefaultsToSunJSSE() {
        SecTag tag = new SecTag(SecTag.SUN_JSSE, "nullTest", "defaultValue");
        SecTag.REGISTRAR.registerValue(tag);

        // When providerID is null in lookup(String, String), it defaults to SUN_JSSE
        SecTag retrieved = SecTag.lookup(null, "nullTest");

        assertNotNull(retrieved);
        assertEquals(SecTag.SUN_JSSE, retrieved.getProviderID());
    }

    @Test
    public void testLookupTagValueWithNullProviderDefaultsToSunJSSE() {
        SecTag tag = new SecTag(SecTag.SUN_JSSE, "sunTag", "sunValue");
        SecTag.REGISTRAR.registerValue(tag);

        // When providerID is null, lookupTagValue defaults to SUN_JSSE
        String tagValue = SecTag.lookupTagValue(null, "sunTag");

        assertEquals("SUNVALUE", tagValue);
    }

    @Test
    public void testLookupTagValueWithNullProviderAndEnumDefaultsToSunJSSE() {
        SecTag tag = new SecTag(SecTag.SUN_JSSE, SecTag.TagID.X500, "enumSunValue");
        SecTag.REGISTRAR.registerValue(tag);

        // When providerID is null, lookupTagValue with enum also defaults to SUN_JSSE
        String tagValue = SecTag.lookupTagValue(null, SecTag.TagID.X500);

        assertEquals("ENUMSUNVALUE", tagValue);
    }

}