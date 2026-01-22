package org.zoxweb.shared.util;

public interface UsageTracker
        extends IsExpired, AutoCloseable {

    /**
     *
     * @return last usage
     */
    long lastUsage();


    /**
     * @return current usage update
     */
    long updateUsage();


    long updateUsage(long value);


}
