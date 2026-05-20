package org.zoxweb.shared.util;

import java.util.List;

public class ListAsArray<T> {
    private final List<T> list;
    private final T[] empty;
    private volatile T[] vals;

    public ListAsArray(List<T> list, T[] empty) {
        this.list = list;
        this.empty = empty;
        this.vals = list.toArray(empty);
    }

    public boolean add(T... t) {
        synchronized (this) {
            boolean ret = false;
            for (int i = 0; i < t.length; i++) {
                ret = list.add(t[i]);
            }
            this.vals = list.toArray(empty);
            return ret;
        }
    }

    public boolean remove(T... t) {
        synchronized (this) {
            boolean ret = false;
            for (int i = 0; i < t.length; i++) {
                ret = list.remove(t[i]);
            }
            this.vals = list.toArray(empty);
            return ret;
        }
    }

    public T remove(int index) {
        synchronized (this) {
            T ret = list.remove(index);
            this.vals = list.toArray(empty);
            return ret;
        }
    }

    public void clear() {
        synchronized (this) {
            list.clear();
            this.vals = list.toArray(empty);
        }
    }

    public T[] asArray() {
        return vals;
    }
}
