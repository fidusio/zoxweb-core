package org.zoxweb.shared.http;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.http.OkHTTPCall;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.SharedUtil;

import java.net.MalformedURLException;
import java.net.URL;

public class HTTPAuthorizationTest {

    @Test
    public void basic() {
        HTTPAuthorization authToken = new HTTPAuthorizationBasic("user", "password");
        GetNameValue<String> nvs = authToken.toHTTPHeader();
        System.out.println(nvs);


        authToken = HTTPAuthScheme.BASIC.toHTTPAuthentication(nvs.getValue());//new HTTPAuthorizationToken(HTTPAuthScheme.GENERIC, nvs.getValue());

        nvs = authToken.toHTTPHeader();
        System.out.println(nvs);


        HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit("https://iot.xlogistx.io", "login", HTTPMethod.GET);
        hmci.setAuthorization(authToken);
        hmci.setAccept(HTTPMediaType.APPLICATION_JSON);
        hmci.setContentType(HTTPMediaType.APPLICATION_JSON);

        System.out.println(GSONUtil.toJSONDefault(hmci, true));
    }

    @Test
    public void bearer() {
        HTTPAuthorization authToken = new HTTPAuthorization(HTTPAuthScheme.BEARER);
        authToken.setToken("dskjfdljfdksajgfhdsgjhjkhgfd");

        GetNameValue<String> nvs = authToken.toHTTPHeader();
        System.out.println(nvs);


        authToken = HTTPAuthScheme.BEARER.toHTTPAuthentication(nvs.getValue());//new HTTPAuthorizationToken(HTTPAuthScheme.GENERIC, nvs.getValue());
        nvs = authToken.toHTTPHeader();
        System.out.println(nvs);

        HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit("https://iot.xlogistx.io", "login", HTTPMethod.GET);
        hmci.setAuthorization(authToken);
        hmci.setAccept(HTTPMediaType.APPLICATION_JSON);
        hmci.setContentType(HTTPMediaType.APPLICATION_JSON);

        System.out.println(GSONUtil.toJSONDefault(hmci, true));
    }

    @Test
    public void generic() {
        HTTPAuthorization authToken = new HTTPAuthorization("xlogistx-key", "key1232ljkwerjew3lr5jk342j5243");
        GetNameValue<String> nvs = authToken.toHTTPHeader();
        System.out.println(nvs);
        System.out.println(authToken.getNVConfig());
        System.out.println(((NVConfigEntity) authToken.getNVConfig()).getAttributes());


        System.out.println("attributes:" + authToken.getAttributes());


        authToken = HTTPAuthScheme.GENERIC.toHTTPAuthentication(nvs.getValue());//new HTTPAuthorizationToken(HTTPAuthScheme.GENERIC, nvs.getValue());
        nvs = authToken.toHTTPHeader();
        System.out.println(nvs);


        HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit("https://iot.xlogistx.io", "login", HTTPMethod.GET);
        hmci.setAuthorization(authToken);
        hmci.setAccept(HTTPMediaType.APPLICATION_JSON);
        hmci.setContentType(HTTPMediaType.APPLICATION_JSON);

        authToken = hmci.getAuthorization();

        String json = GSONUtil.toJSONDefault(hmci, true);
        System.out.println(json);
//        System.out.println(((NVConfigEntity)authToken.getNVConfig()).getAttributes());
        System.out.println(authToken);
        hmci = GSONUtil.fromJSONDefault(json, HTTPMessageConfig.class);
        String json2 = GSONUtil.toJSONDefault(hmci, true);
        assert json2.equals(json);
        System.out.println(json2);

        try {

            System.out.println(OkHTTPCall.send(hmci));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void userInfoFromURL() throws MalformedURLException {
        URL url = new URL("http://john:secret123@example.com:8080/resource?name=mario&lastname=taza");

        System.out.println(SharedUtil.toCanonicalID(',', url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery()));
        GetNameValue<String> authorization = HTTPAuthScheme.BASIC.toHTTPHeader(url.getUserInfo());
        System.out.println(authorization);
    }
}
