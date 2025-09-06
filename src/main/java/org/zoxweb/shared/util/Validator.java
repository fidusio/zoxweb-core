package org.zoxweb.shared.util;

public interface Validator<V> {
    /**
     * Check if the value is valid
     * @param in value to be checked
     * @return true if in value valid
     */
    boolean isValid(V in);
}
