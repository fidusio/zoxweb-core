package org.zoxweb.shared.task;

public interface Publisher<T>
{
    void publish(T event);
}
