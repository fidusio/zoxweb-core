package org.zoxweb.shared.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IPAddressTest {

    // ==================== Constructor Tests ====================

    @Test
    public void testDefaultConstructor() {
        IPAddress ip = new IPAddress();
        assertNull(ip.getInetAddress());
        assertEquals(0, ip.getPort());
        assertEquals(0, ip.getBacklog());
        assertNull(ip.getProxyType());
    }

    @Test
    public void testConstructorWithAddressAndPort() {
        IPAddress ip = new IPAddress("192.168.1.1", 8080);
        assertEquals("192.168.1.1", ip.getInetAddress());
        assertEquals(8080, ip.getPort());
        assertEquals(128, ip.getBacklog());
        assertNull(ip.getProxyType());
    }

    @Test
    public void testConstructorWithAddressPortAndProxyType() {
        IPAddress ip = new IPAddress("localhost", 443, ProxyType.SOCKS);
        assertEquals("localhost", ip.getInetAddress());
        assertEquals(443, ip.getPort());
        assertEquals(128, ip.getBacklog());
        assertEquals(ProxyType.SOCKS, ip.getProxyType());
    }

    @Test
    public void testConstructorWithAllParams() {
        IPAddress ip = new IPAddress("example.com", 8443, 256, ProxyType.HTTP);
        assertEquals("example.com", ip.getInetAddress());
        assertEquals(8443, ip.getPort());
        assertEquals(256, ip.getBacklog());
        assertEquals(ProxyType.HTTP, ip.getProxyType());
    }

    @Test
    public void testStringConstructorWithPortOnly() {
        IPAddress ip = new IPAddress("8080");
        assertNull(ip.getInetAddress());
        assertEquals(8080, ip.getPort());
    }

    @Test
    public void testStringConstructorWithAddressOnly() {
        IPAddress ip = new IPAddress("localhost");
        assertEquals("localhost", ip.getInetAddress());
        assertEquals(-1, ip.getPort());
    }

    @Test
    public void testStringConstructorWithAddressAndPort() {
        IPAddress ip = new IPAddress("192.168.1.1:9090");
        assertEquals("192.168.1.1", ip.getInetAddress());
        assertEquals(9090, ip.getPort());
    }

    @Test
    public void testStringConstructorWithAddressPortAndProxyType() {
        IPAddress ip = new IPAddress("proxy.example.com:3128:HTTP");
        assertEquals("proxy.example.com", ip.getInetAddress());
        assertEquals(3128, ip.getPort());
        assertEquals(ProxyType.HTTP, ip.getProxyType());
    }

    @Test
    public void testStringConstructorInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            new IPAddress("a:b:c:d");
        });
    }

    // ==================== Setter/Getter Tests ====================

    @Test
    public void testSetAndGetInetAddress() {
        IPAddress ip = new IPAddress();
        ip.setInetAddress("10.0.0.1");
        assertEquals("10.0.0.1", ip.getInetAddress());
    }

    @Test
    public void testSetAndGetPort() {
        IPAddress ip = new IPAddress();
        ip.setPort(443);
        assertEquals(443, ip.getPort());
    }

    @Test
    public void testSetPortMinusOne() {
        IPAddress ip = new IPAddress();
        ip.setPort(-1);
        assertEquals(-1, ip.getPort());
    }

    @Test
    public void testSetInvalidPort() {
        IPAddress ip = new IPAddress();
        assertThrows(IllegalArgumentException.class, () -> {
            ip.setPort(-2);
        });
    }

    @Test
    public void testSetAndGetBacklog() {
        IPAddress ip = new IPAddress();
        ip.setBacklog(512);
        assertEquals(512, ip.getBacklog());
    }

    @Test
    public void testSetInvalidBacklog() {
        IPAddress ip = new IPAddress();
        assertThrows(IllegalArgumentException.class, () -> {
            ip.setBacklog(-2);
        });
    }

    @Test
    public void testSetProxyTypeEnum() {
        IPAddress ip = new IPAddress();
        ip.setProxyType(ProxyType.SOCKS);
        assertEquals(ProxyType.SOCKS, ip.getProxyType());
    }

    @Test
    public void testSetProxyTypeString() {
        IPAddress ip = new IPAddress();
        ip.setProxyType("HTTP");
        assertEquals(ProxyType.HTTP, ip.getProxyType());
    }

    // ==================== equals/hashCode Tests ====================

    @Test
    public void testEqualsWithSameAddressAndPort() {
        IPAddress ip1 = new IPAddress("localhost", 8080);
        IPAddress ip2 = new IPAddress("localhost", 8080);
        assertEquals(ip1, ip2);
    }

    @Test
    public void testEqualsCaseInsensitive() {
        IPAddress ip1 = new IPAddress("LocalHost", 8080);
        IPAddress ip2 = new IPAddress("localhost", 8080);
        assertEquals(ip1, ip2);
    }

    @Test
    public void testNotEqualsDifferentPort() {
        IPAddress ip1 = new IPAddress("localhost", 8080);
        IPAddress ip2 = new IPAddress("localhost", 9090);
        assertNotEquals(ip1, ip2);
    }

    @Test
    public void testNotEqualsDifferentAddress() {
        IPAddress ip1 = new IPAddress("localhost", 8080);
        IPAddress ip2 = new IPAddress("127.0.0.1", 8080);
        assertNotEquals(ip1, ip2);
    }

    @Test
    public void testHashCodeConsistency() {
        IPAddress ip1 = new IPAddress("localhost", 8080);
        IPAddress ip2 = new IPAddress("localhost", 8080);
        assertEquals(ip1.hashCode(), ip2.hashCode());
    }

    // ==================== toString Tests ====================

    @Test
    public void testToString() {
        IPAddress ip = new IPAddress("example.com", 443);
        assertEquals("example.com:443", ip.toString());
    }

    // ==================== parse Tests ====================

    @Test
    public void testParseSimple() {
        IPAddress ip = IPAddress.parse("google.com:80");
        assertEquals("google.com", ip.getInetAddress());
        assertEquals(80, ip.getPort());
    }

    @Test
    public void testParseWithProxyType() {
        IPAddress ip = IPAddress.parse("proxy.com:8080", ProxyType.SOCKS);
        assertEquals("proxy.com", ip.getInetAddress());
        assertEquals(8080, ip.getPort());
        assertEquals(ProxyType.SOCKS, ip.getProxyType());
    }

    @Test
    public void testParseWithExistingProxyTypeInString() {
        // When the proxy type is already in the string, it is used (not the parameter)
        IPAddress ip = IPAddress.parse("proxy.com:8080:SOCKS", ProxyType.SOCKS);
        assertEquals("proxy.com", ip.getInetAddress());
        assertEquals(8080, ip.getPort());
        assertEquals(ProxyType.SOCKS, ip.getProxyType());
    }

    // ==================== URLDecoder Tests ====================

    @Test
    public void testURLDecoderHttpUrl() {
        IPAddress ip = IPAddress.URLDecoder.decode("http://example.com:8080/path");
        assertEquals("example.com", ip.getInetAddress());
        assertEquals(8080, ip.getPort());
    }

    @Test
    public void testURLDecoderHttpUrlDefaultPort() {
        IPAddress ip = IPAddress.URLDecoder.decode("http://example.com/path");
        assertEquals("example.com", ip.getInetAddress());
        assertEquals(80, ip.getPort());
    }

    @Test
    public void testURLDecoderHttpsUrl() {
        IPAddress ip = IPAddress.URLDecoder.decode("https://secure.example.com");
        assertEquals("secure.example.com", ip.getInetAddress());
        assertEquals(443, ip.getPort());
    }

    @Test
    public void testURLDecoderHttpsUrlWithPort() {
        IPAddress ip = IPAddress.URLDecoder.decode("https://secure.example.com:8443");
        assertEquals("secure.example.com", ip.getInetAddress());
        assertEquals(8443, ip.getPort());
    }

    @Test
    public void testURLDecoderWithQueryString() {
        IPAddress ip = IPAddress.URLDecoder.decode("http://example.com:9000?query=test");
        assertEquals("example.com", ip.getInetAddress());
        assertEquals(9000, ip.getPort());
    }

    @Test
    public void testURLDecoderNullInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            IPAddress.URLDecoder.decode(null);
        });
    }

    @Test
    public void testURLDecoderEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            IPAddress.URLDecoder.decode("");
        });
    }

    @Test
    public void testURLDecoderInvalidScheme() {
        assertThrows(IllegalArgumentException.class, () -> {
            IPAddress.URLDecoder.decode("invalid://example.com");
        });
    }

    // ==================== RangeDecoder Tests ====================

    @Test
    public void testRangeDecoderSinglePort() {
        IPAddress[] result = IPAddress.RangeDecoder.decode("192.168.1.1:8080");
        assertEquals(1, result.length);
        assertEquals("192.168.1.1", result[0].getInetAddress());
        assertEquals(8080, result[0].getPort());
    }

    @Test
    public void testRangeDecoderPortRange() {
        IPAddress[] result = IPAddress.RangeDecoder.decode("localhost:[8080,8083]");
        assertEquals(4, result.length);
        for (int i = 0; i < result.length; i++) {
            assertEquals("localhost", result[i].getInetAddress());
            assertEquals(8080 + i, result[i].getPort());
        }
    }

    // ==================== parseList Tests ====================

    @Test
    public void testParseListSingleUrl() {
        IPAddress[] result = IPAddress.parseList("http://example.com:8080");
        assertEquals(1, result.length);
        assertEquals("example.com", result[0].getInetAddress());
        assertEquals(8080, result[0].getPort());
    }

    @Test
    public void testParseListMultipleUrls() {
        IPAddress[] result = IPAddress.parseList(
                "http://example1.com:8080",
                "https://example2.com:443"
        );
        assertEquals(2, result.length);
        assertEquals("example1.com", result[0].getInetAddress());
        assertEquals(8080, result[0].getPort());
        assertEquals("example2.com", result[1].getInetAddress());
        assertEquals(443, result[1].getPort());
    }

    @Test
    public void testParseListWithPortRange() {
        IPAddress[] result = IPAddress.parseList("localhost:[8080,8082]");
        assertEquals(3, result.length);
        assertEquals("localhost", result[0].getInetAddress());
        assertEquals(8080, result[0].getPort());
        assertEquals("localhost", result[1].getInetAddress());
        assertEquals(8081, result[1].getPort());
        assertEquals("localhost", result[2].getInetAddress());
        assertEquals(8082, result[2].getPort());
    }

    @Test
    public void testParseListMixedUrlsAndRanges() {
        IPAddress[] result = IPAddress.parseList(
                "http://example.com:80",
                "localhost:[9000,9001]"
        );
        assertEquals(3, result.length);
        assertEquals("example.com", result[0].getInetAddress());
        assertEquals(80, result[0].getPort());
        assertEquals("localhost", result[1].getInetAddress());
        assertEquals(9000, result[1].getPort());
        assertEquals("localhost", result[2].getInetAddress());
        assertEquals(9001, result[2].getPort());
    }

    @Test
    public void testParseListWithDefaultHttpsPort() {
        IPAddress[] result = IPAddress.parseList("https://secure.example.com");
        assertEquals(1, result.length);
        assertEquals("secure.example.com", result[0].getInetAddress());
        assertEquals(443, result[0].getPort());
    }

    @Test
    public void testParseListEmptyArray() {
        IPAddress[] result = IPAddress.parseList();
        assertEquals(0, result.length);
    }

    @Test
    public void testParseListWithInvalidEntries() {
        // Invalid entries should be silently skipped
        IPAddress[] result = IPAddress.parseList(
                "http://valid.com:80",
                "completely-invalid-string",
                "https://another-valid.com:443"
        );
        // Only valid URLs should be parsed
        assertEquals(2, result.length);
        assertEquals("valid.com", result[0].getInetAddress());
        assertEquals(80, result[0].getPort());
        assertEquals("another-valid.com", result[1].getInetAddress());
        assertEquals(443, result[1].getPort());
    }

    @Test
    public void testParseListSinglePortAddress() {
        IPAddress[] result = IPAddress.parseList("192.168.1.1:3000");
        assertEquals(1, result.length);
        assertEquals("192.168.1.1", result[0].getInetAddress());
        assertEquals(3000, result[0].getPort());
    }

    @Test
    public void testParseListMultipleRanges() {
        IPAddress[] result = IPAddress.parseList(
                "server1:[80,81]",
                "server2:[443,444]"
        );
        assertEquals(4, result.length);
        assertEquals("server1", result[0].getInetAddress());
        assertEquals(80, result[0].getPort());
        assertEquals("server1", result[1].getInetAddress());
        assertEquals(81, result[1].getPort());
        assertEquals("server2", result[2].getInetAddress());
        assertEquals(443, result[2].getPort());
        assertEquals("server2", result[3].getInetAddress());
        assertEquals(444, result[3].getPort());
    }

    @Test

    public void testParseListLargeRange() {
        IPAddress[] result = IPAddress.parseList("scanner:[1,10]", "https://example.io", "http://example.io:8080");
        assertEquals(12, result.length);
        for (int i = 0; i < result.length; i++) {
            assert (result[i].getInetAddress() != null);
            assert (result[i].getPort() != -1);
            System.out.println(result[i]);
        }
    }


    @Test
    public void testParseFullUrl() {
        String[] toTest = new String[]{"http://xlogistx.io/", "http://xlogistx.io:8080///", "https://xlogistx.com/index.html", "https://xlogistx.ai?a=1",
                "https://finance.yahoo.com/news/why-your-utility-bills-are-out-of-control-its-not-just-the-arctic-blast-or-ai-100008895.html"};

        for (String ip : toTest) {
            System.out.println(IPAddress.parse(ip));
        }

    }
}
