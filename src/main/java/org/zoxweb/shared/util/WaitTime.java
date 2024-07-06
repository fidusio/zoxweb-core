package org.zoxweb.shared.util;

public interface WaitTime<T> {

    /**
     * @return the next calculated wait time in lillis
     */
    long nextWait();

    T getType();
}
