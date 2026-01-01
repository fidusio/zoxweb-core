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
import org.zoxweb.shared.util.NVGenericMap;

/**
 * Generic container for an HTTP result combining message configuration and typed data.
 * Pairs an {@link HTTPMessageConfig} with the response data of a specified type.
 *
 * @param <D> the type of the response data
 * @author mnael
 */
public class HTTPResult<D>
{
    private final HTTPMessageConfig hmci = new HTTPMessageConfig();
    private final D data;

    /**
     * Constructs an HTTPResult with the specified data.
     *
     * @param data the response data
     */
    public HTTPResult(D data)
    {
        this.data = data;
    }

    /**
     * Returns the HTTP message configuration.
     *
     * @return the message configuration
     */
    public HTTPMessageConfigInterface getHTTPMessageConfig()
    {
        return hmci;
    }

    /**
     * Returns the response data.
     *
     * @return the data of type D
     */
    public D getData(){return data;}

}
