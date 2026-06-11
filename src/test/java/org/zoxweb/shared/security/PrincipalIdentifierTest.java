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
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier("test", "test1", "test2");

        assertNotNull(principalIdentifier);
        assertEquals("test", principalIdentifier.getPrincipalID());
        assertEquals("test1", principalIdentifier.getDomainID());
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
    void getDomainID() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier("test", "test1", "test2");
        String getPID = principalIdentifier.getDomainID();
        assertEquals("test1", getPID);
    }

    @Test
    void setDomainID() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier();
        principalIdentifier.setDomainID("test");
        assertEquals("test", principalIdentifier.getDomainID());
    }

    @Test
    void getAppID() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier("test", "test1", "test2");
        String getPID = principalIdentifier.getAppID();
        assertEquals("test2", getPID);
    }

    @Test
    void setAppID() {
        PrincipalIdentifier principalIdentifier = new PrincipalIdentifier();
        principalIdentifier.setAppID("test");
        assertEquals("test", principalIdentifier.getAppID());
    }

}