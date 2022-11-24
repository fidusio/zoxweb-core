package org.zoxweb.server.net;

import org.zoxweb.server.task.TaskCallback;

import java.util.logging.Logger;

/**
 * Session callback
 * @param <CF> Session configuration
 * @param <C> Session consumer
 * @param <S> Session supplier
 */
public abstract class SessionCallback<CF, C, S> implements TaskCallback<C,S> {
    protected  static final Logger log = Logger.getLogger(SessionCallback.class.getName());
    private CF config;

    public final CF getConfig(){return config;}
    public void setConfig(CF config){this.config = config;}
    @Override
    public void exception(Exception e) {
        log.info("" + e);
    }


}
