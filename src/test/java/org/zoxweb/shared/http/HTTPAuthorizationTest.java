package org.zoxweb.shared.http;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.GetNameValue;

public class HTTPAuthorizationTest {

    @Test
    public void basic()
    {

    }
    @Test
    public void bearer()
    {

    }

    @Test
    public void generic()
    {
        HTTPAuthorization authToken = new HTTPAuthorizationToken(HTTPAuthScheme.GENERIC, "xlogistx-key", "key1232ljkwerjew3lr5jk342j5243");
        GetNameValue<String> nvs = authToken.toHTTPHeader();
        System.out.println(nvs);


        authToken = HTTPAuthScheme.GENERIC.toHTTPAuthentication(nvs.getValue());//new HTTPAuthorizationToken(HTTPAuthScheme.GENERIC, nvs.getValue());
        nvs = authToken.toHTTPHeader();
        System.out.println(nvs);


    }
}
