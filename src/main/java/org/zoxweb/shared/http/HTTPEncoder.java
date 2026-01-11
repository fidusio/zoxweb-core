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

import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.SUS;

/**
 * Enumeration of HTTP parameter encoding formats.
 * Provides different strategies for encoding name-value pairs into HTTP-compatible strings.
 * <ul>
 *   <li>{@link #URL_ENCODED}: Standard URL query string format (name=value&amp;name2=value2)</li>
 *   <li>{@link #URI_REST_ENCODED}: REST-style path segments (value1/value2)</li>
 *   <li>{@link #HEADER}: HTTP header parameter format (name=value; name2=value2)</li>
 * </ul>
 *
 * @author mnael
 */
public enum HTTPEncoder
        implements GetNameValue<String>, ValueFilter<GetNameValue<String>, String> {

    /** URL query string encoding format: name=value&amp;name2=value2 */
    URL_ENCODED("=", "&") {
        @Override
        public String validate(GetNameValue<String> nvp) throws NullPointerException, IllegalArgumentException {
            if (!isValid(nvp)) {
                throw new IllegalArgumentException("Invalid NVP:" + nvp);
            }

            return nvp.getName() +
                    getNameValueSep() +
                    (Object) nvp.getValue();// MN 2025-02-222 DO NOT REMOVE the Object casting It is on purpose to support int and long and other type NOT String
        }

        @Override
        public boolean isValid(GetNameValue<String> nvp) {
            return nvp != null && nvp.getValue() != null && nvp.getName() != null;
        }

        @Override
        public String toCanonicalID() {
            return null;
        }
    },
    /** REST URI path segment encoding format: value1/value2 */
    URI_REST_ENCODED(null, "/") {
        @Override
        public String validate(GetNameValue<String> nvp) throws NullPointerException, IllegalArgumentException {
            if (!isValid(nvp)) {
                throw new IllegalArgumentException("Invalid NVP:" + nvp);
            }

            return nvp.getValue();
        }

        @Override
        public boolean isValid(GetNameValue<String> nvp) {
            return nvp != null && nvp.getValue() != null;
        }

        @Override
        public String toCanonicalID() {
            return null;
        }
    },
    /** HTTP header parameter encoding format: name=value; name2=value2 */
    HEADER("=", "; ") {
        @Override
        public String validate(GetNameValue<String> nvp) throws NullPointerException, IllegalArgumentException {
            if (!isValid(nvp)) {
                throw new IllegalArgumentException("Invalid NVP:" + nvp);
            }

            return nvp.getName() +
                    getNameValueSep() +
                    (Object) nvp.getValue();// MN 2025-02-222 DO NOT REMOVE the Object casting It is on purpose to support int and long and other type NOT String
        }

        @Override
        public boolean isValid(GetNameValue<String> nvp) {
            return nvp != null && nvp.getValue() != null;
        }

        @Override
        public String toCanonicalID() {
            return null;
        }
    };

    private final String paramSep;
    private final String nvSep;

    HTTPEncoder(String nvSep, String sep) {
        paramSep = sep;
        this.nvSep = nvSep;
    }


    @Override
    public String getName() {
        return name().toLowerCase();
    }

    /**
     * Returns the parameter separator string.
     *
     * @return the separator used between parameters
     */
    @Override
    public String getValue() {
        return paramSep;
    }

    /**
     * Returns the name-value separator string.
     *
     * @return the separator used between name and value
     */
    public String getNameValueSep() {
        return nvSep;
    }


    private StringBuilder format_int(StringBuilder sb, GetNameValue<String> nvp) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        if (isValid(nvp)) {
            if (sb.length() > 0) {
                int index = sb.length() - paramSep.length();
                if (index > 0) {
                    boolean appendSep = false;
                    for (int i = 0; i < paramSep.length(); i++) {
                        if (sb.charAt(index + i) != paramSep.charAt(i)) {
                            appendSep = true;
                            break;
                        }
                    }
                    if (appendSep) {
                        sb.append(paramSep);
                    }
                }

            }
            sb.append(validate(nvp));
        }
        return sb;
    }

    private StringBuilder format_int(StringBuilder sb, String str) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        if (!SUS.isEmpty(str)) {
            if (sb.length() > 0) {
                int index = sb.length() - paramSep.length();
                if (index > 0) {
                    boolean appendSep = false;
                    for (int i = 0; i < paramSep.length(); i++) {
                        if (sb.charAt(index + i) != paramSep.charAt(i)) {
                            appendSep = true;
                            break;
                        }
                    }
                    if (appendSep) {
                        sb.append(paramSep);
                    }
                }
            }
            sb.append(str);
        }
        return sb;
    }

    /**
     * Formats an array of name-value pairs using this encoder's format.
     *
     * @param nvps the name-value pairs to format
     * @return the formatted string
     */
    @SuppressWarnings("unchecked")
    public String format(GetNameValue<String>... nvps) {
        return format(null, nvps);
    }

    /**
     * Formats an array of name-value pairs using this encoder's format,
     * appending to the provided StringBuilder.
     *
     * @param sb   the StringBuilder to append to (created if null)
     * @param nvps the name-value pairs to format
     * @return the formatted parameters as string
     */
    @SuppressWarnings("unchecked")
    public String format(StringBuilder sb, GetNameValue<String>... nvps) {
        if (sb == null) {
            sb = new StringBuilder();
        }
        for (GetNameValue<String> nvp : nvps) {
            sb = format_int(sb, nvp);
        }

        return sb.toString();
    }

    /**
     * Formats an array of string parameters using this encoder's format.
     *
     * @param params the string parameters to format
     * @return the formatted string
     */
    public String format(String... params) {
        return format(null, params);
    }

    /**
     * Formats an array of string parameters using this encoder's format,
     * appending to the provided StringBuilder.
     *
     * @param sb     the StringBuilder to append to (created if null)
     * @param params the string parameters to format
     * @return the formatted parameters as string
     */
    public String format(StringBuilder sb, String... params) {
        if (sb == null) {
            sb = new StringBuilder();
        }
        for (String str : params) {
            sb = format_int(sb, str);
        }

        return sb.toString();
    }


}