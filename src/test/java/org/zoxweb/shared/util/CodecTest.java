package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.http.HTTPNVGMBiEncoder;
import org.zoxweb.server.util.GSONUtil;

import java.util.HashMap;
import java.util.Map;

public class CodecTest
{

    @Test
    public void dataEncoder()
    {

    }

    @Test
    public void dataDecoder()
    {

    }

    @Test
    public void json()
    {
        NVGenericMap nvgm = new NVGenericMap("parameters");
        nvgm.add("par1", "val1");
        nvgm.add("par2", "val2");
        nvgm.add("par3", "val3");
        nvgm.add("par4", "val4");
        nvgm.add("to_include", "toIncludeVal");
        HTTPNVGMBiEncoder httpBiEncoder = new HTTPNVGMBiEncoder(nvgm, "attr_name", "to_include");

        String json = GSONUtil.toJSONDefault(httpBiEncoder);
        httpBiEncoder = GSONUtil.fromJSONDefault(json, HTTPNVGMBiEncoder.class);
        String json1 =GSONUtil.toJSONDefault(httpBiEncoder);

        assert (json1.equals(json));
        System.out.println(GSONUtil.toJSONDefault(httpBiEncoder));

        Map<String, String> map = new HashMap<>();
        map.put("par1", "destination_par1");
        map.put("par3", "destination_par30");

        httpBiEncoder = new HTTPNVGMBiEncoder(nvgm, "attr_name", map);
        System.out.println(GSONUtil.toJSONDefault(httpBiEncoder));
    }
}
