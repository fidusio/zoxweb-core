package org.zoxweb.shared.http;

import java.util.List;
import java.util.Map;

public class HTTPAPIResult<O>
extends HTTPResponse
{

    private final O data;

    public HTTPAPIResult(int status, Map<String, List<String>> headers, O data)
    {
        super(status, headers);
        this.data = data;
    }


    public O getData()
    {
        return  data;
    }
}
