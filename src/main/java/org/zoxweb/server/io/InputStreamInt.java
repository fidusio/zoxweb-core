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
import java.io.InputStream;

/**
 * An interface that defines input stream read operations.
 * <p>
 * This interface mirrors the core read methods of {@link InputStream} but as an interface
 * rather than an abstract class. This allows for more flexible implementations and
 * enables classes to implement input stream behavior without extending {@link InputStream}.
 * </p>
 * <p>
 * Extends {@link AutoCloseable} to support try-with-resources statements.
 * </p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * try (InputStreamInt input = getInputStream()) {
 *     byte[] buffer = new byte[1024];
 *     int bytesRead;
 *     while ((bytesRead = input.read(buffer)) != -1) {
 *         // process buffer
 *     }
 * }
 * }</pre>
 *
 * @see InputStream
 * @see OutputStreamInt
 * @see AutoCloseable
 */
public interface InputStreamInt extends AutoCloseable {

    /**
     * Reads a single byte from the input stream.
     *
     * @return the byte read as an int (0-255), or -1 if end of stream is reached
     * @throws IOException if an I/O error occurs
     */
    int read() throws IOException;

    /**
     * Reads bytes into a portion of a byte array.
     *
     * @param b the buffer into which the data is read
     * @param off the start offset in array {@code b} at which the data is written
     * @param len the maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or -1 if end of stream
     * @throws IOException if an I/O error occurs
     * @throws IndexOutOfBoundsException if off or len are invalid
     */
    int read(byte[] b, int off, int len) throws IOException;

    /**
     * Reads bytes into a byte array.
     *
     * @param b the buffer into which the data is read
     * @return the total number of bytes read into the buffer, or -1 if end of stream
     * @throws IOException if an I/O error occurs
     */
    int read(byte[] b) throws IOException;
}
