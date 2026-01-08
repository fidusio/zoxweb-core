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
package org.zoxweb.server.http;

import org.zoxweb.shared.http.HTTPAuthorization;
import org.zoxweb.shared.http.HTTPHeader;
import org.zoxweb.shared.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing HTTP header lines into structured {@link NamedValue} objects.
 * <p>
 * This parser handles various HTTP header formats and extracts both primary values and
 * sub-parameters. It provides specialized parsing for common headers like {@code Content-Length},
 * {@code Cookie}, {@code Keep-Alive}, and {@code Authorization}, while also supporting
 * generic header parsing with comma-separated values and semicolon-separated parameters.
 * </p>
 *
 * <h3>Header Format Notation</h3>
 * <pre>
 * Header-Name: value1; param1=p1value, value2; param2="quoted value", value3
 * |-- name --|  |-V0-|  |-- PNV0-1 --|  |-V1-|  |---- PNV1-1 -----|  |-V2-|
 *
 * Where:
 *   V   = Primary value segment
 *   PNV = Parameter name-value pair for a value segment
 * </pre>
 *
 * <h3>Supported Header Types</h3>
 * <ul>
 *     <li><b>Content-Length:</b> Parsed as a {@code Long} value</li>
 *     <li><b>Cookie:</b> Parsed into individual cookie name-value pairs (semicolon-separated)</li>
 *     <li><b>Keep-Alive:</b> Parsed into parameters like {@code timeout=5, max=100}</li>
 *     <li><b>Authorization:</b> Parsed using {@link HTTPAuthorization} with scheme and token extraction</li>
 *     <li><b>Generic:</b> Comma-separated values with optional semicolon-separated parameters</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Parse a Content-Type header
 * NamedValue<?> result = HTTPHeaderParser.parseHeader("Content-Type", "text/html; charset=utf-8");
 * // result.getName() = "text/html"
 * // result.getProperties().getValue("charset") = "utf-8"
 *
 * // Parse a Cookie header
 * NamedValue<?> cookies = HTTPHeaderParser.parseHeader("Cookie", "session=abc123; user=john");
 * // cookies.getProperties().getValue("session") = "abc123"
 * // cookies.getProperties().getValue("user") = "john"
 *
 * // Parse a full header line
 * NamedValue<?> header = HTTPHeaderParser.parseFullHeaderLine("Accept: text/html, application/json");
 * }</pre>
 *
 * @see NamedValue
 * @see HTTPAuthorization
 * @see HTTPHeader
 */
public class HTTPHeaderParser {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private HTTPHeaderParser() {
    }

    /**
     * Pattern for splitting header values by commas while respecting quoted strings.
     * Matches segments that may contain quoted strings or any non-comma characters.
     */
    private static final Pattern COMMA_PATTERN = Pattern.compile("((?:\\\"[^\\\"]*\\\"|[^,])+)(?:,\\\\s*)?");

    /**
     * Pattern for parsing semicolon-separated parameters in the format {@code name=value}.
     * Handles both quoted and unquoted values.
     */
    private static final Pattern PARAM_PATTERN = Pattern.compile("([\\w-]+)=((?:\\\"[^\\\"]*\\\")|[^;]+)");

    /**
     * Pattern for parsing comma-separated authorization parameters.
     * Used for headers like {@code WWW-Authenticate} with multiple parameters.
     */
    private static final Pattern AUTH_PARAM_PATTERN = Pattern.compile("([\\w-]+)=((?:\\\"[^\\\"]*\\\")|[^,]+)(?:,\\\\s*)?");

    /**
     * Pattern for parsing cookie name-value pairs.
     * Matches {@code name=value} format used in Cookie headers.
     */
    private static final Pattern COOKIE_PATTERN = Pattern.compile("([^=;\\\\s]+)=([^;]+)");


    /**
     * Separates an HTTP header line into its name and value components.
     * <p>
     * Parses a header line in the format {@code "Header-Name: header value"} and returns
     * a two-element array where index 0 contains the trimmed header name and index 1
     * contains the trimmed header value.
     * </p>
     *
     * @param headerLine the complete HTTP header line to parse (e.g., "Content-Type: text/html")
     * @return a String array of length 2: [0] = header name, [1] = header value
     * @throws IllegalArgumentException if the header line doesn't contain a colon separator
     *                                  or if the header value is empty
     */
    public static String[] separateHeaderNameFromValue(String headerLine) {
        int index = headerLine.indexOf(":");
        if (index == -1)
            throw new IllegalArgumentException("Invalid header line: " + headerLine);
        String[] ret = new String[2];
        ret[0] = headerLine.substring(0, index).trim();
        ret[1] = headerLine.substring(index + 1).trim();
        if (SUS.isEmpty(ret[1]))
            throw new IllegalArgumentException("Invalid header value empty: " + headerLine);
        return ret;
    }


    /**
     * Parses an HTTP header from a name-value pair object.
     * <p>
     * Convenience method that extracts the name and value from a {@link GetNameValue}
     * and delegates to {@link #parseHeader(String, String)}.
     * </p>
     *
     * @param httpHeaderNV the name-value pair containing the header name and value
     * @return a {@link NamedValue} containing the parsed header structure
     * @see #parseHeader(String, String)
     */
    public static NamedValue<?> parseHeader(GetNameValue<String> httpHeaderNV) {
        return parseHeader(httpHeaderNV.getName(), httpHeaderNV.getValue());
    }

    /**
     * Parses an HTTP header from a named object and value string.
     * <p>
     * Convenience method that extracts the name from a {@link GetName} instance
     * and delegates to {@link #parseHeader(String, String)}.
     * </p>
     *
     * @param headerName the object providing the header name
     * @param headerValue the header value string to parse
     * @return a {@link NamedValue} containing the parsed header structure
     * @see #parseHeader(String, String)
     */
    public static NamedValue<?> parseHeader(GetName headerName, String headerValue) {
        return parseHeader(headerName.getName(), headerValue);
    }

    /**
     * Parses an HTTP header value into a structured {@link NamedValue} object.
     * <p>
     * This method provides specialized parsing for common HTTP headers:
     * </p>
     * <ul>
     *     <li><b>Content-Length:</b> Returns {@code NamedValue<Long>} with the numeric value</li>
     *     <li><b>Cookie:</b> Parses semicolon-separated {@code name=value} pairs into properties</li>
     *     <li><b>Keep-Alive:</b> Parses comma-separated parameters like {@code timeout=5, max=100}</li>
     *     <li><b>Authorization:</b> Extracts auth scheme, token, and any additional parameters</li>
     *     <li><b>Other headers:</b> Parses comma-separated values with optional semicolon parameters</li>
     * </ul>
     *
     * <h4>Return Structure</h4>
     * <p>
     * For single-value headers, returns a {@link NamedValue} where:
     * <ul>
     *     <li>{@code getName()} returns the primary value or header name</li>
     *     <li>{@code getValue()} returns the raw value</li>
     *     <li>{@code getProperties()} contains any parsed parameters</li>
     * </ul>
     * For multi-value headers (comma-separated), returns a {@link NamedValue} containing
     * an {@link NVGenericMap} with all parsed value segments.
     * </p>
     *
     * @param headerName the HTTP header name (e.g., "Content-Type", "Cookie")
     * @param headerValue the header value string to parse
     * @return a {@link NamedValue} containing the parsed header structure, or null if parsing fails
     */
    public static NamedValue<?> parseHeader(String headerName, String headerValue) {
        headerName = headerName.trim();
        headerValue = headerValue.trim();
        NamedValue<?> ret = null;


        if (headerName.equalsIgnoreCase(HTTPHeader.CONTENT_LENGTH.getName())) {
            ret = new NamedValue<>(headerName, Long.parseLong(headerValue));
        } else if (headerName.equalsIgnoreCase("Cookie")) {

            ret = new NamedValue<>(headerName, headerValue);
            // Cookie: name1=value1; name2=value2
            for (String cookiePair : headerValue.split(";\\s*")) {
                if (cookiePair.isEmpty()) continue;
                String[] nv = cookiePair.split("=", 2);
                ret.getProperties().build(nv[0].trim(), nv.length > 1 ? nv[1] : "");
            }

        } else if (headerName.equalsIgnoreCase("Keep-Alive")) {

            ret = new NamedValue<>(headerName, headerValue);
            // Cookie: name1=value1; name2=value2
            for (String cookiePair : headerValue.split(",\\s*")) {
                if (cookiePair.isEmpty()) continue;
                String[] nv = cookiePair.split("=", 2);
                ret.getProperties().build(nv[0].trim(), nv.length > 1 ? nv[1] : "");
            }

        } else if (headerName.equalsIgnoreCase("Authorization")) {
            HTTPAuthorization httpAuthorization = new HTTPAuthorization(headerValue);
            ret = new NamedValue<>(headerName, headerValue);
            NamedValue<String> internalToken = httpAuthorization.lookup(HTTPAuthorization.NVC_TOKEN);
            if (internalToken.getProperties().size() > 0) {
                NVGenericMap.copy(internalToken.getProperties(), ret.getProperties(), false);

            }

            ret.getProperties().build("auth_scheme", httpAuthorization.getName());
            ret.getProperties().build("auth_token", httpAuthorization.getToken());
        } else {
            // Generic header: split values by commas, params by semicolons
            Matcher cm = COMMA_PATTERN.matcher(headerValue);
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
                ret = new NamedValue(headerName, values);
            else if (values.size() == 1) {
                NamedValue<?>[] toRet = values.valuesAs(new NamedValue[0]);
                toRet[0].setName(headerName);
                ret = toRet[0];

            }
        }


        return ret;
    }

    /**
     * Parses a complete HTTP header line into its structured components.
     * <p>
     * This is a convenience method that combines {@link #separateHeaderNameFromValue(String)}
     * and {@link #parseHeader(String, String)} to parse a full header line in one call.
     * </p>
     *
     * <h4>Header Line Format</h4>
     * <pre>
     * Header-Name: firstValue; param1=value1, secondValue; param2="quoted", thirdValue
     * |--name--|   |--- V0 ---|  |-- PNV0 --|  |--- V1 ---|  |--- PNV1 ---|  |-- V2 --|
     *
     * Legend:
     *   V   = Value segment (comma-separated)
     *   PNV = Parameter name-value pair (semicolon-separated within a value segment)
     * </pre>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * NamedValue<?> result = HTTPHeaderParser.parseFullHeaderLine(
     *     "Content-Type: text/html; charset=utf-8"
     * );
     * // result.getName() = "text/html"
     * // result.getProperties().getValue("charset") = "utf-8"
     * }</pre>
     *
     * @param fullHeaderLine the complete HTTP header line including name and value
     *                       (e.g., "Content-Type: application/json")
     * @return a {@link NamedValue} containing the parsed header structure
     * @throws IllegalArgumentException if the header line is malformed (missing colon or empty value)
     * @see #separateHeaderNameFromValue(String)
     * @see #parseHeader(String, String)
     */
    public static NamedValue<?> parseFullHeaderLine(String fullHeaderLine) {
        String[] parts = separateHeaderNameFromValue(fullHeaderLine);
        return parseHeader(parts[0], parts[1]);
    }
}
