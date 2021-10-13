package org.zoxweb.shared.http;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.SharedStringUtil;

import java.util.Arrays;

public class HTTPEndPointTest
{

    static HTTPEndPoint hep;
    @BeforeAll
    public static  void setup()
    {
        hep = new HTTPEndPoint();
        hep.setName("test");
        hep.setBean("org.zoxweb.shared.data.AddressDAO");
        hep.setMethods(HTTPMethod.GET, HTTPMethod.POST);
        hep.setPaths("/ping/{detailed}", "/info/{level}/{detailed}");
    }

    @Test
    public void pathSupport()
    {
        String[] uris = {"/ping", "/ping/", "ping/", "ping", "/pings", "/ping/detailed", "/info/1/detailed"};

        for (String uri: uris)
        {
            System.out.println(uri + ":" + hep.isPathSupported(uri) + ", " + hep.isPathSupported(uri));
            System.out.println("ParseString:" + Arrays.toString(SharedStringUtil.parseString(uri, "/", true)));

        }
    }
}
