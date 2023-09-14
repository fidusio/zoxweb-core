package org.zoxweb.shared.http;

import org.zoxweb.shared.task.ConsumerSupplierCallback;

public abstract class HTTPCallBack<I,O>
    implements ConsumerSupplierCallback<HTTPAPIResult<O>,I>
{

    protected I input;
    public HTTPCallBack(I input)
    {
        set(input);
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
}
