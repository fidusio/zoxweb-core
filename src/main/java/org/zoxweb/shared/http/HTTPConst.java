package org.zoxweb.shared.http;

import org.zoxweb.shared.util.*;

public final class HTTPConst {
    private HTTPConst() {
    }

    public final static String APPLICATION_JSON = "application/json";
    public final static String APPLICATION_PDF = "application/pdf";
    public final static String TEXT_CSV = "text/csv";
    public final static String TEXT_CSS = "text/css";
    public final static String TEXT_JAVASCRIPT = "text/javascript";
    public final static String TEXT_HTML = "text/html";
    public final static String TEXT_PLAIN = "text/plain";
    public final static String TEXT_YAML = "text/yaml";

    public final static String SESSION_ID = "JSESSIONID";

    public static final GetNameValue<String> CHARSET_UTF_8 = new NVPair("charset", Const.UTF_8);


    /**
     * HTTP Common-Name parameters
     */
    public enum CNP
            implements GetName {
        MEDIA_TYPE("media-type"),
        FILENAME("filename"),
        CONTENT_LENGTH("content-length"),
        BOUNDARY("boundary"),

        ;
        private final String name;

        CNP(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String toString() {
            return getName();
        }
    }

    public enum HTTPValue
            implements GetValue<String> {
        CLOSE("close"),
        KEEP_ALIVE("keep-alive"),
        NO_CACHE("no-cache"),
        NO_STORE("no-store"),
        UPGRADE("upgrade");
        private final String value;

        HTTPValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum CommonHeader
            implements GetNameValue<String> {
        CONNECTION_CLOSE(HTTPHeader.CONNECTION, "close"),
        CONNECTION_KEEP_ALIVE(HTTPHeader.CONNECTION, "keep-alive"),
        CONNECTION_UPGRADE(HTTPHeader.CONNECTION, "upgrade"),
        CONTENT_TYPE_JSON_UTF8(toHTTPHeader(HTTPHeader.CONTENT_TYPE, HTTPMediaType.APPLICATION_JSON, HTTPConst.CHARSET_UTF_8)),
        EXPIRES_ZERO(toHTTPHeader(HTTPHeader.EXPIRES, "0")),
        NO_CACHE_CONTROL(HTTPHeader.CACHE_CONTROL, "no-store, no-cache, must-revalidate, private"),
        UPGRADE_WEBSOCKET(HTTPHeader.UPGRADE, "websocket"),
        WWW_AUTHENTICATE(HTTPHeader.WWW_AUTHENTICATE, "Basic realm=\"xlogistx\""),
        STRICT_TRANSPORT_SECURITY(HTTPHeader.STRICT_TRANSPORT_SECURITY, "max-age=31536000; includeSubDomains; preload"),
        X_CONTENT_TYPE_OPTIONS_NO_SNIFF("X-Content-Type-Options", " nosniff"),

        ;
        private final String name;
        private final String value;

        CommonHeader(GetNameValue<String> gnv) {
            this.name = gnv.getName();
            this.value = gnv.getValue();
        }

        CommonHeader(GetName name, String value) {
            this(name.getName(), value);
        }

        CommonHeader(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String toString() {
            return name + ": " + value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }
    }


    public static GetNameValue<String> toHTTPHeader(GetName gn, GetValue<?>... values) {
        return toHTTPHeader(gn.getName(), values);
    }


    public static GetNameValue<String> toHTTPHeader(GetName gn, String... values) {
        return toHTTPHeader(gn.getName(), values);
    }


    public static GetNameValue<String> toHTTPHeader(String name, GetValue<?>... values) {
        StringBuilder headerValue = new StringBuilder();

        if (values != null) {
            for (GetValue<?> value : values) {
                if (headerValue.length() > 0)
                    headerValue.append("; ");

                if (value != null && value.getValue() != null) {

                    if (value instanceof GetNameValue) {
                        headerValue.append(((GetNameValue<?>) value).getName());
                        headerValue.append("=");
                        headerValue.append(value.getValue());
                    } else
                        headerValue.append(value.getValue());
                }

            }
        }

        return new NVPair(name, headerValue.toString());
    }


    public static GetNameValue<String> toHTTPHeader(String name, String... values) {
        StringBuilder headerValue = new StringBuilder();

        if (values != null) {
            for (String value : values) {
                if (headerValue.length() > 0)
                    headerValue.append("; ");

                if (!SUS.isEmpty(value))
                    headerValue.append(value);

            }
        }

        return new NVPair(name, headerValue.toString());
    }

    public static GetNameValue<String> toHTTPHeader(GetName name, GetNameValue<?>... gnvs) {
        return toHTTPHeader(name.getName(), gnvs);
    }


    public static GetNameValue<String> toHTTPHeader(String name, GetNameValue<?>... gnvs) {

        StringBuilder headerValue = new StringBuilder();

        if (gnvs != null) {
            for (GetNameValue<?> gnv : gnvs) {
                if (headerValue.length() > 0)
                    headerValue.append("; ");

                if (gnv != null && !SUS.isEmpty(gnv.getName())) {
                    headerValue.append(gnv.getName());
                    if (gnv.getValue() != null) {
                        headerValue.append('=');
                        headerValue.append(gnv.getValue());
                    }
                }

            }
        }
        return new NVPair(name, headerValue.toString());
    }


    public static String toString(GetNameValue<?> gnv) {
        return gnv.getName() + ": " + gnv.getValue();
    }

    public static byte[] toBytes(GetNameValue<?> gnv) {
        return SharedStringUtil.getBytes(gnv.getName() + ": " + gnv.getValue());
    }


}
