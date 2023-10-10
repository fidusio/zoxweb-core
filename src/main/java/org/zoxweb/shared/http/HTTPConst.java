package org.zoxweb.shared.http;

import org.zoxweb.shared.util.*;

public final class HTTPConst
{
    private HTTPConst(){}

    public enum CommonHeader
            implements GetNameValue<String>
    {
        CONNECTION_CLOSE(HTTPHeader.CONNECTION, "close"),
        CONNECTION_KEEP_ALIVE(HTTPHeader.CONNECTION, "keep-alive"),

        CONNECTION_UPGRADE(HTTPHeader.CONNECTION, "upgrade"),
        UPGRADE_WEBSOCKET(HTTPHeader.UPGRADE, "websocket"),
        ;
        private final String name;
        private final String value;

        CommonHeader(GetName name, String value)
        {
            this(name.getName(), value);
        }
        CommonHeader(String name, String value)
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




    public static GetNameValue<String> toHTTPHeader(GetName gn, GetValue<String>...values)
    {
        return toHTTPHeader(gn.getName(), values);
    }


    public static GetNameValue<String> toHTTPHeader(GetName gn, String ...values)
    {
        return toHTTPHeader(gn.getName(), values);
    }


    public static GetNameValue<String> toHTTPHeader(String name, GetValue<String> ...values)
    {
        StringBuilder headerValue = new StringBuilder();

        if(values != null)
        {
            for(GetValue<String> value: values)
            {
                if (headerValue.length() > 0)
                    headerValue.append("; ");

                if (value != null && !SharedStringUtil.isEmpty(value.getValue()))
                {

                    if (value instanceof GetNameValue)
                    {
                        headerValue.append(((GetNameValue<String>) value).getName());
                        headerValue.append("=");
                        headerValue.append(value.getValue());
                    }
                    else
                        headerValue.append(value.getValue());
                }

            }
        }

        return new NVPair(name, headerValue.toString());
    }


    public static GetNameValue<String> toHTTPHeader(String name, String ...values)
    {
        StringBuilder headerValue = new StringBuilder();

        if(values != null)
        {
            for(int i=0; i<values.length; i++)
            {
                if (headerValue.length() > 0)
                    headerValue.append("; ");

                if (!SharedStringUtil.isEmpty(values[i]))
                    headerValue.append(values[i]);

            }
        }

        return new NVPair(name, headerValue.toString());
    }

    public static GetNameValue<String> toHTTPHeader(GetName name, GetNameValue<?> ...gnvs)
    {

        return toHTTPHeader(name.getName(), gnvs);
    }


    public static GetNameValue<String> toHTTPHeader(String name, GetNameValue<?> ...gnvs)
    {

        StringBuilder headerValue = new StringBuilder();

        if(gnvs != null)
        {
            for(int i=0; i<gnvs.length; i++)
            {
                if (headerValue.length() > 0)
                    headerValue.append("; ");

                if (gnvs[i] !=null && !SharedStringUtil.isEmpty(gnvs[i].getName()))
                {
                    headerValue.append(gnvs[i].getName());
                    if(gnvs[i].getValue() != null)
                    {
                        headerValue.append('=');
                        headerValue.append(gnvs[i].getValue());
                    }
                }

            }
        }
        return new NVPair(name, headerValue.toString());
    }

}
