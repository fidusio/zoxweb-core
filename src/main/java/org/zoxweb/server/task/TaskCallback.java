package org.zoxweb.server.task;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface TaskCallback<P, S>
        extends Consumer<P>, Supplier<S>
{
    void exception(Exception e);
}
