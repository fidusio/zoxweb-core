package org.zoxweb.shared.task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public interface CallableConsumerTask<V>
    extends Callable<V>, Consumer<V>
{
    default void exception(Exception e){};
    default boolean isExceptionStackTraceEnabled(){return false;};
}
