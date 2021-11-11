package org.zoxweb.shared.http;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.SharedStringUtil;

import java.util.Arrays;

public class HTTPEndPointTest
{

    static HTTPEndPoint hep;

    static HTTPEndPoint sysHep;
    @BeforeAll
    public static  void setup()
    {
        hep = new HTTPEndPoint();
        hep.setName("test");
        hep.setBean("org.zoxweb.shared.data.AddressDAO");
        hep.setMethods(HTTPMethod.GET, HTTPMethod.POST);
        hep.setPaths("/ping/{detailed}", "/info/{level}/{detailed}");

        sysHep = new HTTPEndPoint();
        sysHep.setName("system");
        sysHep.setBean("org.zoxweb.shared.data.AddressDAO");
        sysHep.setMethods(HTTPMethod.GET, HTTPMethod.POST);
        sysHep.setPaths("/system/reboot", "/system/shutdown");
    }

    @Test
    public void pathSupport()
    {
        String[] uris = {"/ping", "/ping/", "ping/", "ping", "/pings", "/ping/detailed", "/info/1/detailed", "/info", "/info/1"};

        for (String uri: uris)
        {
            uri = SharedStringUtil.removeCharFromEnd('/', uri);
            System.out.println(uri + ":" + hep.isPathSupported(uri)  +", ParseString:" + Arrays.toString(SharedStringUtil.parseString(uri, "/", true)));
        }
    }

    @Test
    public void pathSysSupport()
    {
        String[] uris = {"/system/reboot", "/system/shutdown", "/system/reboot/", "system/reboot/","/system//////////",  "system/reboot/now" };

        for (String uri: uris)
        {
            uri = SharedStringUtil.removeCharFromEnd('/', uri);
            System.out.println(uri + ":" + sysHep.isPathSupported(uri)  +", ParseString:" + Arrays.toString(SharedStringUtil.parseString(uri, "/", true)));

        }
    }
}
