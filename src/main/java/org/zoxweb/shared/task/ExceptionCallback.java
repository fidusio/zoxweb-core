package org.zoxweb.shared.task;

public interface ExceptionCallback
{
    default void exception(Throwable e){}
    default boolean isStackTraceEnabled(){return false;}
}
