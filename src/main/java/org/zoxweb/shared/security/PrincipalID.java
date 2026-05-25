package org.zoxweb.shared.security;

public interface PrincipalID<T> {
    void setPrincipalID(T id);
    T getPrincipalID();
}
