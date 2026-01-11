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
package org.zoxweb.server.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An interface that defines output stream write operations.
 * <p>
 * This interface mirrors the core write methods of {@link OutputStream} but as an interface
 * rather than an abstract class. This allows for more flexible implementations and
 * enables classes to implement output stream behavior without extending {@link OutputStream}.
 * </p>
 * <p>
 * Extends {@link AutoCloseable} to support try-with-resources statements.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try (OutputStreamInt output = getOutputStream()) {
 *     byte[] data = "Hello, World!".getBytes();
 *     output.write(data);
 * }
 * }</pre>
 *
 * @see OutputStream
 * @see InputStreamInt
 * @see AutoCloseable
 */
public interface OutputStreamInt extends AutoCloseable {

    /**
     * Writes a single byte to the output stream.
     * <p>
     * Only the eight low-order bits of the argument are written.
     * The 24 high-order bits are ignored.
     * </p>
     *
     * @param b the byte to write
     * @throws IOException if an I/O error occurs
     */
    void write(int b) throws IOException;

    /**
     * Writes a portion of a byte array to the output stream.
     *
     * @param b the data to write
     * @param off the start offset in the data
     * @param len the number of bytes to write
     * @throws IOException if an I/O error occurs
     * @throws IndexOutOfBoundsException if off or len are invalid
     */
    void write(byte[] b, int off, int len) throws IOException;

    /**
     * Writes a byte array to the output stream.
     *
     * @param b the data to write
     * @throws IOException if an I/O error occurs
     */
    void write(byte[] b) throws IOException;
}
