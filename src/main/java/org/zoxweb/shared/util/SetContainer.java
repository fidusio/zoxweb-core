package org.zoxweb.shared.util;

import java.util.HashSet;
import java.util.Set;

public abstract class SetContainer<T> {
    protected volatile Set<T> set = new HashSet<>();

    public synchronized <C extends SetContainer<T>> C add(T t) {
        set.add(t);
        return (C) this;
    }

    public boolean contains(T t) {
        return set.contains(t);
    }


    public synchronized <C extends SetContainer<T>> C remove(T t) {
        set.remove(t);
        return (C) this;
    }

    public abstract T validate(T t) throws IllegalArgumentException, NullPointerException;

}
