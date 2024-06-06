package org.zoxweb.shared.task;

import java.util.concurrent.Callable;

public interface ConsumerCallable<V>
    extends Callable<V>, ConsumerCallback<V>
{

}
