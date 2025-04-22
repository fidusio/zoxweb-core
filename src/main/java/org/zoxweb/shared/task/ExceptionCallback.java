package org.zoxweb.shared.task;

public interface ExceptionCallback
{
    default void exception(Exception e){}
    default boolean isStackTraceEnabled(){return false;}
}
