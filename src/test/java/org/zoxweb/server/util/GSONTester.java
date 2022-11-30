package org.zoxweb.server.util;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.util.NVEntity;
import org.zoxweb.shared.util.NVPair;
import org.zoxweb.shared.util.ParamUtil;

public class GSONTester {

    public static void main(String ...args)
    {
        try
        {
            ParamUtil.ParamMap params = ParamUtil.parse("=", args);
            String jsonFile = params.stringValue("json", true);
            HTTPMessageConfig hmc = new HTTPMessageConfig();
            hmc.setURI("api/spots/36186/contacts/add");
            hmc.setURL("https://www.spothopperapp.com");
            hmc.setMethod("post");
            hmc.getParameters().add(new NVPair("email", "null", FilterType.EMAIL));
            System.out.println(GSONUtil.toJSON(hmc, true, false, true ));
            System.out.println(hmc);


            if(jsonFile != null)
            {
                NVEntity nve = GSONUtil.fromJSON(IOUtil.inputStreamToString(jsonFile));
                System.out.println("\n" + nve);
                System.out.println(GSONUtil.toJSON(nve, true, true, true ));
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
