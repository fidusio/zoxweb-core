package org.zoxweb.shared.http;

import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVPair;

public final class HTTPConst
{
    private HTTPConst(){}

    public enum Headers
            implements GetNameValue<String>
    {
        CONNECTION_CLOSE(HTTPHeader.CONNECTION, "close"),
        CONNECTION_KEEP_ALIVE(HTTPHeader.CONNECTION, "keep-alive"),
        ;
        private final String name;
        private final String value;

        Headers(GetName name, String value)
        {
            this(name.getName(), value);
        }
        Headers(String name, String value)
        {
            this.name = name;
            this.value = value;
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

    public static final GetNameValue<String> CHARSET_UTF_8 = new NVPair("charset", "utf-8");



}
