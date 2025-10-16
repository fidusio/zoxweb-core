package org.zoxweb.shared.net;

public enum ProxyType {
    /**
     * Represents a direct connection, or the absence of a proxy.
     */
    DIRECT,
    /**
     * Represents proxy for high level protocols such as HTTP or FTP.
     */
    HTTP,
    /**
     * Represents a SOCKS (V4 or V5) proxy.
     */
    SOCKS
}
