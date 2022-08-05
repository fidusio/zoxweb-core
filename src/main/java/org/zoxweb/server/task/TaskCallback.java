package org.zoxweb.server.task;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Task callback interface that contains both the consumer and supplier
 * @param <C> Consumer data type
 * @param <S> Supplier data type
 */

public interface TaskCallback<C, S>
        extends Consumer<C>, Supplier<S>
{
    void exception(Exception e);
}
