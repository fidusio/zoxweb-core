package org.zoxweb.shared.security;

import org.junit.jupiter.api.Test;

public class SShURITest {

    @Test
    public void passTest()
    {
        System.out.println(SShURI.parse("alice@192.168.1.10:2222"));     // IPv4
        System.out.println(SShURI.parse("bob@server.example.com:2022"));  // domain
        System.out.println(SShURI.parse("carol@[2001:db8::1]:2222"));    // IPv6
        System.out.println(SShURI.parse("dave@[fe80::abcd]"));           // IPv6, default port
        System.out.println(SShURI.parse("eve@host123"));
    }
}
