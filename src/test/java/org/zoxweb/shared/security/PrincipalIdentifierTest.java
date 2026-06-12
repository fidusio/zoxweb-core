package org.zoxweb.shared.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrincipalIdentifierTest {

    // ---------- Construction ----------

    @Test
    void defaultConstructorHasNullFields() {
        PrincipalIdentifier pi = new PrincipalIdentifier();

        assertNotNull(pi);
        assertNull(pi.getPrincipalID());
        assertNull(pi.getDomainID());
        assertNull(pi.getAppID());
    }

    @Test
    void oneArgConstructorSetsOnlyPrincipalID() {
        PrincipalIdentifier pi = new PrincipalIdentifier("test");

        assertEquals("test", pi.getPrincipalID());
        assertNull(pi.getDomainID());
        assertNull(pi.getAppID());
    }

    @Test
    void threeArgConstructorSetsAllFields() {
        PrincipalIdentifier pi = new PrincipalIdentifier("test", "test1.com", "test2");

        assertEquals("test", pi.getPrincipalID());
        assertEquals("test1.com", pi.getDomainID());
        assertEquals("test2", pi.getAppID());
    }

    @Test
    void threeArgConstructorRejectsInvalidDomain() {
        assertThrows(IllegalArgumentException.class,
                () -> new PrincipalIdentifier("test", "notADomain", "app"));
    }

    @Test
    void threeArgConstructorRejectsInvalidAppName() {
        assertThrows(IllegalArgumentException.class,
                () -> new PrincipalIdentifier("test", "test.com", "bad app!"));
    }

    // ---------- PrincipalID ----------

    @Test
    void setAndGetPrincipalID() {
        PrincipalIdentifier pi = new PrincipalIdentifier();
        pi.setPrincipalID("test");
        assertEquals("test", pi.getPrincipalID());
    }

    @Test
    void setPrincipalIDOverwritesPreviousValue() {
        PrincipalIdentifier pi = new PrincipalIdentifier("first");
        pi.setPrincipalID("second");
        assertEquals("second", pi.getPrincipalID());
    }

    @Test
    void setPrincipalIDToNullClearsValue() {
        PrincipalIdentifier pi = new PrincipalIdentifier("test");
        pi.setPrincipalID(null);
        assertNull(pi.getPrincipalID());
    }

    // ---------- DomainID ----------

    @Test
    void setAndGetDomainID() {
        PrincipalIdentifier pi = new PrincipalIdentifier();
        pi.setDomainID("test.com");
        assertEquals("test.com", pi.getDomainID());
    }

    @Test
    void setDomainIDRejectsInvalidValue() {
        PrincipalIdentifier pi = new PrincipalIdentifier();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pi.setDomainID("test"));
        assertEquals("Invalid input: test", ex.getMessage());
    }

    @Test
    void setDomainIDIsIndependentOfPrincipalID() {
        PrincipalIdentifier pi = new PrincipalIdentifier("principal");
        pi.setDomainID("example.com");

        assertEquals("principal", pi.getPrincipalID());
        assertEquals("example.com", pi.getDomainID());
    }

    // ---------- AppID ----------

    @Test
    void setAppIDWithoutDomainThrows() {
        PrincipalIdentifier pi = new PrincipalIdentifier();
        assertThrows(IllegalArgumentException.class, () -> pi.setAppID("test"));
    }

    @Test
    void setAppIDLowercasesValue() {
        PrincipalIdentifier pi = new PrincipalIdentifier();
        pi.setDomainID("domain-test.com");
        pi.setAppID("AppID");

        assertEquals("appid", pi.getAppID());
    }

    @Test
    void setAppIDRejectsNonAlphanumeric() {
        PrincipalIdentifier pi = new PrincipalIdentifier();
        pi.setDomainID("test.com");

        assertThrows(IllegalArgumentException.class, () -> pi.setAppID("app id"));
        assertThrows(IllegalArgumentException.class, () -> pi.setAppID("app!"));
    }

    // ---------- Domain + App ----------

    @Test
    void setDomainAppIDSetsBothFields() {
        PrincipalIdentifier pi = new PrincipalIdentifier();
        pi.setDomainAppID("test.com", "test");

        assertEquals("test.com", pi.getDomainID());
        assertEquals("test", pi.getAppID());
    }

    @Test
    void getDomainAppIDReturnsCanonicalForm() {
        PrincipalIdentifier pi = new PrincipalIdentifier("user", "test.com", "myApp");
        String canonical = pi.getDomainAppID();

        assertNotNull(canonical);
        assertTrue(canonical.toLowerCase().contains("test.com"));
        assertTrue(canonical.toLowerCase().contains("myapp"));
    }

    // ---------- equals ----------

    @Test
    void equalsIsSymmetric() {
        PrincipalIdentifier a = new PrincipalIdentifier("test");
        PrincipalIdentifier b = new PrincipalIdentifier("test");

        assertEquals(a, b);
        assertEquals(b, a);
    }

    @Test
    void equalsReturnsFalseForDifferentPrincipalID() {
        PrincipalIdentifier a = new PrincipalIdentifier("test");
        PrincipalIdentifier b = new PrincipalIdentifier("other");

        assertNotEquals(a, b);
    }

    @Test
    void equalsIsCaseInsensitiveOnPrincipalID() {
        PrincipalIdentifier lower = new PrincipalIdentifier("user@test.com");
        PrincipalIdentifier upper = new PrincipalIdentifier("USER@TEST.COM");

        assertEquals(lower, upper);
    }

    @Test
    void equalsIgnoresDomainAndApp() {
        PrincipalIdentifier a = new PrincipalIdentifier("test", "a.com", "app1");
        PrincipalIdentifier b = new PrincipalIdentifier("test", "b.com", "app2");

        assertEquals(a, b);
    }

    @Test
    void equalsReturnsTrueWhenBothPrincipalIDsNull() {
        PrincipalIdentifier a = new PrincipalIdentifier();
        PrincipalIdentifier b = new PrincipalIdentifier();

        assertEquals(a, b);
    }

    @Test
    @SuppressWarnings({"SimplifiableAssertion", "ConstantValue"})
    void equalsReturnsFalseForNull() {
        PrincipalIdentifier pi = new PrincipalIdentifier("test");
        assertFalse(pi.equals(null));
    }

    @Test
    @SuppressWarnings({"SimplifiableAssertion", "EqualsBetweenInconvertibleTypes"})
    void equalsReturnsFalseForUnrelatedType() {
        PrincipalIdentifier pi = new PrincipalIdentifier("test");
        Object stringValue = "test";
        Object intValue = 42;

        assertFalse(pi.equals(stringValue));
        assertFalse(pi.equals(intValue));
    }

    // ---------- hashCode ----------

    @Test
    void hashCodeMatchesPrincipalIDHash() {
        PrincipalIdentifier pi = new PrincipalIdentifier("test@gmail.com");
        assertEquals("test@gmail.com".hashCode(), pi.hashCode());
    }

    @Test
    void hashCodeOfEmptyIsZero() {
        PrincipalIdentifier pi = new PrincipalIdentifier();
        assertEquals(0, pi.hashCode());
    }

    @Test
    void hashCodeIsConsistentAcrossCalls() {
        PrincipalIdentifier pi = new PrincipalIdentifier("test");
        int first = pi.hashCode();
        int second = pi.hashCode();
        assertEquals(first, second);
    }

    @Test
    void equalObjectsHaveEqualHashCodes() {
        PrincipalIdentifier a = new PrincipalIdentifier("test");
        PrincipalIdentifier b = new PrincipalIdentifier("test");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void hashCodeChangesAfterPrincipalIDChange() {
        PrincipalIdentifier pi = new PrincipalIdentifier();
        int empty = pi.hashCode();

        pi.setPrincipalID("test@gmail.com");
        assertNotEquals(empty, pi.hashCode());

        pi.setPrincipalID(null);
        assertEquals(empty, pi.hashCode());
    }
}
