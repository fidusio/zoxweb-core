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
    public void exception(Exception e) {
       e.printStackTrace();
    }



    @Override
    public I get() {
        return input;
    }

    public HTTPCallBack<I,O> set(I input)
    {
        this.input = input;
        return this;
    }

    public HTTPCallBack<I,O> setEndPoint(HTTPAPIEndPoint<I,O> endpoint)
    {
        this.endpoint = endpoint;
        return this;
    }

    public HTTPAPIEndPoint<I,O> getEndpoint()
    {
        return endpoint;
    }
}
