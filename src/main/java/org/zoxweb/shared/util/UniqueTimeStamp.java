package org.zoxweb.shared.util;

public class UniqueTimeStamp {

    private long lastTimeStamp = System.currentTimeMillis();

    public UniqueTimeStamp() {
    }

    public long uniqueTimeStampInMillis() {
        long currentTimeStamp = System.currentTimeMillis();
        synchronized (this) {
            if (currentTimeStamp > lastTimeStamp) {
                lastTimeStamp = currentTimeStamp;
            } else {
                currentTimeStamp = ++lastTimeStamp;

            }
        }
        return currentTimeStamp;
    }
}
