/*
 * Copyright (c) 2012-2023 ZoxWeb.com LLC.
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
package org.zoxweb.server.http;

import org.zoxweb.shared.http.HTTPAPIResult;
import org.zoxweb.shared.task.ConsumerSupplierCallback;

public abstract class HTTPCallBack<I,O>
    implements ConsumerSupplierCallback<HTTPAPIResult<O>,I>
{

    private transient HTTPAPIEndPoint<I,O> endpoint;

    protected I input;
    public HTTPCallBack(I input)
    {
        set(input);
    }

    public HTTPCallBack()
    {
    }


    @Override
    public I get() 
    {
        return input;
    }

    public HTTPCallBack<I,O> set(I input)
    {
        this.input = input;
        return this;
    }

    public HTTPCallBack<I,O> setEndpoint(HTTPAPIEndPoint<I,O> endpoint)
    {
        this.endpoint = endpoint;
        return this;
    }

    public HTTPAPIEndPoint<I,O> getEndpoint()
    {
        return endpoint;
    }
}
