package org.zoxweb.shared.task;

import java.util.function.Supplier;

/**
 * Task callback interface that contains both the consumer and supplier
 * @param <C> Consumer data type
 * @param <S> Supplier data type
 */

public interface ConsumerSupplierCallback<C, S>
        extends ConsumerCallback<C>, Supplier<S>
{
}
