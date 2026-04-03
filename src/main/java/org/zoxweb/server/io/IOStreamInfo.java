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

import org.zoxweb.shared.io.CloseableTypeDelegate;
import org.zoxweb.shared.io.SharedIOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Encapsulates an {@link InputStream}, {@link OutputStream}, and an optional {@link Socket}
 * into a single {@link AutoCloseable} resource. Closing this object releases all underlying
 * I/O resources in one operation.
 */
public class IOStreamInfo
        implements AutoCloseable {

    private volatile InputStream is;
    private volatile OutputStream os;
    private volatile Socket s;
    private final CloseableTypeDelegate cdt;

    /**
     * Creates an IOStreamInfo from explicit input and output streams.
     *
     * @param is the input stream
     * @param os the output stream
     */
    public IOStreamInfo(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
        cdt = new CloseableTypeDelegate(()->SharedIOUtil.close(is,  os, s), false);
    }

    /**
     * Creates an IOStreamInfo from a socket, extracting its input and output streams.
     *
     * @param socket the socket to wrap
     * @throws IOException if an I/O error occurs when obtaining the streams
     */
    public IOStreamInfo(Socket socket)
            throws IOException {
        this(socket.getInputStream(), socket.getOutputStream());
        s = socket;
    }

    /**
     * Creates an IOStreamInfo from a socket with a specified read timeout.
     *
     * @param socket  the socket to wrap
     * @param timeout the read timeout in milliseconds ({@link Socket#setSoTimeout(int)})
     * @throws IOException if an I/O error occurs when obtaining the streams
     */
    public IOStreamInfo(Socket socket, int timeout)
            throws IOException {
        this(socket.getInputStream(), socket.getOutputStream());
        socket.setSoTimeout(timeout);
        s = socket;
    }

    /**
     * Closes the underlying input stream, output stream, and socket (if present).
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close()
            throws IOException {
        cdt.close();
    }

    /**
     * @return the input stream
     */
    public InputStream getInputStream() { return is; }

    /**
     * @return the output stream
     */
    public OutputStream getOutputStream() { return os; }

    /**
     * @return the underlying socket, or {@code null} if constructed from raw streams
     */
    public Socket getSocket() { return s; }
}
