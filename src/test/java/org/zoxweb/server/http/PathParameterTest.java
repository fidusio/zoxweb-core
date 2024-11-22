package org.zoxweb.server.http;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NVInt;


import java.io.UnsupportedEncodingException;
import java.util.HashMap;
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
    public void parseParametersWithValues()
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
    public void parseFormatParameters() throws UnsupportedEncodingException {
        String metaPath = "/path/{param1}/{param2}/token/{param3}";

        System.out.println(HTTPUtil.parseURIParameters(metaPath));
        NVGenericMap nvgm = new NVGenericMap().build("param1", "value1")
                .build(new NVInt("param2", 25))
                .build("param3", null);
        String uri = HTTPUtil.formatURI(metaPath, nvgm);

        System.out.println(metaPath);
        System.out.println(uri);

        Map<String, Object> params = new HashMap<>();
        params.put("param1", "batata");
        params.put("param2", 1.75);
        params.put("param3", true);

        uri = HTTPUtil.formatURI(metaPath, params);

        System.out.println(metaPath);
        System.out.println(uri);
        uri = HTTPUtil.formatURI(metaPath, params);

        System.out.println(metaPath);
        System.out.println(uri);
        uri = HTTPUtil.formatURI(metaPath, new NVGenericMap());

        System.out.println(metaPath);
        System.out.println(uri);


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
