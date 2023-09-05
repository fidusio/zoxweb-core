package org.zoxweb.shared.task;

import java.util.function.Supplier;

public abstract class SupplierTask<T>
implements Runnable, Supplier<T>
{

    private final T param;
    public SupplierTask(T param)
    {
        this.param = param;
    }


    @Override
    public T get() {
        return param;
    }
}
