package org.zoxweb.server.net;

import org.zoxweb.server.task.TaskCallback;

import java.util.logging.Logger;

public abstract class SessionCallback<C, P, S> implements TaskCallback<P,S> {
    protected  static final transient Logger log = Logger.getLogger(SessionCallback.class.getName());
    private C config;

    public final C getConfig(){return config;}
    public final void setConfig(C config){this.config = config;}
    @Override
    public void exception(Exception e) {
        log.info("" + e);
    }


}
