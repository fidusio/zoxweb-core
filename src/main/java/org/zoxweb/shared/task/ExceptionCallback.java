package org.zoxweb.shared.task;

public interface ExceptionCallback
{

    void exception(Throwable e);

    default boolean isStackTraceEnabled(){return false;}
}
