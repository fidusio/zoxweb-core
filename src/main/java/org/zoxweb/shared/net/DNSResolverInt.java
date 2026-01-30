package org.zoxweb.shared.net;

import java.io.IOException;

public interface DNSResolverInt {
    IPAddress resolveIPA(String host) throws IOException;
}
