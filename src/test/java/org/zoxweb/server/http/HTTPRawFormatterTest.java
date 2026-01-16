package org.zoxweb.server.http;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.http.HTTPMessageConfig;
import org.zoxweb.shared.http.HTTPMessageConfigInterface;
import org.zoxweb.shared.http.HTTPVersion;

public class HTTPRawFormatterTest {

    @Test
    public void testHMCI()
    {
        HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit("https://xlogistx.io", "timestamps", "GET");
        hmci.setHTTPVersion(HTTPVersion.HTTP_1_1);
        HTTPRawFormatter hrf = new  HTTPRawFormatter(hmci);


        System.out.println(hrf.format());
    }
}
