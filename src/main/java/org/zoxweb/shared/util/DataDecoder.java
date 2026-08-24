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
package org.zoxweb.shared.util;

/**
 * A functional interface for decoding/parsing data from one type to another.
 * <p>
 * This interface is the decoding counterpart to {@link DataEncoder}. While encoders
 * typically transform or serialize data into a target format, decoders parse or
 * convert raw input into a usable type.
 * </p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Using the built-in string decoder
 * String s = DataDecoder.AsStringOrNull.decode(someObject);  // null if not a String
 *
 * // Custom decoder using lambda
 * DataDecoder<String, Integer> hexToInt = (s) -> Integer.parseInt(s, 16);
 * int value = hexToInt.decode("ff");  // 255
 *
 * // Applied to a container value via NVGenericMap
 * String url = nvgm.decodeValue("url", DataDecoder.AsStringOrNull);
 * }</pre>
 *
 * <h2>Built-in Decoders</h2>
 * <ul>
 *     <li>{@link #AsStringOrNull} - Returns the input as a String if it is one, null otherwise</li>
 *     <li>{@link #StringToData} - Decodes a {@code txt:}/{@code hex:}/{@code base64:}-prefixed literal to bytes</li>
 * </ul>
 *
 * @param <DI> the input type to decode
 * @param <DO> the output type after decoding
 * @see DataEncoder
 * @see Codec
 */
public interface DataDecoder<DI, DO>
        extends Codec {
    /**
     * Decodes the input and converts it to an output object.
     *
     * @param input the object to decode
     * @return the decoded result of type DO
     */
    DO decode(DI input);

    /**
     * Decoder that returns the input as a String if it is a String instance.
     * <p>
     * Returns null for null input or any non-String object (no toString conversion
     * is performed).
     * </p>
     */
    DataDecoder<Object, String> AsStringOrNull = (o)-> o instanceof String ? (String)o : null;


    /**
     * Decoder for prefix-encoded data literals: the input splits at the <b>first</b> colon and
     * the prefix (matched case-insensitively) selects the encoding — {@code txt:} UTF-8
     * verbatim, {@code hex:} hex digits with whitespace ignored, {@code base64:} a Base64 body.
     * A string with no recognized prefix is taken in full — colon included — as UTF-8 text
     * ({@code "USER: bob"} is text, not a directive).
     * <p>
     * A null or empty input yields an empty array, never null. Throws
     * {@link IllegalArgumentException} if a declared hex/base64 body fails to decode.
     * </p>
     */
    DataDecoder<String, byte[]> StringToData = (encoded) -> {
        if (encoded == null || encoded.isEmpty())
            return Const.EMPTY_BYTE_ARRAY;
        int c = encoded.indexOf(':');
        String prefix = c > 0 ? encoded.substring(0, c).toLowerCase() : "";
        String body = c > 0 ? encoded.substring(c + 1) : encoded;
        try {
            switch (prefix) {
                case "hex":
                    return SharedStringUtil.hexToBytes(body);
                case "base64":
                    return SharedBase64.decode(body);
                case "txt":
                    return SharedStringUtil.getBytes(body);
                default:
                    // no recognized prefix: whole string is UTF-8 text
                    return SharedStringUtil.getBytes(encoded);
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("cannot decode " + prefix + ": data: " + e.getMessage(), e);
        }
    };
}