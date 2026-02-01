package org.zoxweb.shared.http;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class URLInfoTest {

    // ==================== Basic URL Parsing Tests ====================

    @Test
    public void testParseSimpleHttpUrl() {
        URLInfo info = URLInfo.parse("http://example.com");
        assertEquals(URIScheme.HTTP, info.scheme);
        assertEquals("example.com", info.ipAddress.getInetAddress());
        assertEquals(80, info.ipAddress.getPort());
        assertNull(info.username);
        assertNull(info.password);
        assertEquals("", info.path);
        assertNull(info.query);
        assertNull(info.fragment);
    }

    @Test
    public void testParseSimpleHttpsUrl() {
        URLInfo info = URLInfo.parse("https://secure.example.com");
        assertEquals(URIScheme.HTTPS, info.scheme);
        assertEquals("secure.example.com", info.ipAddress.getInetAddress());
        assertEquals(443, info.ipAddress.getPort());
    }

    @Test
    public void testParseUrlWithExplicitPort() {
        URLInfo info = URLInfo.parse("http://example.com:8080");
        assertEquals("example.com", info.ipAddress.getInetAddress());
        assertEquals(8080, info.ipAddress.getPort());
    }

    @Test
    public void testParseUrlWithPath() {
        URLInfo info = URLInfo.parse("http://example.com/path/to/resource");
        assertEquals("example.com", info.ipAddress.getInetAddress());
        assertEquals("/path/to/resource", info.path);
    }

    @Test
    public void testParseUrlWithPathAndPort() {
        URLInfo info = URLInfo.parse("http://example.com:3000/api/v1/users");
        assertEquals("example.com", info.ipAddress.getInetAddress());
        assertEquals(3000, info.ipAddress.getPort());
        assertEquals("/api/v1/users", info.path);
    }

    // ==================== Query String Tests ====================

    @Test
    public void testParseUrlWithSingleQueryParam() {
        URLInfo info = URLInfo.parse("http://example.com?name=value");
        assertEquals("name=value", info.query);
        assertEquals(1, info.params.size());
        assertEquals("value", info.params.get("name").get(0));
    }

    @Test
    public void testParseUrlWithMultipleQueryParams() {
        URLInfo info = URLInfo.parse("http://example.com?foo=bar&baz=qux");
        assertEquals(2, info.params.size());
        assertEquals("bar", info.params.get("foo").get(0));
        assertEquals("qux", info.params.get("baz").get(0));
    }

    @Test
    public void testParseUrlWithRepeatedQueryParams() {
        URLInfo info = URLInfo.parse("http://example.com?tag=java&tag=python&tag=rust");
        assertEquals(1, info.params.size());
        List<String> tags = info.params.get("tag");
        assertEquals(3, tags.size());
        assertEquals("java", tags.get(0));
        assertEquals("python", tags.get(1));
        assertEquals("rust", tags.get(2));
    }

    @Test
    public void testParseUrlWithQueryParamNoValue() {
        URLInfo info = URLInfo.parse("http://example.com?flag");
        assertEquals(1, info.params.size());
        assertEquals("", info.params.get("flag").get(0));
    }

    @Test
    public void testParseUrlWithEncodedQueryParams() {
        URLInfo info = URLInfo.parse("http://example.com?name=hello%20world");
        assertEquals("hello world", info.params.get("name").get(0));
    }

    @Test
    public void testParseUrlWithPlusEncodedSpace() {
        URLInfo info = URLInfo.parse("http://example.com?name=hello+world");
        assertEquals("hello world", info.params.get("name").get(0));
    }

    @Test
    public void testParseUrlWithPathAndQuery() {
        URLInfo info = URLInfo.parse("http://example.com/search?q=test&page=1");
        assertEquals("/search", info.path);
        assertEquals("test", info.params.get("q").get(0));
        assertEquals("1", info.params.get("page").get(0));
    }

    // ==================== Fragment Tests ====================

    @Test
    public void testParseUrlWithFragment() {
        URLInfo info = URLInfo.parse("http://example.com#section1");
        assertEquals("section1", info.fragment);
    }

    @Test
    public void testParseUrlWithPathAndFragment() {
        URLInfo info = URLInfo.parse("http://example.com/page#top");
        assertEquals("/page", info.path);
        assertEquals("top", info.fragment);
    }

    @Test
    public void testParseUrlWithQueryAndFragment() {
        URLInfo info = URLInfo.parse("http://example.com?key=value#section");
        assertEquals("value", info.params.get("key").get(0));
        assertEquals("section", info.fragment);
    }

    @Test
    public void testParseUrlWithPathQueryAndFragment() {
        URLInfo info = URLInfo.parse("http://example.com/page?id=123#comments");
        assertEquals("/page", info.path);
        assertEquals("123", info.params.get("id").get(0));
        assertEquals("comments", info.fragment);
    }

    // ==================== Authentication Tests ====================

    @Test
    public void testParseUrlWithUsername() {
        URLInfo info = URLInfo.parse("http://user@example.com");
        assertEquals("user", info.username);
        assertNull(info.password);
        assertEquals("example.com", info.ipAddress.getInetAddress());
    }

    @Test
    public void testParseUrlWithUsernameAndPassword() {
        URLInfo info = URLInfo.parse("http://user:pass@example.com");
        assertEquals("user", info.username);
        assertEquals("pass", info.password);
        assertEquals("example.com", info.ipAddress.getInetAddress());
    }

    @Test
    public void testParseUrlWithAuthAndPort() {
        URLInfo info = URLInfo.parse("http://admin:secret@example.com:8080/admin");
        assertEquals("admin", info.username);
        assertEquals("secret", info.password);
        assertEquals("example.com", info.ipAddress.getInetAddress());
        assertEquals(8080, info.ipAddress.getPort());
        assertEquals("/admin", info.path);
    }

    // ==================== IPv6 Tests ====================

    @Test
    public void testParseUrlWithIPv6Host() {
        URLInfo info = URLInfo.parse("http://[::1]:8080/path");
        assertEquals("::1", info.ipAddress.getInetAddress());
        assertEquals(8080, info.ipAddress.getPort());
        assertEquals("/path", info.path);
    }

    @Test
    public void testParseUrlWithIPv6HostNoPort() {
        URLInfo info = URLInfo.parse("http://[2001:db8::1]/resource");
        assertEquals("2001:db8::1", info.ipAddress.getInetAddress());
        assertEquals(80, info.ipAddress.getPort()); // default port
        assertEquals("/resource", info.path);
    }

    @Test
    public void testParseUrlWithIPv6Localhost() {
        URLInfo info = URLInfo.parse("https://[::1]");
        assertEquals("::1", info.ipAddress.getInetAddress());
        assertEquals(443, info.ipAddress.getPort());
    }

    // ==================== IPv4 Tests ====================

    @Test
    public void testParseUrlWithIPv4Host() {
        URLInfo info = URLInfo.parse("http://192.168.1.1:3000");
        assertEquals("192.168.1.1", info.ipAddress.getInetAddress());
        assertEquals(3000, info.ipAddress.getPort());
    }

    @Test
    public void testParseUrlWithIPv4HostDefaultPort() {
        URLInfo info = URLInfo.parse("https://10.0.0.1/api");
        assertEquals("10.0.0.1", info.ipAddress.getInetAddress());
        assertEquals(443, info.ipAddress.getPort());
        assertEquals("/api", info.path);
    }

    // ==================== toURL() Round-trip Tests ====================

    @Test
    public void testToUrlSimple() {
        URLInfo info = URLInfo.parse("http://example.com:80");
        String url = info.toURL();
        System.out.println(url + " , " + info.toURI());
        assertTrue(url.contains("example.com"));
        assertTrue(url.startsWith("http://"));
    }

    @Test
    public void testToUrlWithPath() {
        URLInfo info = URLInfo.parse("http://example.com/path/to/resource");
        String url = info.toURL();
        System.out.println(url + " , " + info.toURI());
        assertTrue(url.contains("/path/to/resource"));
    }

    @Test
    public void testToUrlWithQueryParams() {
        URLInfo info = URLInfo.parse("http://example.com?foo=bar");
        String url = info.toURL();
        System.out.println(url + " , " + info.toURI());
        assertTrue(url.contains("foo=bar"));
    }

    @Test
    public void testToUrlWithFragment() {
        URLInfo info = URLInfo.parse("http://example.com#section");
        String url = info.toURL();
        System.out.println(url + " , " + info.toURI());
        assertTrue(url.contains("#section"));
    }

    @Test
    public void testToUrlWithAuth() {
        URLInfo info = URLInfo.parse("http://user:pass@example.com");
        String url = info.toURL();
        System.out.println(url + " , " + info.toURI());
        assertTrue(url.contains("user"));
        assertTrue(url.contains("@"));
    }

    @Test
    public void testToUrlWithIPv6() {
        URLInfo info = URLInfo.parse("http://[::1]:8080");
        String url = info.toURL();
        System.out.println(url + " , " + info.toURI());
        assertTrue(url.contains("[::1]"));
    }

    @Test
    public void testToUrlRoundTrip() {
        String original = "http://example.com:8080/path?key=value#frag";
        URLInfo info = URLInfo.parse(original);
        String reconstructed = info.toURL();
        System.out.println(reconstructed +  " , " + info.toURI());
        // Parse again and verify components match
        URLInfo reparsed = URLInfo.parse(reconstructed);
        assertEquals(info.scheme, reparsed.scheme);
        assertEquals(info.ipAddress.getInetAddress(), reparsed.ipAddress.getInetAddress());
        assertEquals(info.ipAddress.getPort(), reparsed.ipAddress.getPort());
        assertEquals(info.path, reparsed.path);
        assertEquals(info.fragment, reparsed.fragment);
    }

    // ==================== Edge Cases and Error Handling ====================

    @Test
    public void testParseNullThrows() {
        assertThrows(NullPointerException.class, () -> URLInfo.parse(null));
    }

    @Test
    public void testParseEmptyStringThrows() {
        assertThrows(IllegalArgumentException.class, () -> URLInfo.parse(""));
    }

    @Test
    public void testParseWhitespaceOnlyThrows() {
        assertThrows(IllegalArgumentException.class, () -> URLInfo.parse("   "));
    }

    @Test
    public void testParseMissingSchemeThrows() {
        assertThrows(IllegalArgumentException.class, () -> URLInfo.parse("example.com"));
    }

    @Test
    public void testParseMalformedSchemeThrows() {
        assertThrows(IllegalArgumentException.class, () -> URLInfo.parse("://example.com"));
    }

    @Test
    public void testParseInvalidIPv6Throws() {
        assertThrows(IllegalArgumentException.class, () -> URLInfo.parse("http://[::1/path"));
    }

    @Test
    public void testParsePortOutOfRangeThrows() {
        assertThrows(IllegalArgumentException.class, () -> URLInfo.parse("http://example.com:99999"));
    }

    @Test
    public void testParseBadPercentEncodingThrows() {
        assertThrows(IllegalArgumentException.class, () -> URLInfo.parse("http://example.com?name=%GG"));
    }

    @Test
    public void testParseTruncatedPercentEncodingThrows() {
        assertThrows(IllegalArgumentException.class, () -> URLInfo.parse("http://example.com?name=%2"));
    }

    // ==================== Various Scheme Tests ====================

    @Test
    public void testParseFtpScheme() {
        URLInfo info = URLInfo.parse("ftp://ftp.example.com/file.txt");
        assertEquals(URIScheme.FTP, info.scheme);
        assertEquals("ftp.example.com", info.ipAddress.getInetAddress());
        assertEquals(21, info.ipAddress.getPort());
        assertEquals("/file.txt", info.path);
    }

    @Test
    public void testParseWsScheme() {
        URLInfo info = URLInfo.parse("ws://socket.example.com/chat");
        assertEquals(URIScheme.WS, info.scheme);
        assertEquals("socket.example.com", info.ipAddress.getInetAddress());
        assertEquals("/chat", info.path);
    }

    @Test
    public void testParseWssScheme() {
        URLInfo info = URLInfo.parse("wss://secure-socket.example.com/chat");
        assertEquals(URIScheme.WSS, info.scheme);
        assertEquals("secure-socket.example.com", info.ipAddress.getInetAddress());
    }

    // ==================== Special Characters Tests ====================

    @Test
    public void testParseUrlWithSpecialCharsInQuery() {
        URLInfo info = URLInfo.parse("http://example.com?msg=hello%26world");
        assertEquals("hello&world", info.params.get("msg").get(0));
    }

    @Test
    public void testParseUrlWithEncodedSlashInQuery() {
        URLInfo info = URLInfo.parse("http://example.com?path=%2Fhome%2Fuser");
        assertEquals("/home/user", info.params.get("path").get(0));
    }

    @Test
    public void testToUrlEncodesSpecialChars() {
        URLInfo info = URLInfo.parse("http://example.com?name=a%20b");
        String url = info.toURL();
        // Space should be encoded as + or %20
        assertTrue(url.contains("a+b") || url.contains("a%20b"));
    }

    // ==================== toString() Tests ====================

    @Test
    public void testToStringContainsAllComponents() {
        URLInfo info = URLInfo.parse("http://user:pass@example.com:8080/path?key=val#frag");
        String str = info.toString();
        assertTrue(str.contains("http"));
        assertTrue(str.contains("user"));
        assertTrue(str.contains("pass"));
        assertTrue(str.contains("example.com"));
        assertTrue(str.contains("8080"));
        assertTrue(str.contains("/path"));
        assertTrue(str.contains("frag"));
    }

    // ==================== Complex URL Tests ====================

    @Test
    public void testParseComplexUrl() {
        String url = "https://admin:secret123@api.example.com:8443/v2/users?status=active&role=admin&sort=name#results";
        URLInfo info = URLInfo.parse(url);

        assertEquals(URIScheme.HTTPS, info.scheme);
        assertEquals("admin", info.username);
        assertEquals("secret123", info.password);
        assertEquals("api.example.com", info.ipAddress.getInetAddress());
        assertEquals(8443, info.ipAddress.getPort());
        assertEquals("/v2/users", info.path);
        assertEquals("active", info.params.get("status").get(0));
        assertEquals("admin", info.params.get("role").get(0));
        assertEquals("name", info.params.get("sort").get(0));
        assertEquals("results", info.fragment);
    }

    @Test
    public void testParseUrlWithEmptyPath() {
        URLInfo info = URLInfo.parse("http://example.com?query=1");
        assertEquals("", info.path);
        assertEquals("1", info.params.get("query").get(0));
    }

    @Test
    public void testParseUrlWithTrailingSlash() {
        URLInfo info = URLInfo.parse("http://example.com/");
        assertEquals("/", info.path);
    }

    @Test
    public void testParseUrlWithMultipleSlashesInPath() {
        URLInfo info = URLInfo.parse("http://example.com/a/b/c/d/e");
        assertEquals("/a/b/c/d/e", info.path);
    }
}
