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

import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.Validator;

import java.util.HashSet;
import java.util.Set;

/**
 * URL Validator that supports wildcard and strict domain matching.
 * <p>
 * Wildcard pattern: {@code *.xlogistx.io} matches any subdomain like {@code api.xlogistx.io},
 * {@code www.xlogistx.io}, and also the base domain {@code xlogistx.io}.
 * <p>
 * Strict pattern: {@code www.xlogistx.io,xlogistx.io} matches only the exact domains listed.
 * <p>
 * The validator extracts the host from URLs, ignoring scheme (http, https, ftp, etc.) and port.
 * <p>
 * Example usage:
 * <pre>
 * // Wildcard matching
 * URLValidator validator = new URLValidator("*.xlogistx.io");
 * validator.validate("https://api.xlogistx.io:8080"); // true
 * validator.validate("ftp://xlogistx.io"); // true
 * validator.validate("xlogistx.io"); // true
 * validator.validate("https://other.com"); // false
 *
 * // Strict matching
 * URLValidator validator = new URLValidator("www.xlogistx.io,xlogistx.io");
 * validator.validate("https://www.xlogistx.io"); // true
 * validator.validate("https://api.xlogistx.io"); // false
 * </pre>
 */
public class URLMatcher implements Validator<String> {

    private final String pattern;
    private final boolean wildcardMode;
    private final String wildcardSuffix;
    private final Set<String> strictDomains;

    /**
     * Creates a URLValidator with the specified pattern.
     *
     * @param pattern the validation pattern - either wildcard (e.g., "*.xlogistx.io")
     *                or comma-separated strict domains (e.g., "www.xlogistx.io,xlogistx.io")
     * @throws NullPointerException if pattern is null
     * @throws IllegalArgumentException if pattern is empty
     */
    public URLMatcher(String pattern) {
        SUS.checkIfNulls("Pattern cannot be null", pattern);
        this.pattern = SUS.trimOrNull(pattern);
        if (this.pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be empty");
        }

        if (this.pattern.startsWith("*.")) {
            this.wildcardMode = true;
            this.wildcardSuffix = this.pattern.substring(1).toLowerCase(); // ".xlogistx.io"
            this.strictDomains = null;
        } else {
            this.wildcardMode = false;
            this.wildcardSuffix = null;
            this.strictDomains = new HashSet<>();
            String[] domains = this.pattern.split(",");
            for (String domain : domains) {
                String trimmed = SUS.trimOrNull(domain);
                if (trimmed != null) {
                    strictDomains.add(trimmed.toLowerCase());
                }
            }
            if (strictDomains.isEmpty()) {
                throw new IllegalArgumentException("No valid domains in pattern");
            }
        }
    }

    /**
     * Validates the input URL or host against the configured pattern.
     * <p>
     * The input can be:
     * <ul>
     * <li>A full URL: {@code https://api.xlogistx.io:8080/path}</li>
     * <li>A URL without path: {@code ftp://xlogistx.io}</li>
     * <li>Just a host: {@code xlogistx.io}</li>
     * <li>A host with port: {@code api.xlogistx.io:8080}</li>
     * </ul>
     *
     * @param input the URL or host to validate
     * @return true if the input matches the pattern, false otherwise
     */
    @Override
    public boolean isValid(String input) {
        if (input == null) {
            return false;
        }

        String host = extractHost(input);
        if (host == null) {
            return false;
        }

        host = host.toLowerCase();

        if (wildcardMode) {
            return matchesWildcard(host);
        } else {
            return strictDomains.contains(host);
        }
    }


    /**
     * Extracts the host from a URL or returns the input if it's already a host.
     *
     * @param input the URL or host
     * @return the extracted host, or null if invalid
     */
    private String extractHost(String input) {
        input = SUS.trimOrNull(input);
        if (input == null) {
            return null;
        }

        String host = input;

        // Remove scheme if present (http://, https://, ftp://, etc.)
        int schemeEnd = host.indexOf("://");
        if (schemeEnd != -1) {
            host = host.substring(schemeEnd + 3);
        }

        // Remove user info if present (user:pass@)
        int atIndex = host.indexOf('@');
        if (atIndex != -1) {
            host = host.substring(atIndex + 1);
        }

        // Remove path if present
        int pathStart = host.indexOf('/');
        if (pathStart != -1) {
            host = host.substring(0, pathStart);
        }

        // Remove query string if present (shouldn't happen after removing path, but just in case)
        int queryStart = host.indexOf('?');
        if (queryStart != -1) {
            host = host.substring(0, queryStart);
        }

        // Remove port if present
        int portIndex = host.lastIndexOf(':');
        if (portIndex != -1) {
            // Make sure it's a port (all digits after colon)
            String possiblePort = host.substring(portIndex + 1);
            if (isNumeric(possiblePort)) {
                host = host.substring(0, portIndex);
            }
        }

        return SUS.trimOrNull(host);
    }

    /**
     * Checks if the host matches the wildcard pattern.
     * <p>
     * For pattern "*.xlogistx.io":
     * <ul>
     * <li>"xlogistx.io" matches (base domain)</li>
     * <li>"api.xlogistx.io" matches (subdomain)</li>
     * <li>"sub.api.xlogistx.io" matches (nested subdomain)</li>
     * <li>"other.com" does not match</li>
     * </ul>
     *
     * @param host the host to check
     * @return true if matches
     */
    private boolean matchesWildcard(String host) {
        // wildcardSuffix is ".xlogistx.io"
        // Check if host ends with the suffix (e.g., "api.xlogistx.io" ends with ".xlogistx.io")
        if (host.endsWith(wildcardSuffix)) {
            return true;
        }

        // Check if host equals the base domain (e.g., "xlogistx.io" equals "xlogistx.io")
        // wildcardSuffix is ".xlogistx.io", so base domain is "xlogistx.io"
        String baseDomain = wildcardSuffix.substring(1);
        return host.equals(baseDomain);
    }

    /**
     * Checks if the string contains only numeric characters.
     *
     * @param str the string to check
     * @return true if all characters are digits
     */
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the original pattern.
     *
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Returns whether this validator is in wildcard mode.
     *
     * @return true if wildcard mode
     */
    public boolean isWildcardMode() {
        return wildcardMode;
    }

    @Override
    public String toString() {
        return "URLValidator{" +
                "pattern='" + pattern + '\'' +
                ", wildcardMode=" + wildcardMode +
                '}';
    }
}
