package org.zoxweb.server.http;

import org.junit.jupiter.api.Test;


import java.util.Map;

public class PathParameterTest {
    @Test
    public void parsePath()
    {
        String basePath = "/path/pathplus";
        String fullPath = basePath + "/{param1}/{param2}";
        String httpBasePath = HTTPUtil.basePath(fullPath, true);
        assert(httpBasePath.equals(basePath));
    }

    @Test
    public void parseParameters()
    {
        String metaPath = "/path/{param1}/{param2}/token/{param3}";
        String[] dataPaths ={
          "/path/v1",
          "/path/v1/v2/token/v3",
          "/path/v1/v2/token/",
        };

        for(String dp : dataPaths)
        {
            Map<String, Object> result = HTTPUtil.parsePathParameters(metaPath, dp, false);
            System.out.println(result);
            result = HTTPUtil.parsePathParameters(metaPath, dp, true);
            System.out.println(result);

        }
    }

    @Test
    public void parseQuery()
    {
        String[] uris = {
                "/hello?a=1&b=2",
        };

        for(String uri :  uris)
        {
            System.out.println(HTTPUtil.parseQuery(uri, true));
        }
    }
}
