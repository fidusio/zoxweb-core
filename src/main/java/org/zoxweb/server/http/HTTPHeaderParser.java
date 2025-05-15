package org.zoxweb.server.http;

import org.zoxweb.shared.http.HTTPAuthorization;
import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NamedValue;
import org.zoxweb.shared.util.SUS;

import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class HTTPHeaderParser {
    private HTTPHeaderParser(){}



    // Patterns for splitting and parameter parsing
    private static final Pattern COMMA_PATTERN = Pattern.compile("((?:\\\"[^\\\"]*\\\"|[^,])+)(?:,\\\\s*)?");
    private static final Pattern PARAM_PATTERN = Pattern.compile("([\\w-]+)=((?:\\\"[^\\\"]*\\\")|[^;]+)");
    private static final Pattern AUTH_PARAM_PATTERN = Pattern.compile("([\\w-]+)=((?:\\\"[^\\\"]*\\\")|[^,]+)(?:,\\\\s*)?");
    private static final Pattern COOKIE_PATTERN = Pattern.compile("([^=;\\\\s]+)=([^;]+)");



     public static String[] separateHeaderNameFromValue(String headerLine)
     {
         int index = headerLine.indexOf(":");
         if(index == -1)
             throw new IllegalArgumentException("Invalid header line: " + headerLine);
         String[] ret = new String[2];
         ret[0] = headerLine.substring(0, index).trim();
         ret[1] = headerLine.substring(index+1).trim();
         if(SUS.isEmpty(ret[1]))
             throw new IllegalArgumentException("Invalid header value empty: " + headerLine);
         return ret;
     }


    public static NamedValue<?> parseHTTPHeader(GetName gn, String rawValue)
    {
        return parseHTTPHeader(gn.getName(), rawValue);
    }

    public static NamedValue<?> parseHTTPHeader(String httpHeaderName, String rawValue)
    {
        httpHeaderName = httpHeaderName.trim();
        rawValue = rawValue.trim();
        NamedValue<?> ret = null;
        if (httpHeaderName.equalsIgnoreCase("Cookie"))

        {

            ret = new NamedValue<>(httpHeaderName, rawValue);
            // Cookie: name1=value1; name2=value2
            for (String cookiePair : rawValue.split(";\\s*")) {
                if (cookiePair.isEmpty()) continue;
                String[] nv = cookiePair.split("=", 2);
                ret.getProperties().build(nv[0].trim(), nv.length > 1 ? nv[1] : "");
            }

        }
        if (httpHeaderName.equalsIgnoreCase("Keep-Alive"))
        {

            ret = new NamedValue<>(httpHeaderName, rawValue);
            // Cookie: name1=value1; name2=value2
            for (String cookiePair : rawValue.split(",\\s*")) {
                if (cookiePair.isEmpty()) continue;
                String[] nv = cookiePair.split("=", 2);
                ret.getProperties().build(nv[0].trim(), nv.length > 1 ? nv[1] : "");
            }

        }
        else if (httpHeaderName.equalsIgnoreCase("Authorization"))
        {
            HTTPAuthorization httpAuthorization = new HTTPAuthorization(rawValue);
            ret = new NamedValue<>(httpHeaderName, rawValue);
            NamedValue<String> internalToken= httpAuthorization.lookup(HTTPAuthorization.NVC_TOKEN);
            if(internalToken.getProperties().size() > 0) {
                NVGenericMap.copy(internalToken.getProperties(), ret.getProperties(), false);

            }

            ret.getProperties().build("auth_scheme", httpAuthorization.getName());
            ret.getProperties().build("auth_token", httpAuthorization.getToken());
        }
        else
        {
            // Generic header: split values by commas, params by semicolons
            Matcher cm = COMMA_PATTERN.matcher(rawValue);
            NVGenericMap values = new NVGenericMap("header_values");
            //List<NamedValue<?>> list = new ArrayList<>();
            while (cm.find()) {
                String segment = cm.group(1).trim();
                NamedValue<String> hv;
                int idx = segment.indexOf(';');
                String mainValue = idx >= 0 ? segment.substring(0, idx).trim() : segment;
                hv = new NamedValue<>(mainValue, mainValue);

                Matcher pm = PARAM_PATTERN.matcher(segment);
                while (pm.find()) {
                    String key = pm.group(1);
                    String val = pm.group(2);
                    if (val.startsWith("\"") && val.endsWith("\"")) {
                        val = val.substring(1, val.length() - 1);
                    }
                    hv.getProperties().build(key, val);
                }
                values.add(hv);
            }


            if (values.size() > 1)
                ret = new NamedValue(httpHeaderName, values);
            else if (values.size() == 1)
            {
                NamedValue<?>[] toRet = values.valuesAs(new NamedValue[0]);
                toRet[0].setName(httpHeaderName);
                ret = toRet[0];

            }
        }


        return ret;
    }

    /**
     * Parses a full HTTP header string into its components.
     * I: ignore
     * V: value
     * PNV: sub parameter for a V (VALUE)
     * headerName : firstValue(As value); name1=value1 , secondValue; nameQuoted="quotedValue", valueLessName
     * |--name--|   |------ V0 --------|  |- PNV0-1 -|   |--- V1 --|  |--name--|II|-value---|   |--- V2 ----|
     *              |--------------------------- rawValue 0-------------------------------------------------|
     * extract the header name first
     * @param headerLine the HTTP header line to parse
     * @return a map containing the header name, main value, and parameters
     */
    public static NamedValue<?> parseHTTPHeaderLine(String headerLine)
    {
        String[] parts = separateHeaderNameFromValue(headerLine);
        return parseHTTPHeader(parts[0], parts[1]);
    }
}
