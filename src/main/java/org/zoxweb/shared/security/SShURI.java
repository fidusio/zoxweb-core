package org.zoxweb.shared.security;


/**
 * SSH URI is a util to represent an ssh login as uri it is composed user@host:[port default 22] the host can be ip4 or ip6 if ip [ip6] to avoid confusion with port
 */
public class SShURI {
    public final String user;
    public final String host;
    public final int port;
    public final boolean ipV4;// For internal use to distinguish between ipv4 and ipv6


    public SShURI(String host, int port, String user) {
        this.host = host;
        this.port = port;
        this.user = user;
        ipV4 = (host.split(":").length < 2);

    }

    /**
     *
     * @return user@host:[port default 22]
     */
    @Override
    public String toString() {
        return user + "@" + (!ipV4 ? "[" : "") + host + (!ipV4 ? "]" : "") + ":" + port;
    }

    public static SShURI parse(String uri) {

        int atIdx = uri.indexOf('@');
        if (atIdx < 0) throw new IllegalArgumentException("Missing user@ part");
        String user = uri.substring(0, atIdx);

        String hostPort = uri.substring(atIdx + 1);
        String host;
        int port = 22;

        // IPv6 addresses in SSH URIs are always in brackets: [IPv6]:port
        if (hostPort.startsWith("[")) {
            int closingIdx = hostPort.indexOf(']');
            if (closingIdx < 0)
                throw new IllegalArgumentException("Malformed IPv6 address, missing ']'");
            host = hostPort.substring(1, closingIdx);
            if (hostPort.length() > closingIdx + 1 && hostPort.charAt(closingIdx + 1) == ':') {
                port = Integer.parseInt(hostPort.substring(closingIdx + 2));
            }
        } else {
            int colonIdx = hostPort.lastIndexOf(':'); // use last, for IPv6 safety
            if (colonIdx > -1 && hostPort.indexOf(':') == colonIdx) {
                // Only one colon, not IPv6
                host = hostPort.substring(0, colonIdx);
                port = Integer.parseInt(hostPort.substring(colonIdx + 1));
            } else {
                host = hostPort;
            }
        }


        return new SShURI(host, port, user);
    }
}
