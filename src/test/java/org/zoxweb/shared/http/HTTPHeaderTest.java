package org.zoxweb.shared.http;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.GetNameValue;
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
}
