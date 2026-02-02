package org.zoxweb.shared.http;

import org.zoxweb.shared.net.IPAddress;
import org.zoxweb.shared.net.SharedNetUtil;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

import java.util.*;

public final class URLInfo {

    public final URIScheme scheme;
    public final String username;
    public final String password;
    public final IPAddress ipAddress;
    public final String path;
    public final String query;
    public final Map<String, List<String>> params;
    public final String fragment;

    private URLInfo(URIScheme scheme, String username, String password,
                   IPAddress ipAddress,
                   String path, String query,
                   Map<String, List<String>> params,
                   String fragment) {
        this.scheme = scheme;
        this.username = username;
        this.password = password;
        this.ipAddress = ipAddress;
        this.path = path;
        this.query = query;
        this.params = params;
        this.fragment = fragment;
    }

    @Override
    public String toString() {
        return "URLInfo{" +
                "scheme='" + scheme + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", host='" + ipAddress.getInetAddress() + '\'' +
                ", port=" + ipAddress.getPort() +
                ", path='" + path + '\'' +
                ", query='" + query + '\'' +
                ", params=" + params +
                ", fragment='" + fragment + '\'' +
                '}';
    }

    public String toURL() {
        StringBuilder sb = new StringBuilder();

        // scheme://
        if (scheme != null) {
            sb.append(scheme).append("://");
        }

        // userinfo
        if (username != null) {
            sb.append(percentEncode(username));
            if (password != null) {
                sb.append(':').append(percentEncode(password));
            }
            sb.append('@');
        }

        // host (IPv6 needs brackets)
        String host = ipAddress.getInetAddress();
        if (host != null) {
            if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
                sb.append('[').append(host).append(']');
            } else {
                sb.append(host);
            }
        }


        // port
        if (ipAddress.getPort() >= 0) {
            sb.append(':').append(ipAddress.getPort());
        }

        // path
        if (path != null && !path.isEmpty()) {
            if (!path.startsWith("/")) sb.append('/');
            sb.append(percentEncodePath(path));
        }

        // query
        if (params != null && !params.isEmpty()) {
            sb.append('?');
            boolean first = true;
            for (Map.Entry<String, List<String>> e : params.entrySet()) {
                for (String v : e.getValue()) {
                    if (!first) sb.append('&');
                    first = false;
                    sb.append(percentEncode(e.getKey()));
                    if (v != null && !v.isEmpty()) {
                        sb.append('=').append(percentEncode(v));
                    }
                }
            }
        } else if (query != null) {
            sb.append('?').append(query); // fallback to raw query if params unused
        }

        // fragment
        if (fragment != null) {
            sb.append('#').append(percentEncode(fragment));
        }

        return sb.toString();
    }
    public String toBasicURL()
    {
        return  scheme + "://" + ipAddress.getInetAddress() + ":" + ipAddress.getPort();
    }



    public String toURI() { // request-target (origin-form)
        StringBuilder sb = new StringBuilder();

        // 1) Path: must be present; if empty => "/"
        String p = (path == null || path.isEmpty()) ? "/" : path;

        // Ensure it starts with '/'
        if (p.charAt(0) != '/') sb.append('/');
        sb.append(percentEncodePath(p)); // keeps '/' unescaped

        // 2) Query: append if present (either rebuild from params, or use raw query)
        String q = buildQueryFromParams(); // preferred (encoded)
        if (q == null || q.isEmpty()) q = query; // fallback to raw query if you kept it

        if (q != null && !q.isEmpty()) {
            sb.append('?').append(q);
        }

        // 3) Fragment: NEVER in request-target
        return sb.toString();
    }

    private String buildQueryFromParams() {
        if (params == null || params.isEmpty()) return null;

        StringBuilder q = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, List<String>> e : params.entrySet()) {
            String k = e.getKey();
            List<String> values = e.getValue();
            if (values == null || values.isEmpty()) values = Collections.singletonList("");

            for (String v : values) {
                if (!first) q.append('&');
                first = false;

                q.append(percentEncode(k == null ? "" : k));
                if (v != null && !v.isEmpty()) {
                    q.append('=').append(percentEncode(v));
                }
                // if v is empty -> emit "key" (no '='). If you prefer "key=" change this behavior.
            }
        }
        return q.toString();
    }



    public static URLInfo parse(String input) {
        Objects.requireNonNull(input);
        String s = input.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Empty URL");

        int schemeIdx = s.indexOf("://");
        if (schemeIdx <= 0) throw new IllegalArgumentException("Missing scheme");

        String scheme = SUS.trimOrNull(s.substring(0, schemeIdx));
        URIScheme uriScheme = URIScheme.match(scheme);
        String rest = s.substring(schemeIdx + 3);

        int authorityEnd = firstIndexOfAny(rest, '/', '?', '#');
        String authority = authorityEnd >= 0 ? rest.substring(0, authorityEnd) : rest;
        String remainder = authorityEnd >= 0 ? rest.substring(authorityEnd) : "";

        String username = null, password = null;

        int at = authority.lastIndexOf('@');
        String hostport;
        if (at >= 0) {
            String userinfo = authority.substring(0, at);
            hostport = authority.substring(at + 1);

            int c = userinfo.indexOf(':');
            if (c >= 0) {
                username = SUS.trimOrNull(userinfo.substring(0, c));
                password = SUS.trimOrNull(userinfo.substring(c + 1));
            } else {
                username = SUS.trimOrNull(userinfo);
            }
        } else {
            hostport = authority;
        }

        IPAddress ipAddress = parseHostPort(hostport);
        if (ipAddress.getPort() == -1) ipAddress.setPort(uriScheme.defaultPort());


        String fragment = null;
        int hash = remainder.indexOf('#');
        if (hash >= 0) {
            fragment = SUS.trimOrNull(remainder.substring(hash + 1));
            remainder = remainder.substring(0, hash);
        }

        String query = null;
        int q = remainder.indexOf('?');
        if (q >= 0) {
            query = SUS.trimOrNull(remainder.substring(q + 1));
            remainder = remainder.substring(0, q);
        }

        String path = remainder;

        Map<String, List<String>> params = parseQueryParams(query);

        return new URLInfo(
                uriScheme, username, password,
                ipAddress,
                path, query, params, fragment
        );
    }

  /* =========================
     Query parsing & decoding
     ========================= */

    private static Map<String, List<String>> parseQueryParams(String query) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) return out;

        for (String part : query.split("&", -1)) {
            if (part.isEmpty()) continue;

            String key, value;
            int eq = part.indexOf('=');
            if (eq >= 0) {
                key = part.substring(0, eq);
                value = part.substring(eq + 1);
            } else {
                key = part;
                value = "";
            }

            key = percentDecode(key);
            value = percentDecode(value);

            out.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return out;
    }

    /**
     * Percent-decode using UTF-8
     * + -> space
     * %HH -> byte
     */
    private static String percentDecode(String s) {
        byte[] buf = new byte[s.length()];
        int len = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '+') {
                buf[len++] = (byte) ' ';
            } else if (c == '%') {
                if (i + 2 >= s.length())
                    throw new IllegalArgumentException("Bad % encoding");

                int hi = SharedStringUtil.hexToInt(s.charAt(i + 1));
                int lo = SharedStringUtil.hexToInt(s.charAt(i + 2));
                if (hi < 0 || lo < 0)
                    throw new IllegalArgumentException("Bad % encoding");

                buf[len++] = (byte) ((hi << 4) | lo);
                i += 2;
            } else {
                buf[len++] = (byte) c;
            }
        }
        return SharedStringUtil.toString(buf, 0, len);
    }



    private static IPAddress parseHostPort(String hp) {
        if (hp == null || hp.isEmpty()) return new IPAddress("", -1, -1, null);

        if (hp.startsWith("[")) {
            int end = hp.indexOf(']');
            if (end < 0) throw new IllegalArgumentException("Bad IPv6 host");
            String host = hp.substring(1, end);
            int port = -1;

            if (end + 1 < hp.length() && hp.charAt(end + 1) == ':') {
                port = SharedNetUtil.parsePort(hp.substring(end + 2));
            }
            return new IPAddress(host, port, -1, null);
        }

        int lastColon = hp.lastIndexOf(':');
        if (lastColon >= 0) {
            String maybePort = hp.substring(lastColon + 1);
            if (SharedStringUtil.isDigits(maybePort)) {
                return new IPAddress(hp.substring(0, lastColon), SharedNetUtil.parsePort(maybePort), -1, null);
            }
        }
        return new IPAddress(hp, -1, -1, null);
    }



    /* ========================= */



    private static int firstIndexOfAny(String s, char... cs) {
        int best = -1;
        for (char c : cs) {
            int i = s.indexOf(c);
            if (i >= 0 && (best < 0 || i < best)) best = i;
        }
        return best;
    }

    private static String percentEncode(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isUnreserved(c)) {
                out.append(c);
            } else if (c == ' ') {
                out.append('+');
            } else {
                byte[] bytes = SharedStringUtil.getBytes(String.valueOf(c));//.getBytes(Const.UTF8);
                for (byte b : bytes) {
                    out.append('%');
                    out.append(HEX[(b >> 4) & 0xF]);
                    out.append(HEX[b & 0xF]);
                }
            }
        }
        return out.toString();
    }

    private static String percentEncodePath(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '/' || isUnreserved(c)) {
                out.append(c);
            } else {
                byte[] bytes = SharedStringUtil.getBytes(String.valueOf(c));//String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                for (byte b : bytes) {
                    out.append('%');
                    out.append(HEX[(b >> 4) & 0xF]);
                    out.append(HEX[b & 0xF]);
                }
            }
        }
        return out.toString();
    }

    private static boolean isUnreserved(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9') ||
                c == '-' || c == '.' || c == '_' || c == '~';
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

}
