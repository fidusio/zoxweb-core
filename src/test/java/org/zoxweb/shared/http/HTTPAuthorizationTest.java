package org.zoxweb.shared.http;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.http.HTTPCall;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.util.GetNameValue;

public class HTTPAuthorizationTest {

    @Test
    public void basic()
    {

    }
    @Test
    public void bearer()
    {

    }

    @Test
    public void generic()
    {
        HTTPAuthorization authToken = new HTTPAuthorizationToken(HTTPAuthScheme.GENERIC, "xlogistx-key", "key1232ljkwerjew3lr5jk342j5243");
        GetNameValue<String> nvs = authToken.toHTTPHeader();
        System.out.println(nvs);


        authToken = HTTPAuthScheme.GENERIC.toHTTPAuthentication(nvs.getValue());//new HTTPAuthorizationToken(HTTPAuthScheme.GENERIC, nvs.getValue());
        nvs = authToken.toHTTPHeader();
        System.out.println(nvs);


        HTTPMessageConfigInterface hmci =  HTTPMessageConfig.createAndInit("https://iot.xlogistx.io", "login", HTTPMethod.GET);
        hmci.setAuthorization(authToken);
        hmci.setAccept(HTTPMediaType.APPLICATION_JSON);
        hmci.setContentType(HTTPMediaType.APPLICATION_JSON);

        System.out.println(GSONUtil.toJSONDefault(hmci, true));


        try
        {

            System.out.println(HTTPCall.send(hmci));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


    }
}
