package org.zoxweb.shared.task;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SupplierConsumerTask<T>
    implements Runnable
{
    private final Supplier<T> supplier;
    private final Consumer<T> consumer;



    public SupplierConsumerTask(Consumer<T> consumer)
    {
        this(null, consumer);
    }

    public SupplierConsumerTask(Supplier<T> supplier, Consumer<T> consumer)
    {
        this.supplier = supplier;
        this.consumer = consumer;
    }

    @Override
    public void run()
    {
        consumer.accept(supplier != null ? supplier.get() : null);
    }

}
