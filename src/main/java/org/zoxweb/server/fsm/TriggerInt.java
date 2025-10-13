package org.zoxweb.server.fsm;


import java.util.function.Supplier;

public interface TriggerInt<D>
        extends Supplier<D> {
    StateInt lastState();

    String getCanonicalID();

    long getID();

    long getTimestamp();
}
