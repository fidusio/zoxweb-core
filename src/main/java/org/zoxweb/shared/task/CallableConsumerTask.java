package org.zoxweb.shared.task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class CallableConsumerTask<V>
implements CallableConsumer<V>
{

    private Callable<V> callable;
    private Consumer<V> consumer;
    private ExceptionCallback exceptionCallback;
    public CallableConsumerTask()
    {
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    public V call() throws Exception
    {
        return callable != null ? callable.call() : null;
    }

    /**
     * Performs this operation on the given argument.
     *
     * @param v the input argument
     */
    @Override
    public void accept(V v)
    {
        if(consumer != null)
            consumer.accept(v);
        else if (callable instanceof Consumer)
            ((Consumer<V>) callable).accept(v);
    }

    @Override
    public void exception(Exception e)
    {
        if(exceptionCallback != null)
            exceptionCallback.exception(e);
        else if (callable instanceof ExceptionCallback)
            ((ExceptionCallback) callable).exception(e);
    }

    public CallableConsumerTask<V> setCallable(Callable<V> callable)
    {
        this.callable = callable;
        return this;
    }

    public CallableConsumerTask<V> setConsumer(Consumer<V> consumer)
    {
        this.consumer = consumer;
        return this;
    }

    public CallableConsumerTask<V> setExceptionCallback(ExceptionCallback exceptionCallback)
    {
        this.exceptionCallback = exceptionCallback;
        return this;
    }


}
