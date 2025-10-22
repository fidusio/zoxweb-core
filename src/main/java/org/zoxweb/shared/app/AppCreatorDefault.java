package org.zoxweb.shared.app;

import org.zoxweb.shared.util.AppConfig;


public abstract class AppCreatorDefault<A, C extends AppConfig>
        implements AppCreator<A, C> {
    private String name;
    private C appConfig;


    @Override
    public AppCreator<A, C> setAppConfig(C appConfig) {
        this.appConfig = appConfig;
        return this;
    }

    @Override
    public AppCreator<A, C> setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public C getAppConfig() {
        return appConfig;
    }


    @Override
    public String getName() {
        return name;
    }
}
