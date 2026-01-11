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
 * Marker interface for encoding and decoding operations.
 * <p>
 * This interface serves as the base type for all codec-related interfaces,
 * enabling type-safe handling of encoders and decoders through a common hierarchy.
 * </p>
 *
 * <h2>Related Interfaces</h2>
 * <ul>
 *     <li>{@link DataEncoder} - For encoding/transforming data from one type to another</li>
 *     <li>{@link DataDecoder} - For decoding/parsing data from one type to another</li>
 * </ul>
 *
 * @see DataEncoder
 * @see DataDecoder
 */
public interface Codec {
}
