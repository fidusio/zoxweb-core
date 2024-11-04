package org.zoxweb.server.http;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.io.UByteArrayOutputStream;
import org.zoxweb.shared.util.NamedValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseHTTPRequestTest
{
    public static UByteArrayOutputStream rawRequest;
    @BeforeAll
    public static void loadData() throws IOException
    {
        rawRequest = IOUtil.inputStreamToByteArray(IOUtil.locateFile("multipart-raw-data.txt"), true);
    }

    @Test
    public void parseMultipartRequest()
    {
        System.out.println(rawRequest);

        HTTPRawMessage hrm = new HTTPRawMessage(rawRequest);
        hrm.parse(true);
        System.out.println("is message complete: " + hrm.isMessageComplete());
        System.out.println(hrm.getHTTPMessageConfig().getBoundary());
        System.out.println(hrm.getHTTPMessageConfig().getHeaders());


    }

    // Pattern to parse the header name and value
    private static final Pattern HEADER_PATTERN = Pattern.compile("^([^:]+):\\s*(.*)$");

    // Pattern to parse the main value and parameters
    private static final Pattern VALUE_PARAMETER_PATTERN = Pattern.compile(
            "\\G\\s*([^\\s;,]+)(?:\\s*;\\s*([^=]+)=((?:\"(?:\\\\.|[^\"])*\"|[^;,\\s]*)))?\\s*(?:,|$)");

    // Pattern to parse parameters within the value
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(
            "\\s*;\\s*([^=]+)=((?:\"(?:\\\\.|[^\"])*\"|[^;,\\s]*))");

    /**
     * Parses a full HTTP header string into its components.
     *
     * @param headerLine the HTTP header line to parse
     * @return a map containing the header name, main value, and parameters
     */
    public static Map<String, Object> parseHeader(String headerLine) {
        Map<String, Object> headerComponents = new HashMap<>();

        Matcher headerMatcher = HEADER_PATTERN.matcher(headerLine);
        if (headerMatcher.matches()) {
            String headerName = headerMatcher.group(1).trim();
            String headerValue = headerMatcher.group(2).trim();

            headerComponents.put("headerName", headerName);

            // Check if the header value contains comma-separated values
            if (headerValue.contains(",")) {
                List<Map<String, Object>> values = parseCommaSeparatedValues(headerValue);
                headerComponents.put("values", values);
            } else {
                Map<String, String> valueParams = parseValueAndParameters(headerValue);
                headerComponents.put("mainValue", valueParams.get("mainValue"));
                headerComponents.put("parameters", valueParams.get("parameters"));
            }
        }

        return headerComponents;
    }

    private static List<Map<String, Object>> parseCommaSeparatedValues(String headerValue) {
        List<Map<String, Object>> valuesList = new ArrayList<>();
        String[] values = headerValue.split(",");

        for (String value : values) {
            Map<String, String> valueParams = parseValueAndParameters(value.trim());
            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put("mainValue", valueParams.get("mainValue"));
            valueMap.put("parameters", valueParams.get("parameters"));
            valuesList.add(valueMap);
        }

        return valuesList;
    }

    private static Map<String, String> parseValueAndParameters(String value) {
        Map<String, String> result = new HashMap<>();
        Matcher matcher = Pattern.compile("([^;]+)(.*)").matcher(value);
        if (matcher.matches()) {
            String mainValue = matcher.group(1).trim();
            String paramsPart = matcher.group(2);

            result.put("mainValue", mainValue);

            if (paramsPart != null && !paramsPart.isEmpty()) {
                Map<String, String> params = parseParameters(paramsPart);
                result.put("parameters", params.toString());
            } else {
                result.put("parameters", "{}");
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



    @Test
    public void parseHeaderTest()
    {
        String[] headers = {"Content-Type: text/html;charset=UTF-8; boundary=\"--exampleBoundary\", batata=232",
                "Content-Disposition: form-data; name=\"file\"; filename=\"example.pdf\"",
                "Accept: text/html, application/xhtml+xml;q=0.9, image/webp;q=0.8, */*;q=0.7;q=0.9",
                "Content-Type: multipart/form-data; boundary=bd1a40c9-9408-4b59-8d4b-f6693561887e",
                "Authorization: Bearer jdlksjfgdksljgikjrtjtkrejtiohyu4o35hjhj5rk;charset=UTF-8"
            };

        for (String header: headers)
        {
            System.out.println(header);
            NamedValue<String> parsedHeader = HTTPHeaderParser.parseHeader(header);
            System.out.println(parsedHeader);
            System.out.println("-----------------------------------------------------------");

        }

    }
}
