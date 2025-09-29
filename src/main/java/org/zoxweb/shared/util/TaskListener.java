package org.zoxweb.shared.util;

import java.util.EventListener;

public interface TaskListener<T, R>
        extends EventListener {
    void started(T t);

    void executionResult(int status, long executionCounter, long timestamp, R result);

    void terminated(T t);

}
