package org.zoxweb.shared.http;

import org.zoxweb.shared.util.GetNVProperties;
import org.zoxweb.shared.util.NVGenericMap;

public class HTTPResult<D>
{
    private final HTTPMessageConfig hmci = new HTTPMessageConfig();
    private final D data;

    public HTTPResult(D data)
    {
        this.data = data;
    }

    public HTTPMessageConfigInterface getHTTPMessageConfig()
    {
        return hmci;
    }


    public D getData(){return data;}

}
