package org.zoxweb.shared.app;

import org.zoxweb.shared.util.AppConfig;
import org.zoxweb.shared.util.GetName;

import java.io.IOException;

public interface AppCreator<A, C extends AppConfig>
        extends GetName, AutoCloseable {
    AppCreator<A, C> setAppConfig(C appConfig);

    AppCreator<A, C> setName(String name);

    C getAppConfig();

    A createApp() throws NullPointerException, IllegalArgumentException, IOException;
}
