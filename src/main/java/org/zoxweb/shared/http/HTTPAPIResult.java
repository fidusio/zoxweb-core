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

import org.zoxweb.shared.util.Const;

import java.util.List;
import java.util.Map;

/**
 * Generic HTTP API result container that extends {@link HTTPResponse} to include typed response data.
 * This class encapsulates the HTTP response status, headers, and the deserialized response payload
 * of a specified type.
 *
 * @param <O> the type of the response data payload
 * @author mnael
 */
public class HTTPAPIResult<O>
extends HTTPResponse
{

    private final O data;

    /**
     * Constructs an HTTPAPIResult with the specified status, headers, data, and duration.
     *
     * @param status   the HTTP status code of the response
     * @param headers  the HTTP response headers as a map of header names to lists of values
     * @param data     the deserialized response data payload
     * @param duration the time taken for the HTTP call in milliseconds
     */
    public HTTPAPIResult(int status, Map<String, List<String>> headers, O data, long duration)
    {
        super(status, headers, duration);
        this.data = data;
    }


    /**
     * Returns the deserialized response data payload.
     *
     * @return the response data of type O
     */
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
