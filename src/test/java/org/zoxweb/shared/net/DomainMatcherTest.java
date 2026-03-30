package org.zoxweb.shared.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DomainMatcherTest {

    // ==================== Exact Match Tests ====================

    @Test
    public void testExactMatch() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("xlogistx.io");
        assertTrue(dm.matches("xlogistx.io"));
    }

    @Test
    public void testExactMatchCaseInsensitive() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("XlogistX.IO");
        assertTrue(dm.matches("xlogistx.io"));
        assertTrue(dm.matches("XLOGISTX.IO"));
    }

    @Test
    public void testExactMatchNoFalsePositive() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("xlogistx.io");
        assertFalse(dm.matches("other.com"));
        assertFalse(dm.matches("sub.xlogistx.io"));
    }

    // ==================== Suffix (Wildcard) Match Tests ====================

    @Test
    public void testSuffixMatchSubdomain() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("*.xlogistx.io");
        assertTrue(dm.matches("api.xlogistx.io"));
        assertTrue(dm.matches("www.xlogistx.io"));
    }

    @Test
    public void testSuffixMatchDeepSubdomain() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("*.xlogistx.io");
        assertTrue(dm.matches("a.b.c.xlogistx.io"));
    }

    @Test
    public void testSuffixMatchesBareDomain() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("*.xlogistx.io");
        assertTrue(dm.matches("xlogistx.io"));
    }

    @Test
    public void testSuffixMatchCaseInsensitive() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("*.Example.COM");
        assertTrue(dm.matches("sub.example.com"));
    }

    // ==================== Glob Pattern Tests ====================

    @Test
    public void testGlobQuestionMark() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("api-?.example.com");
        assertTrue(dm.matches("api-1.example.com"));
        assertTrue(dm.matches("api-a.example.com"));
        assertFalse(dm.matches("api-12.example.com"));
    }

    @Test
    public void testGlobStarInMiddle() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("api*.example.com");
        assertTrue(dm.matches("api.example.com"));
        assertTrue(dm.matches("api-v2.example.com"));
        assertFalse(dm.matches("web.example.com"));
    }

    @Test
    public void testGlobMatchAll() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("*");
        assertTrue(dm.matches("anything.com"));
        assertTrue(dm.matches("a.b.c.d"));
    }

    @Test
    public void testGlobComplexPattern() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("*.*.example.com");
        assertTrue(dm.matches("a.b.example.com"));
        assertFalse(dm.matches("a.example.com"));
    }

    // ==================== Remove Tests ====================

    @Test
    public void testRemoveExactPattern() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("xlogistx.io");
        assertTrue(dm.removePattern("xlogistx.io"));
        assertFalse(dm.matches("xlogistx.io"));
        assertEquals(0, dm.size());
    }

    @Test
    public void testRemoveSuffixPattern() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("*.xlogistx.io");
        assertTrue(dm.removePattern("*.xlogistx.io"));
        assertFalse(dm.matches("api.xlogistx.io"));
        assertEquals(0, dm.size());
    }

    @Test
    public void testRemoveGlobPattern() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("api-?.example.com");
        assertTrue(dm.removePattern("api-?.example.com"));
        assertFalse(dm.matches("api-1.example.com"));
        assertEquals(0, dm.size());
    }

    @Test
    public void testRemoveNonExistentPattern() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("xlogistx.io");
        assertFalse(dm.removePattern("other.com"));
        assertEquals(1, dm.size());
    }

    // ==================== Size Tests ====================

    @Test
    public void testSizeEmpty() {
        DomainMatcher dm = new DomainMatcher();
        assertEquals(0, dm.size());
    }

    @Test
    public void testSizeMixedTiers() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("xlogistx.io");
        dm.addPattern("*.example.com");
        dm.addPattern("api-?.test.com");
        assertEquals(3, dm.size());
    }

    // ==================== Validation Tests ====================

    @Test
    public void testAddNullPattern() {
        DomainMatcher dm = new DomainMatcher();
        assertThrows(Exception.class, () -> dm.addPattern(null));
    }

    @Test
    public void testAddEmptyPattern() {
        DomainMatcher dm = new DomainMatcher();
        assertThrows(IllegalArgumentException.class, () -> dm.addPattern(""));
    }

    @Test
    public void testMatchNullDomain() {
        DomainMatcher dm = new DomainMatcher();
        assertThrows(Exception.class, () -> dm.matches(null));
    }

    @Test
    public void testRemoveNullPattern() {
        DomainMatcher dm = new DomainMatcher();
        assertThrows(Exception.class, () -> dm.removePattern(null));
    }

    @Test
    public void testRemoveEmptyPattern() {
        DomainMatcher dm = new DomainMatcher();
        assertThrows(IllegalArgumentException.class, () -> dm.removePattern(""));
    }

    // ==================== Multi-Pattern Tests ====================

    @Test
    public void testMultiplePatternsMatchCorrectly() {
        DomainMatcher dm = new DomainMatcher();
        dm.addPattern("xlogistx.io");
        dm.addPattern("*.example.com");
        dm.addPattern("api-?.test.com");

        assertTrue(dm.matches("xlogistx.io"));
        assertTrue(dm.matches("sub.example.com"));
        assertTrue(dm.matches("api-1.test.com"));
        assertFalse(dm.matches("other.org"));
    }

    @Test
    public void testNoMatchOnEmptyMatcher() {
        DomainMatcher dm = new DomainMatcher();
        assertFalse(dm.matches("anything.com"));
    }
}
