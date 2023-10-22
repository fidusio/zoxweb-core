package org.zoxweb.shared.http;

import org.zoxweb.shared.util.Const;

import java.util.List;
import java.util.Map;

public class HTTPAPIResult<O>
extends HTTPResponse
{

    private final O data;

    public HTTPAPIResult(int status, Map<String, List<String>> headers, O data, long duration)
    {
        super(status, headers);
        this.data = data;
        setDuration(duration);
    }


    public O getData()
    {
        return  data;
    }

    @Override
    public String toString() {
        return "HTTPAPIResult{" +
                "status: " + getStatus() + ", " +
                "headers: " + getHeaders() + ", " +
                "data: " + getData() + ", " +
                "duration: " + Const.TimeInMillis.toString(getDuration()) +
                "}";
    }
}
