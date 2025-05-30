package org.zoxweb.shared.http;

import org.zoxweb.shared.util.GetName;

import java.util.List;
import java.util.Map;

public abstract class HTTPResponse
{
    private final int status;
    private final Map<String, List<String>> headers;



    private final long duration;


    protected HTTPResponse (int status, Map<String, List<String>> headers, long duration)
    {
        this.status = status;
        this.headers = headers;
        this.duration = duration;

    }

    public  int getStatus()
    {
        return status;
    }

    public Map<String, List<String>> getHeaders()
    {
        return headers;
    }

    public List<String> headerValues(GetName headerName)
    {
        return headerValues(headerName.getName());
    }

    public List<String> headerValues(String headerName)
    {
        return headers.get(headerName);
    }
    public String headerValue(String headerName)
    {
        List<String> headerValue = headers.get(headerName);
        if (headerName != null && !headerValue.isEmpty())
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

    public String[] headerNames()
    {
        return headers.keySet().toArray(new String[0]);
    }

    /**
     * If set return the total call duration in millis
     * @return the duration of req-->resp in millis
     */
    public long getDuration() {
        return duration;
    }

}
