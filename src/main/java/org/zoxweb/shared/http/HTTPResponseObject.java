package org.zoxweb.shared.http;

import java.util.List;
import java.util.Map;

public class HTTPResponseObject <O>
extends HTTPResponse
{

    private final O object;

    public HTTPResponseObject(int status, Map<String, List<String>> headers, O object)
    {
        super(status, headers);
        this.object = object;
    }


    public <T> T getObject()
    {
        return (T) object;
    }
}
