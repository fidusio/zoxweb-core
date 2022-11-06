package org.zoxweb.shared.http;

import java.util.List;
import java.util.Map;

public abstract class HTTPResponse
{
    private final int status;
    private final Map<String, List<String>> headers;

    protected HTTPResponse (int status, Map<String, List<String>> headers)
    {
        this.status = status;
        this.headers = headers;
    }

    public final int getStatus()
    {
        return status;
    }

    public final Map<String, List<String>> getHeaders()
    {
        return headers;
    }

}
