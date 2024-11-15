package org.zoxweb.server.http;

import org.junit.jupiter.api.BeforeAll;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPMethod;
import org.zoxweb.shared.util.NVGenericMap;

public class HTTPAPICallerTest {
    @BeforeAll
    public static void createEndPoints()
    {

        String url = "https://loclahost:6443";
        String domain = "xlogistx-api";
        // https://localhost/timestamps
        //
        HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit(url, "timestamp", HTTPMethod.GET);
        hmci.setName("timestamp");
        HTTPAPIEndPoint<NVGenericMap, NVGenericMap> loginEP = HTTPAPIManager.SINGLETON.buildEndPoint(hmci, null, null);


        /////////////////////////////////
        // https://localhost/ping
        //



        /////////////////////////////////
    }
}
