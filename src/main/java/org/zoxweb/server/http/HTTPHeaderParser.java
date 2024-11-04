package org.zoxweb.server.http;

import org.zoxweb.shared.util.NamedValue;

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
    public static NamedValue<String> parseHeader(String headerLine) {
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
