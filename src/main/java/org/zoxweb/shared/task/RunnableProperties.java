package org.zoxweb.shared.task;

import org.zoxweb.shared.util.NVGMProperties;
import org.zoxweb.shared.util.NVGenericMap;

public abstract class RunnableProperties
        extends NVGMProperties
        implements Runnable {

    public RunnableProperties() {
        super(true);
    }

    public RunnableProperties(NVGenericMap nvgm) {
        super(false);
        setProperties(nvgm);
    }
}
