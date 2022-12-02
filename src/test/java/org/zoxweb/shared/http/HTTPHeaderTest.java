package org.zoxweb.shared.http;

import org.junit.jupiter.api.Test;

public class HTTPHeaderTest {


    @Test
    public void testHeaderName()
    {
        System.out.println(HTTPHeader.toHTTPHeader(HTTPHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS, HTTPMimeType.APPLICATION_JSON));
    }
}
