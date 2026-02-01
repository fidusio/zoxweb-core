/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.shared.http;

import org.zoxweb.shared.util.GetNVProperties;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.NVGenericMap;

import java.util.List;
import java.util.Map;

/**
 * Abstract base class representing an HTTP response.
 * Provides access to the response status, headers, and timing information.
 *
 * @author mnael
 * @see HTTPAPIResult
 * @see HTTPResponseData
 */
public abstract class HTTPResponse
        implements GetNVProperties {
    private final int status;
    private final Map<String, List<String>> headers;
    private final NVGenericMap nvGenericMap;


    private final long duration;


    /**
     * Constructs an HTTPResponse with the specified status, headers, and duration.
     *
     * @param status   the HTTP status code
     * @param headers  the response headers
     * @param duration the request/response duration in milliseconds
     */
    protected HTTPResponse(int status, Map<String, List<String>> headers, long duration) {
        this.status = status;
        this.headers = headers;
        this.duration = duration;
        this.nvGenericMap = null;
    }

    protected HTTPResponse(int status, NVGenericMap headers, long duration) {
        this.status = status;
        this.headers = null;
        this.duration = duration;
        this.nvGenericMap = headers;
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Returns all response headers as a map.
     *
     * @return the headers map
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public NVGenericMap headers() {
        return nvGenericMap;
    }

    public NVGenericMap getProperties() {
        return headers();
    }

    /**
     * Returns all values for the specified header.
     *
     * @param headerName the header name
     * @return list of header values
     */
    public List<String> headerValues(GetName headerName) {
        return headerValues(headerName.getName());
    }

    /**
     * Returns all values for the specified header.
     *
     * @param headerName the header name as string
     * @return list of header values
     */
    public List<String> headerValues(String headerName) {
        return headers != null ? headers.get(headerName) : null;
    }

    /**
     * Returns the first value for the specified header.
     *
     * @param headerName the header name
     * @return the first header value, or null if not found
     */
    public String headerValue(String headerName) {
        if (headers() != null) {
            return headers().getValue(headerName);
        }
        List<String> headerValue = headers.get(headerName);
        if (headerName != null && !headerValue.isEmpty())
            return headerValue.get(0);

        return null;
    }

    /**
     * Returns a header value parsed as a long.
     *
     * @param headerName the header name
     * @return the header value as long
     * @throws NullPointerException if header not found
     */
    public long longHeaderValue(String headerName) {
        String val = headerValue(headerName);
        if (val == null)
            throw new NullPointerException(headerName + " not found");
        return Long.parseLong(val);
    }

    /**
     * Returns a header value parsed as a float.
     *
     * @param headerName the header name
     * @return the header value as float
     * @throws NullPointerException if header not found
     */
    public float floatHeaderValue(String headerName) {
        String val = headerValue(headerName);
        if (val == null)
            throw new NullPointerException(headerName + " not found");
        return Float.parseFloat(val);
    }

    /**
     * Returns a header value parsed as a double.
     *
     * @param headerName the header name
     * @return the header value as double
     * @throws NullPointerException if header not found
     */
    public double doubleHeaderValue(String headerName) {
        String val = headerValue(headerName);
        if (val == null)
            throw new NullPointerException(headerName + " not found");
        return Double.parseDouble(val);
    }

    /**
     * Returns a header value parsed as an int.
     *
     * @param headerName the header name
     * @return the header value as int
     * @throws NullPointerException if header not found
     */
    public int intHeaderValue(String headerName) {
        return (int) longHeaderValue(headerName);
    }

    /**
     * Returns all header names.
     *
     * @return array of header names
     */
    public String[] headerNames() {
        if(headers() != null)
        {
            headers().getValue().keySet().toArray(new String[0]);
        }
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
