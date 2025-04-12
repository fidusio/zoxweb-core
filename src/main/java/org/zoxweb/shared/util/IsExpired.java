package org.zoxweb.shared.util;

public interface IsExpired
{

    /**
     * @return true If the implementation is expired
     */
    default boolean isExpired(){return false;};

}
