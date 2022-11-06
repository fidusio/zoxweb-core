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

    public  int getStatus()
    {
        return status;
    }

    public Map<String, List<String>> getHeaders()
    {
        return headers;
    }



    public List<String> headerValues(String headersName)
    {
        return headers.get(headersName);
    }
    public String headerValue(String headerName)
    {
        List<String> headerValue = headers.get(headerName);
        if (headerName != null && headerValue.size() > 0)
            return headerValue.get(0);

        return null;
    }

    public long longHeaderValue(String headerName)
    {
        String val = headerValue(headerName);
        if(val == null)
            throw new NullPointerException(headerName +" not found");
        return Long.parseLong(val);
    }

    public float floatHeaderValue(String headerName)
    {
        String val = headerValue(headerName);
        if(val == null)
            throw new NullPointerException(headerName +" not found");
        return Float.parseFloat(val);
    }

    public double doubleHeaderValue(String headerName)
    {
        String val = headerValue(headerName);
        if(val == null)
            throw new NullPointerException(headerName +" not found");
        return Double.parseDouble(val);
    }


    public int intHeaderValue(String headerName)
    {
        return (int)longHeaderValue(headerName);
    }

}
