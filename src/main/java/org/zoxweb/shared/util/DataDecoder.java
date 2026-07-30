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
}