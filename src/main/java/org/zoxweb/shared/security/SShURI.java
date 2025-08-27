package org.zoxweb.shared.security;


import org.zoxweb.shared.util.SUS;

/**
 * SSH URI is a util to represent an ssh login as uri it is composed user@host:[port default 22] the host can be ip4 or ip6 if ip [ip6] to avoid confusion with port
 */
public class SShURI {
    public final String subject;
    public final String credential;
    public final String host;
    public final int port;

    public final boolean ipV4;// For internal use to distinguish between ipv4 and ipv6


    public SShURI(String subject, String password, String host, int port) {
        this.host = host;
        this.port = port;
        this.subject = subject;
        this.credential = password;
        ipV4 = (host.split(":").length < 2);
    }

    /**
     *
     * @return user@host:[port default 22]
     */
    @Override
    public String toString() {
        return subject + (credential != null ? ":" + credential : "") + "@" + (!ipV4 ? "[" : "") + host + (!ipV4 ? "]" : "") + ":" + port;
    }

    public static SShURI parse(String uri) {

        if (SUS.isEmpty(uri)) throw new IllegalArgumentException("invalid ssh uri is null");
        String s = SUS.trimOrNull(uri);

        // Split credentials vs host part at the LAST '@' so a password can contain '@'
        int at = s.lastIndexOf('@');
        if (at <= 0 || at == s.length() - 1) {
            throw new IllegalArgumentException("Expected user[:password]@host[:port]");
        }

        String cred = s.substring(0, at);
        String hostPort = s.substring(at + 1);

        // Parse user[:password]
        String user;
        String password = null;
        int colonInCred = cred.indexOf(':');
        if (colonInCred >= 0) {
            user = cred.substring(0, colonInCred);
            password = cred.substring(colonInCred + 1);
        } else {
            user = cred;
        }
        if (user.isEmpty()) throw new IllegalArgumentException("Missing user");

        // Parse host[:port]
        String host;
        int port = 22; // default

        if (hostPort.startsWith("[")) {
            // Bracketed IPv6 (optionally with zone id)
            int close = hostPort.indexOf(']');
            if (close < 0) throw new IllegalArgumentException("Malformed IPv6: missing ']'");
            host = hostPort.substring(1, close); // keep zone id as-is (e.g., fe80::1%25eth0)

            // Optional :port after the closing bracket
            if (hostPort.length() > close + 1) {
                if (hostPort.charAt(close + 1) != ':') {
                    throw new IllegalArgumentException("Unexpected characters after IPv6 ']'");
                }
                String portStr = hostPort.substring(close + 2);
                port = parsePort(portStr);
            }
        } else {
            // Not bracketed: could be domain, IPv4, or (rarely) bare IPv6 without port.
            // If there is exactly one colon, treat it as host:port; otherwise it's all host.
            int firstColon = hostPort.indexOf(':');
            int lastColon = hostPort.lastIndexOf(':');
            if (firstColon >= 0 && firstColon == lastColon) {
                // Exactly one colon -> host:port
                host = hostPort.substring(0, firstColon);
                String portStr = hostPort.substring(firstColon + 1);
                port = parsePort(portStr);
            } else {
                // 0 colons OR multiple colons -> treat entire thing as host (no port)
                // (Bare IPv6 without port must be unbracketed here)
                host = hostPort;
            }
        }

        if (host.isEmpty()) throw new IllegalArgumentException("Missing host");
        return new SShURI(user, password, host, port);
    }

    private static int parsePort(String p) {
        if (p == null || p.isEmpty()) throw new IllegalArgumentException("Empty port");
        try {
            int val = Integer.parseInt(p);
            if (val < 1 || val > 65535) throw new IllegalArgumentException("Port out of range: " + val);
            return val;
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Invalid port: " + p, nfe);
        }
    }
}
