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
     * Encodes the input and converts it to an output object.
     *
     * @param input the object to encode
     * @return the encoded result of type EO
     */
    EO encode(EI input);
}
