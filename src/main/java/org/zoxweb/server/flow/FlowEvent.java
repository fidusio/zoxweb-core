package org.zoxweb.server.flow;


import java.util.EventObject;
import java.util.function.Supplier;


public class FlowEvent<T>
        extends EventObject
        implements Supplier<FlowEvent<T>> {
    private final long timestamp;
    protected T flowType;
    private long sequence;


    public FlowEvent(T type) {
        this(type, type);
    }


    public FlowEvent(Object source, T type) {
        super(source);
        this.flowType = type;
        this.timestamp = System.currentTimeMillis();
    }

    public T getFlowType() {
        return flowType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    protected void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public long getSequence() {
        return sequence;
    }

    public FlowEvent<T> get() {
        return this;
    }

}
