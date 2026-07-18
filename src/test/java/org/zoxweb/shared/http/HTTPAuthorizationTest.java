package org.zoxweb.shared.http;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.SUS;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

public class HTTPAuthorizationTest {

    @Test
    public void basic() {
        HTTPAuthorization authToken = HTTPAuthorization.createBasic("user", "password");
        GetNameValue<String> nvs = authToken.toHTTPHeader();
        System.out.println(nvs);

        // "user:password" base64-encoded
        assertEquals(HTTPHeader.AUTHORIZATION.getName(), nvs.getName());
        assertEquals("Basic dXNlcjpwYXNzd29yZA==", nvs.getValue());
        assertEquals("user", authToken.getTokenProperties().getValue("user"));
        assertEquals("password", authToken.getTokenProperties().getValue("password"));

        // Round-trip through parse: the header value must be stable, not double-prefixed.
        authToken = HTTPAuthScheme.BASIC.toHTTPAuthentication(nvs.getValue());
        GetNameValue<String> roundTrip = authToken.toHTTPHeader();
        System.out.println(roundTrip);
        assertEquals(nvs.getValue(), roundTrip.getValue());
        assertEquals("user", authToken.getTokenProperties().getValue("user"));
        assertEquals("password", authToken.getTokenProperties().getValue("password"));

        HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit("https://iot.xlogistx.io", "login", HTTPMethod.GET);
        hmci.setAuthorization(authToken);
        hmci.setAccept(HTTPMediaType.APPLICATION_JSON);
        hmci.setContentType(HTTPMediaType.APPLICATION_JSON);

        System.out.println(GSONUtil.toJSONDefault(hmci, true));
    }

    @Test
    public void bearer() {
        String rawToken = "dskjfdljfdksajgfhdsgjhjkhgfd";
        HTTPAuthorization authToken = HTTPAuthorization.createBearer(rawToken);

        GetNameValue<String> nvs = authToken.toHTTPHeader();
        System.out.println(nvs);
        assertEquals(HTTPHeader.AUTHORIZATION.getName(), nvs.getName());
        assertEquals("Bearer " + rawToken, nvs.getValue());
        assertEquals(rawToken, authToken.getToken());

        // Round-tripping a full "Bearer <token>" header value must strip the scheme,
        // not stack a second "Bearer " prefix.
        authToken = HTTPAuthScheme.BEARER.toHTTPAuthentication(nvs.getValue());
        GetNameValue<String> roundTrip = authToken.toHTTPHeader();
        System.out.println(roundTrip);
        assertEquals(nvs.getValue(), roundTrip.getValue());
        assertEquals(rawToken, authToken.getToken());
        assertFalse(roundTrip.getValue().contains("Bearer Bearer"));

        HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit("https://iot.xlogistx.io", "login", HTTPMethod.GET);
        hmci.setAuthorization(authToken);
        hmci.setAccept(HTTPMediaType.APPLICATION_JSON);
        hmci.setContentType(HTTPMediaType.APPLICATION_JSON);

        System.out.println(GSONUtil.toJSONDefault(hmci, true));
    }

    @Test
    public void generic() {
        HTTPAuthorization authToken = HTTPAuthorization.createGeneric("xlogistx-key", null, "key1232ljkwerjew3lr5jk342j5243");
        GetNameValue<String> nvs = authToken.toHTTPHeader();
        System.out.println(nvs);
        assertEquals("xlogistx-key", nvs.getName());
        assertEquals("key1232ljkwerjew3lr5jk342j5243", nvs.getValue());
        System.out.println(authToken.getNVConfig());
        System.out.println(((NVConfigEntity) authToken.getNVConfig()).getAttributes());

        System.out.println("attributes:" + authToken.getAttributes());

        authToken = HTTPAuthorization.parse(nvs);//HTTPAuthScheme.GENERIC.toHTTPAuthentication(nvs.getValue());
        nvs = authToken.toHTTPHeader();
        System.out.println(nvs);
        // The parsed generic authorization must produce a usable (non-null) header name.
        assertNotNull(nvs.getName());
        assertEquals("key1232ljkwerjew3lr5jk342j5243", authToken.getToken());
        assertEquals("key1232ljkwerjew3lr5jk342j5243", nvs.getValue());

        HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit("https://iot.xlogistx.io", "login", HTTPMethod.GET);
        hmci.setAuthorization(authToken);
        hmci.setAccept(HTTPMediaType.APPLICATION_JSON);
        hmci.setContentType(HTTPMediaType.APPLICATION_JSON);

        authToken = hmci.getAuthorization();

        String json = GSONUtil.toJSONDefault(hmci, true);
        System.out.println(json);
        System.out.println(authToken);
        hmci = GSONUtil.fromJSONDefault(json, HTTPMessageConfig.class);
        String json2 = GSONUtil.toJSONDefault(hmci, true);
        assertEquals(json, json2);
        System.out.println(json2);
    }

    @Test
    public void userInfoFromURL() throws MalformedURLException {
        URL url = new URL("http://john:secret123@example.com:8080/resource?name=mario&lastname=taza");

        System.out.println(SUS.toCanonicalID(',', url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery()));
        GetNameValue<String> authorization = HTTPAuthScheme.BASIC.toHTTPHeader(url.getUserInfo());
        System.out.println(authorization);
        // "john:secret123" base64-encoded
        assertEquals(HTTPHeader.AUTHORIZATION.getName(), authorization.getName());
        assertEquals("Basic am9objpzZWNyZXQxMjM=", authorization.getValue());
    }

    @Test
    public void genericNotAuthorizationHeader() {
        HTTPAuthorization authToken = HTTPAuthorization.createGeneric("x-xlogistx-key", null, "key1232ljkwerjew3lr5jk342j5243");

        GetNameValue<String> nvs = authToken.toHTTPHeader();
        System.out.println(nvs);
        assertEquals("x-xlogistx-key", nvs.getName());
        assertEquals("key1232ljkwerjew3lr5jk342j5243", nvs.getValue());
        System.out.println(authToken.getNVConfig());
        System.out.println(((NVConfigEntity) authToken.getNVConfig()).getAttributes());

        System.out.println("attributes:" + authToken.getAttributes());

        HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit("https://iot.xlogistx.io", "login", HTTPMethod.GET);
        hmci.setAuthorization(authToken);
        hmci.setAccept(HTTPMediaType.APPLICATION_JSON);
        hmci.setContentType(HTTPMediaType.APPLICATION_JSON);

        authToken = hmci.getAuthorization();
        assertEquals("x-xlogistx-key", authToken.getName());

        String json = GSONUtil.toJSONDefault(hmci, true);
        System.out.println(json);
        System.out.println(authToken);
        hmci = GSONUtil.fromJSONDefault(json, HTTPMessageConfig.class);
        String json2 = GSONUtil.toJSONDefault(hmci, true);
        assertEquals(json, json2);
        System.out.println(json2);
    }
}
