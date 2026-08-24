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
 * A functional interface for encoding/transforming data from one type to another.
 * <p>
 * This interface is the encoding counterpart to {@link DataDecoder}. While decoders
 * typically parse or deserialize data, encoders transform or serialize data into
 * a target format.
 * </p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Using built-in string encoders
 * String lower = DataEncoder.StringLower.encode("HELLO");  // "hello"
 * String upper = DataEncoder.StringUpper.encode("hello");  // "HELLO"
 *
 * // Custom encoder using lambda
 * DataEncoder<Integer, String> intToHex = (i) -> Integer.toHexString(i);
 * String hex = intToHex.encode(255);  // "ff"
 *
 * // JSON encoder example
 * DataEncoder<NVGenericMap, String> jsonEncoder = GSONUtil::toJSONDefault;
 * String json = jsonEncoder.encode(map);
 * }</pre>
 *
 * <h2>Built-in Encoders</h2>
 * <ul>
 *     <li>{@link #StringLower} - Converts strings to lowercase</li>
 *     <li>{@link #StringUpper} - Converts strings to uppercase</li>
 *     <li>{@link #LowerAscii} - ASCII case-folds a byte array ({@code 'A'..'Z'} only, binary-safe)</li>
 * </ul>
 *
 * @param <EI> the input type to encode
 * @param <EO> the output type after encoding
 * @see DataDecoder
 * @see Codec
 */
public interface DataEncoder<EI, EO>
        extends Codec {

    /**
     * Encoder that converts a string to lowercase.
     * <p>
     * Returns null if the input is null.
     * </p>
     */
    DataEncoder<String, String> StringLower = (s) -> s != null ? s.toLowerCase() : null;

    /**
     * Encoder that converts a string to uppercase.
     * <p>
     * Returns null if the input is null.
     * </p>
     */
    DataEncoder<String, String> StringUpper = (s) -> s != null ? s.toUpperCase() : null;


    /**
     * Encoder that ASCII case-folds a byte array: {@code 'A'..'Z'} are lowered, every other
     * byte — digits, symbols, control bytes, values above 0x7F — is untouched, so folding is
     * binary-safe with no locale or UTF-8 case rules. This is the primitive behind
     * case-insensitive protocol-token matching (e.g. the exchange-script {@code validate}
     * {@code ignore_case} option).
     * <p>
     * Returns null for null input and the same (empty) array for empty input; a non-empty
     * input is folded into a fresh copy — the input array is never modified.
     * </p>
     */
    DataEncoder<byte[], byte[]> LowerAscii = (data) -> {
        if (data == null || data.length == 0) return data;
        byte[] ret = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            byte b = data[i];
            ret[i] = (b >= 'A' && b <= 'Z') ? (byte) (b + 32) : b;
        }
        return ret;
    };

    /**
     * Encodes the input and converts it to an output object.
     *
     * @param input the object to encode
     * @return the encoded result of type EO
     */
    EO encode(EI input);
}
