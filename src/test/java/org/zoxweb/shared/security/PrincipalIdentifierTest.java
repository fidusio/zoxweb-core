package org.zoxweb.shared.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrincipalIdentifierTest {


    @Test
    void testConstructor() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier();

        assertNotNull(principalIdentifier);
    }

    @Test
    void testOneArgConstructor() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier("test");

        assertNotNull(principalIdentifier);
        assertEquals("test", principalIdentifier.getPrincipalID());
    }

    @Test
    void testThreeArgConstructor() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier("test", "test1.com", "test2");

        assertNotNull(principalIdentifier);
        assertEquals("test", principalIdentifier.getPrincipalID());
        assertEquals("test1.com", principalIdentifier.getDomainID());
        assertEquals("test2", principalIdentifier.getAppID());
    }

    @Test
    void setPrincipalID() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier();
        principalIdentifier.setPrincipalID("test");
        assertEquals("test", principalIdentifier.getPrincipalID());
    }

    @Test
    void getPrincipalID() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier("test");
        String getPID = principalIdentifier.getPrincipalID();
        assertEquals("test", getPID);
    }

    @Test
    void setDomainAppID() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier();
        principalIdentifier.setDomainAppID("test.com", "test");
        assertEquals("test.com", principalIdentifier.getDomainID());
        assertEquals("test", principalIdentifier.getAppID());
    }

    @Test
    void getDomainID() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier("test", "test1.com", "test2");
        String getPID = principalIdentifier.getDomainID();
        assertEquals("test1.com", getPID);
    }

    @Test
    void setDomainID() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier();
        principalIdentifier.setDomainID("test.com");
        assertEquals("test.com", principalIdentifier.getDomainID());
    }

    @Test
    void negativeSetDomainID() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> principalIdentifier.setDomainID("test"));
        assertEquals("Invalid input: test", exception.getMessage());

    }

    @Test
    void getAppID() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier("test", "test1.com", "test2");
        String getPID = principalIdentifier.getAppID();
        assertEquals("test2", getPID);
    }

    @Test
    void setAppID() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> principalIdentifier.setAppID("test"));
        assertEquals("test can not be set when no domain exists", exception.getMessage());

        principalIdentifier.setDomainID("test1.com");
        principalIdentifier.setAppID("test1");

        assertEquals("test1", principalIdentifier.getAppID());

    }

    @Test
    void equalsTest() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier("test");
        PrincipalIdentifier principalIdentifier1 = new PrincipalIdentifier("test");
        PrincipalIdentifier principalIdentifier2 = new PrincipalIdentifier("test2");

        assertEquals(principalIdentifier, principalIdentifier);
        assertNotEquals(principalIdentifier, principalIdentifier2);
        assertEquals(principalIdentifier, principalIdentifier1);
        assertNotEquals(null, principalIdentifier);
        assertNotEquals("string", principalIdentifier);
    }

    @Test
    void hashTest() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier("test");
        PrincipalIdentifier empty = new PrincipalIdentifier();

        assertEquals("test".hashCode(), principalIdentifier.hashCode());
        assertEquals(empty.hashCode(), empty.hashCode());
    }

    @Test
    void hashCodeOverridesTest() {
        PrincipalIdentifier principal = new PrincipalIdentifier();
        System.out.println(principal.hashCode());
        String principalID = "test@gmail.com";
        principal.setPrincipalID(principalID);
        System.out.println(principal.hashCode());
        assertEquals(principalID.hashCode(), principal.hashCode());
        principal.setPrincipalID(null);
        System.out.println(principal.hashCode());

    }

}