package org.zoxweb.server.http;

import org.zoxweb.shared.util.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class HTTPHeaderParser {
    private HTTPHeaderParser(){}


    // Pattern to parse the header name and value
    public static final Pattern HEADER_PATTERN = Pattern.compile("^([^:]+):\\s*(.*)$");

    // Pattern to parse the main value and parameters
    public static final Pattern VALUE_PARAMETER_PATTERN = Pattern.compile(
            "\\G\\s*([^\\s;,]+)(?:\\s*;\\s*([^=]+)=((?:\"(?:\\\\.|[^\"])*\"|[^;,\\s]*)))?\\s*(?:,|$)");

    // Pattern to parse parameters within the value
    public  static final Pattern PARAMETER_PATTERN = Pattern.compile(
            "\\s*;\\s*([^=]+)=((?:\"(?:\\\\.|[^\"])*\"|[^;,\\s]*))");


    public static NVGenericMap parseHeader(String fullHeaderLine)
    {
        String[] tokens = fullHeaderLine.split(":", 2);
        if (tokens.length != 2)
            throw new IllegalArgumentException("Invalid full line header " + fullHeaderLine);
        return parseHeader(tokens[0], tokens[1]);

    }

    public static NVGenericMap parseHeader(GetNameValue<String> header)
    {
        return parseHeader(header.getName(), header.getValue());
    }

    public static NVGenericMap parseHeader(GetName gn, String headerValues)
    {
        return parseHeader(gn.getName(), headerValues);
    }

    public static NVGenericMap parseHeader(String headerName, String headerValues)
    {
        NVGenericMap ret = new NVGenericMap(headerName);
        List<NVGenericMap> values = parseValue(headerValues);
        // flatten values
        for(NVGenericMap nvgm : values)
        {
            if (SUS.isEmpty(nvgm.getName()) && nvgm.size() == 1)
            {
                ret.add(nvgm.values()[0]);
            }
            else if (SUS.isEmpty(nvgm.getName()))
            {
                nvgm.setName(nvgm.values()[0].getName());
                ret.add(nvgm);
            }
            else
                ret.add(nvgm);
        }
        return ret;

    }


    /**
     * Parses an HTTP header value that may contain comma-separated items.
     * Each item is split into a main value and (optionally) a list of parameters.
     *
     * Example header value:
     *   text/html; q=0.8, application/xhtml+xml; q=0.9, image/webp; q=0.7, **; q=0.5
     *
     *   @param headerValues the raw header value (excluding the header name)
     * @return a List of HeaderValue objects with a main value and parameters.
     */
    protected static List<NVGenericMap> parseValue(String headerValues) {
        List<NVGenericMap> result = new ArrayList<>();
        int len = headerValues.length();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;

        // Loop through the header value and split on top-level commas.
        for (int i = 0; i < len; i++) {
            char c = headerValues.charAt(i);
            if (c == '"') {
                // Toggle inQuote status
                inQuote = !inQuote;
                current.append(c);
            } else if (c == ',' && !inQuote) {
                // Comma outside a quoted string: finish one element.
                String element = current.toString();
                if (!element.trim().isEmpty()) {
                    result.add(parseElement(element.trim()));
                }
                current.setLength(0); // reset builder
            } else {
                current.append(c);
            }
        }
        // Process last element if any.
        if (current.length() > 0) {
            result.add(parseElement(current.toString().trim()));
        }
        return result;
    }


    private static void addParameter(NVGenericMap nvgm, String name, String value)
    {
        NVGenericMap properties = nvgm.getNV("properties");
        if (properties == null)
        {
            properties = new NVGenericMap("properties");
            nvgm.add(properties);
        }
        properties.add(name, value);
    }
    /**
     * Parses a single header element.
     * The element is assumed to be of the form:
     *    main-value; param1=value1; param2="value, with, commas"
     *
     * @param element the header element string to parse.
     * @return a HeaderValue containing the main value and a map of parameters.
     */
    private static NVGenericMap parseElement(String element) {
        int len = element.length();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        List<String> tokens = new ArrayList<>();

        // Loop through the element and split it on semicolons that are not in quotes.
        for (int i = 0; i < len; i++) {
            char c = element.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
                current.append(c);
            } else if (c == ';' && !inQuote) {
                tokens.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            tokens.add(current.toString().trim());
        }

        // The first token is the main value.
        String mainValue = tokens.get(0);
        NVGenericMap nvgm = new NVGenericMap();
        String[] mainValueTokens = mainValue.split("[= ]+");
        if (mainValueTokens.length == 2) {
            nvgm.build(mainValueTokens[0], mainValueTokens[1]);

        }
        else
        {
            nvgm.setName(mainValue);
        }



        // Remaining tokens are parameters.
        for (int i = 1; i < tokens.size(); i++) {
            String token = tokens.get(i);
            // Find the first '=' if any.
            int eqIndex = token.indexOf('=');
            if (eqIndex > 0) {
                String name = token.substring(0, eqIndex).trim();
                String value = token.substring(eqIndex + 1).trim();
                // Remove surrounding quotes, if present.
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                addParameter(nvgm, name, value);
            } else if (!token.isEmpty()) {
                // Parameter is a standalone token (e.g. no-cache in Cache-Control)
                addParameter(nvgm, token, null);
            }
        }
        return nvgm;
    }

    /**
     * Parses a full HTTP header string into its components.
     *
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
    public static NamedValue<String> parseFullLineHeader(String headerLine)
    {
        if (SUS.isEmpty(headerLine))
            return null;

        Map<String, Object> headerComponents = new HashMap<>();

        NamedValue<String> namedValue = new NamedValue<String>();

        Matcher headerMatcher = HEADER_PATTERN.matcher(headerLine);
        if (headerMatcher.matches()) {
            String headerName = headerMatcher.group(1).trim();
            String headerValue = headerMatcher.group(2).trim();

            headerComponents.put("headerName", headerName);
            namedValue.setName(headerName);

            // Check if the header value contains comma-separated values
            if (headerValue.contains(",")) {
                List<Map<String, Object>> values = parseCommaSeparatedValues(headerValue);
                headerComponents.put("values", values);
                for(int i=0; i<values.size(); i++)
                {
                    Map<String, Object> current = values.get(i);
                    if (i==0)
                    {
                        Map<String, String> parameters = (Map<String, String>) current.get("parameters");
                        namedValue.setValue((String) current.get("mainValue"));
                        if(parameters != null)
                        {
                            for (Map.Entry<String,String> nv : parameters.entrySet())
                            {
                                namedValue.getProperties().build(nv.getKey(), nv.getValue());
                            }
                        }
                    }
                    else
                    {
                        NamedValue<String> toAdd = new NamedValue<String>();
                        toAdd.setName("Param-"+i);
                        toAdd.setValue((String) current.get("mainValue"));
                        Map<String, String> parameters = (Map<String, String>) current.get("parameters");
                        if(parameters != null)
                        {
                            for (Map.Entry<String,String> nv : parameters.entrySet())
                            {
                                toAdd.getProperties().build(nv.getKey(), nv.getValue());
                            }
                        }

                        namedValue.getProperties().build(toAdd);
                    }
                }
            } else {
                Map<String, Object> valueParams = parseValueAndParameters(headerValue);
                namedValue.setValue((String)valueParams.get("mainValue"));
                headerComponents.put("mainValue", valueParams.get("mainValue"));
                headerComponents.put("parameters", valueParams.get("parameters"));
                Map<String, String> parameters = (Map<String, String>) valueParams.get("parameters");
                if(parameters != null)
                {
                    for (Map.Entry<String,String> nv : parameters.entrySet())
                    {
                        namedValue.getProperties().build(nv.getKey(), nv.getValue());
                    }
                }


            }
        }

        return namedValue;
    }

    private static List<Map<String, Object>> parseCommaSeparatedValues(String headerValue) {
        List<Map<String, Object>> valuesList = new ArrayList<>();
        String[] values = headerValue.split(",");

        for (String value : values) {
            Map<String, Object> valueParams = parseValueAndParameters(value.trim());
            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put("mainValue", valueParams.get("mainValue"));
            valueMap.put("parameters", valueParams.get("parameters"));
            valuesList.add(valueMap);
        }

        return valuesList;
    }

    private static Map<String, Object> parseValueAndParameters(String value) {
        Map<String, Object> result = new HashMap<>();
        Matcher matcher = Pattern.compile("([^;]+)(.*)").matcher(value);
        if (matcher.matches()) {
            String mainValue = matcher.group(1).trim();
            String paramsPart = matcher.group(2);

            result.put("mainValue", mainValue);

            if (paramsPart != null && !paramsPart.isEmpty()) {
                Map<String, String> params = parseParameters(paramsPart);
                result.put("parameters", params);
            }
        }
        return result;
    }

//    public static NVGenericMap parseHeaderValue(NVGenericMap nvgm, String headerValue)
//    {
//        NVGenericMap ret = nvgm != null ? nvgm : new NVGenericMap();
//        String[] parsed = headerValue.split(",");
//        if (parsed.length != 0)
//        {
//            for (String toParse : parsed)
//            {
//                NVPair nv = SharedUtil.toNVPair(toParse, "=", true);
//
//                if (nv != null)
//                {
//                    ret.build(nv);
//                }
//            }
//        }
//        else
//        {
//            ret = SharedUtil.toNVGenericMap(ret, headerValue, "=", null, true);
//
//        }
//
//        return ret;
//    }

    private static Map<String, String> parseParameters(String paramsPart) {
        Map<String, String> parameters = new HashMap<>();
        Matcher paramMatcher = PARAMETER_PATTERN.matcher(paramsPart);
        while (paramMatcher.find()) {
            String name = paramMatcher.group(1).trim();
            String value = paramMatcher.group(2).trim();
            value = unquote(value);
            parameters.put(name, value);
        }
        return parameters;
    }

    private static String unquote(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            // Remove the surrounding quotes
            value = value.substring(1, value.length() - 1);
            // Unescape any escaped quotes
            value = value.replace("\\\"", "\"");
        }
        return value;
    }
}
