package org.zoxweb.shared.util;

import java.util.concurrent.atomic.AtomicLong;

public class MinMax
        implements GetName {
    private volatile AtomicLong min = null;
    private volatile AtomicLong max = null;
    private final String name;

    public MinMax(String name) {
        this.name = name;
    }

    public MinMax(String name, long min, long max) {
        this(name);
        this.min = new AtomicLong(min);
        this.max = new AtomicLong(max);
    }

    public long getMin() {
        if (min == null)
            throw new IllegalStateException("update has not been called");
        return min.get();
    }

    public long getMax() {
        if (min == null)
            throw new IllegalStateException("update has not been called");
        return max.get();
    }

    public MinMax update(long value) {
        if (min == null && max == null) {
            synchronized (this) {
                if (min == null) {
                    min = new AtomicLong(value);
                }
                if (max == null) {
                    max = new AtomicLong(value);
                }
                return this;
            }
        }

        if (value < min.get())
            min.set(value);
        if (value > max.get())
            max.set(value);

        return this;
    }

    public String toString() {
        return "\"" +name + "\": {\"min\": " + min + ", \"max\": " + max + "}";
    }

    @Override
    public String getName() {
        return name;
    }
}
