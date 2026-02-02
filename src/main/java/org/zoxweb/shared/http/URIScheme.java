/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.shared.http;

import org.zoxweb.shared.util.GetNameValue;

/**
 * Enumeration of URI schemes (protocols) commonly used in web applications.
 * Each scheme has a name (protocol identifier) and a default port.
 * <p>
 * Supports HTTP, HTTPS, FTP, FILE, MAILTO, DATA, WebSocket (WS), and secure WebSocket (WSS).
 * </p>
 *
 * @author mnael
 */
public enum URIScheme
        implements GetNameValue<Integer> {

    // WARNING: it is crucial to define https before http otherwise the match will never detect https
    /** Secure HTTP (port 443) */
    HTTPS("https", 443),
    /** HTTP (port 80) */
    HTTP("http", 80),
    /** FTP (port 21) */
    FTP("ftp", 21),
    /** File system access (no port) */
    FILE("file", -1),
    /** Email mailto links (no port) */
    MAIL_TO("mailto", -1),
    /** Data URLs (no port) */
    DATA("data", -1),
    /** Secure WebSocket (port 443) */
    WSS("wss", 443),
    /** WebSocket (port 80) */
    WS("ws", 80),
    SSH("ssh", 22),

    ;

    private final String name;
    private final int defaultPort;

    URIScheme(String name, int port) {
        this.name = name;
        this.defaultPort = port;
    }

    /**
     * Matches a URI string to its corresponding URIScheme.
     *
     * @param uri the URI string to match
     * @return the matching URIScheme, or null if not found
     */
    public static URIScheme match(String uri, URIScheme... schemes) {
        if (uri != null) {
            uri = uri.toLowerCase().trim();

            if (schemes == null || schemes.length == 0)
                schemes = URIScheme.values();

            for (URIScheme us : schemes) {
                if (uri.startsWith(us.getName())) {
                    return us;
                }
            }
        }

        return null;
    }

    /**
     * Return true if the uri is matching any of the specified URIScheme
     * @param uri to check
     * @param schemes to check against to
     * @return true is match is found
     */
    public static boolean isMatching(String uri, URIScheme... schemes) {
        return match(uri, schemes) != null;
    }

    @Override
    public String getName() {
        return name;
    }

    public String toString() {
        return getName();
    }

    /**
     * @return the default port -1 not defined
     */
    @Override
    public Integer getValue() {
        return defaultPort;
    }

    public int defaultPort() {
        return defaultPort;
    }
}
