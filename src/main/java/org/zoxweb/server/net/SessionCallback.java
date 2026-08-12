package org.zoxweb.server.net;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.io.CloseableTypeDelegate;
import org.zoxweb.shared.task.ConsumerSupplierCallback;

/**
 * Root of the session callback hierarchy: ties a session to its configuration object, an optional
 * {@link ProtocolHandler} and the shared {@link CloseableTypeDelegate} that gives every session
 * one-time close semantics — subclasses arm it with a teardown lambda via
 * {@code closeableDelegate.setDelegate(...)} and route their close/isClosed through it.
 * <p>
 * The type parameters shape the concrete callback: {@code C} is the data unit the session
 * consumes via {@link #accept(Object)} (ByteBuffer for the byte-stream callbacks under
 * {@link BaseSessionCallback}, DataPacket for packet-oriented ones), {@code S} is what the
 * session supplies (typically an OutputStream), and {@code CF} is the configuration the events
 * are driven from (e.g. a state machine). Note: because {@code accept(C)} erases to
 * {@code accept(Object)}, a subclass cannot also implement another interface's same-named
 * generic consumer with a different type argument — pick {@code C} to match.
 *
 * @param <CF> Session configuration
 * @param <C>  Session consumer
 * @param <S>  Session supplier
 */
public abstract class SessionCallback<CF, C, S> implements ConsumerSupplierCallback<C, S> {
    public static final LogWrapper log = new LogWrapper(SessionCallback.class).setEnabled(false);
    protected volatile CF config;
    protected volatile ProtocolHandler protocolHandler;
    protected final CloseableTypeDelegate closeableDelegate = new CloseableTypeDelegate(null, false);


    /**
     * @return the session configuration
     */
    public final CF getConfig() {
        return config;
    }

    /**
     * Sets the session configuration.
     *
     * @param config the session configuration
     */
    public final void setConfig(CF config) {
        this.config = config;
    }

    /**
     * Consumes one session data unit.
     *
     * @param t the data unit to consume
     */
    public abstract void accept(C t);


}
