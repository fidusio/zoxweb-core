package org.zoxweb.server.net;

import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.task.ConsumerSupplierCallback;

/**
 * Session callback
 * @param <CF> Session configuration
 * @param <C> Session consumer
 * @param <S> Session supplier
 */
public abstract class SessionCallback<CF, C, S> implements ConsumerSupplierCallback<C,S> {
    public static final LogWrapper log = new LogWrapper(SessionCallback.class);
    private CF config;

    public final CF getConfig(){return config;}
    public void setConfig(CF config)
    {
        this.config = config;
    }

    @Override
    public void exception(Exception e)
    {
        log.getLogger().info("" + e);
    }
}
