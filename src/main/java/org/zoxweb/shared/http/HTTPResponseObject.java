package org.zoxweb.shared.http;

import java.util.List;
import java.util.Map;

public class HTTPResponseObject <O> {
    private final int status;
    private final Map<String, List<String>> responseHeaders;
    private final O object;

    public HTTPResponseObject(int status, Map<String, List<String>> responseHeaders, O object)
    {
        this.status = status;
        this.responseHeaders = responseHeaders;
        this.object = object;
    }


    public int getStatus()
    {
        return status;
    }

    public Map<String, List<String>> getHeaders()
    {
        return responseHeaders;
    }

    public <T> T getObject()
    {
        return (T) object;
    }
}
