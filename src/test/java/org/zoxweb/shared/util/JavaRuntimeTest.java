package org.zoxweb.shared.util;

import org.junit.jupiter.api.Test;
import org.zoxweb.server.util.RuntimeUtil;

public class JavaRuntimeTest {

    @Test
    public void javaVersion()
    {
        System.out.println(RuntimeUtil.getJavaVersion());
    }

    @Test
    public void javaVersionLookup()
    {
        String[] versions ={"1.3", "10", "12", "1.2", "11", "1.5", "1.8"};
        for(String version : versions)
        {
            System.out.println(version + ": " + Const.JavaClassVersion.lookup(version));
        }
    }

}
