package org.zoxweb.shared.data;

//import org.junit.Test;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.http.HTTPEncoder;
import org.zoxweb.shared.util.NVPair;

import java.io.IOException;

/**
 * Created on 8/11/17
 */
public class AppConfigDAOTest {

    public static final String url = "https://www.zipcodeapi.com";
    public static final String uri = "rest";
    public static final String distance = "25";
    public static final String zipCode = "90025";
    public static final String units = "mile";


    @Test
    public void testCreateAppConfigDAO() throws IOException {
        AppIDDAO appIDDAO = new AppIDDAO("zoxweb.org", "zoxweb");

        AppConfigDAO appConfigDAO = new AppConfigDAO(appIDDAO);

        HTTPMessageConfig hmc = (HTTPMessageConfig) HTTPMessageConfig.createAndInit(url, uri, HTTPMethod.GET);
        hmc.setName("zipcode-api-config");
        hmc.setHTTPParameterFormatter(HTTPEncoder.URI_REST_ENCODED);
        hmc.getParameters().add(new NVPair("zip_code", zipCode));
        hmc.getParameters().add(new NVPair("distance", distance));
        hmc.getParameters().add(new NVPair("units", units));

        appConfigDAO.getProperties().add(hmc);

        System.out.println(GSONUtil.toJSON(appConfigDAO, true, false, true));

    }

}
