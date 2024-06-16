package org.zoxweb.shared.task;

import java.util.concurrent.Callable;

public interface CallableConsumer<V>
    extends Callable<V>, ConsumerCallback<V>
{

}
