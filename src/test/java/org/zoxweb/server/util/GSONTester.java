package org.zoxweb.server.util;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.http.HTTPAuthorization;
import org.zoxweb.shared.http.HTTPAuthorizationBasic;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.NVEnum;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.ParamUtil;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GSONTester {



    public static void testHTTPMessageConfig() {
        HTTPMessageConfig hmc = new HTTPMessageConfig();
        hmc.setURI("api/v1/test");
        hmc.setURL("https://www.test.com");
        hmc.setMethod("post");
        hmc.getParameters().add(new NVPair("email", null, FilterType.EMAIL));
        hmc.getParameters().add(new NVEnum("time_unit", TimeUnit.MINUTES));
        hmc.setAuthorization(new HTTPAuthorizationBasic("marwan", "batata"));
        String json = GSONUtil.toJSONDefault(hmc, true);
        HTTPMessageConfig hmcDeserialized = GSONUtil.fromJSONDefault(json, HTTPMessageConfig.class);
        String jsonSerialized = GSONUtil.toJSONDefault(hmcDeserialized, true);
        System.out.println(json);
        System.out.println(hmc);

        System.out.println("Equals: " + jsonSerialized.equals(json));
    }

    public static void testFileLoading(String jsonFile) throws IOException
    {
        if(jsonFile != null)
        {
            NVEntity nve = GSONUtil.fromJSON(IOUtil.inputStreamToString(jsonFile));
            System.out.println("\n" + nve);
            System.out.println(GSONUtil.toJSONDefault(nve, true));
        }
    }

    public static void main(String ...args)
    {
        try
        {
            ParamUtil.ParamMap params = ParamUtil.parse("=", args);
            String jsonFile = params.stringValue("json", true);


            if(jsonFile != null)
                testFileLoading(jsonFile);

            testHTTPMessageConfig();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
