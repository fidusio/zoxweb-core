package org.zoxweb.server.util;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

public class UUID7 {
    public static UUID randomUUID()
    {
        return UuidCreator.getTimeOrderedEpoch();
    }

    public static UUID fromString(String uuid)
    {
        return UUID.fromString(uuid);
    }
}
