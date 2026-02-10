package org.zoxweb.shared.net;

import java.io.IOException;

public interface DNSResolverInt {
    IPAddress resolveIPAddress(String host) throws IOException;
}
