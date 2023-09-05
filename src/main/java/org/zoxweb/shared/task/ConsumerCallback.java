package org.zoxweb.shared.task;


import java.util.function.Consumer;

public interface ConsumerCallback<C>
    extends Consumer<C>
{
    void exception(Exception e);
}
