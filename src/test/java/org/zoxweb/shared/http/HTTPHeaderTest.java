package org.zoxweb.shared.http;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVFloat;
import org.zoxweb.shared.util.NVLong;
import org.zoxweb.shared.util.NVPair;

public class HTTPHeaderTest {


    @Test
    public void testHeaderName()
    {
        System.out.println(HTTPConst.toString(HTTPConst.toHTTPHeader(HTTPHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS, HTTPMediaType.APPLICATION_JSON)));
    }
    @Test
    public void testHeaderFormat()
    {
        GetNameValue<?>[]  gnvs = {
                new NVLong("max-age", 31536000),
                new NVPair("includeSubDomains", (String) null),
                new NVPair("preload", (String) null),
        };

        System.out.println(HTTPConst.toString(HTTPConst.toHTTPHeader(HTTPHeader.STRICT_TRANSPORT_SECURITY, gnvs)));

    }

    @Test
    public void testHeaderRandomFormat()
    {
        GetNameValue<?>[]  gnvs = {
                new NVLong("long", 31536000),
                new NVFloat("float", 10.254F),
                new NVPair("string", "batata"),
        };

        System.out.println(HTTPConst.toString(HTTPConst.toHTTPHeader("invalid",  gnvs)));

    }
}
