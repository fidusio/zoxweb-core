package org.zoxweb.shared.util;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public interface WaitTime<T>
    extends Delayed
{

    /**
     * @return the next calculated wait time in lillis
     */
    long nextWait();

    T getType();


     default int compareTo(Delayed o) {
        // may cause error
        return SharedUtil.signum( getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }

    @Override
    default long getDelay(TimeUnit unit) {
        return unit.convert(nextWait(), TimeUnit.MILLISECONDS);
    }
}
