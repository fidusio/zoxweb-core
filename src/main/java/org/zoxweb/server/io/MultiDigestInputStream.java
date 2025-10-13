/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 *
 *
 */
public class MultiDigestInputStream
        extends FilterInputStream
        implements MultiDigestInterface {

    /* NOTE: This should be made a generic UpdaterInputStream */

    /* Are we on or off? */
//    private boolean on = true;
//
//    /**
//     * The message digest associated with this stream.
//     */
//    protected MessageDigest[] digests;
//    
//    private AtomicLong totalBytes = new AtomicLong();

    private MultiDigest md = new MultiDigest();

    /**
     * Creates a digest input stream, using the specified input stream
     * and message digest.
     *
     * @param stream the input stream.
     *
     * @param digests the message digest to associate with this stream.
     */
    public MultiDigestInputStream(InputStream stream, MessageDigest... digests) {
        super(stream);
        setMessageDigests(digests);
    }

    /**
     * Returns the message digests associated with this stream.
     *
     * @return the message digests associated with this stream.
     */
    public MessageDigest[] getMessageDigests() {
        return md.getMessageDigests();
    }

    /**
     * Associates the specified message digests with this stream.
     *
     * @param digests the message digest to be associated with this stream.
     */
    public void setMessageDigests(MessageDigest[] digests) {
        md.setMessageDigests(digests);
    }

    /**
     * Reads a byte, and updates the message digests (if the digest
     * function is on).  That is, this method reads a byte from the
     * input stream, blocking until the byte is actually read. If the
     * digest function is on (see {@link #on(boolean) on}), this method
     * will then call <code>update</code> on the message digests associated
     * with this stream, passing it the byte read.
     *
     * @return the byte read.
     *
     * @exception IOException if an I/O error occurs.
     *
     */
    public int read() throws IOException {
        int ch = in.read();

        if (ch != -1) {
            md.update((byte) ch);
        }

        return ch;
    }

    /**
     * Reads into a byte array, and updates the message digests (if the
     * digest function is on).  That is, this method reads up to
     * <code>len</code> bytes from the input stream into the array
     * <code>b</code>, starting at offset <code>off</code>. This method
     * blocks until the data is actually
     * read. If the digest function is on (see
     * {@link #on(boolean) on}), this method will then call <code>update</code>
     * on the message digests associated with this stream, passing it
     * the data.
     *
     * @param b the array into which the data is read.
     *
     * @param off the starting offset into <code>b</code> of where the
     * data should be placed.
     *
     * @param len the maximum number of bytes to be read from the input
     * stream into b, starting at offset <code>off</code>.
     *
     * @return  the actual number of bytes read. This is less than
     * <code>len</code> if the end of the stream is reached prior to
     * reading <code>len</code> bytes. -1 is returned if no bytes were
     * read because the end of the stream had already been reached when
     * the call was made.
     *
     * @exception IOException if an I/O error occurs.
     *
     */
    public int read(byte[] b, int off, int len)
            throws IOException {
        int result = in.read(b, off, len);
        if (result != -1) {
            md.update(b, off, result);
        }
        return result;
    }

    /**
     * Turns the digest function on or off. The default is on.  When
     * it is on, a call to one of the <code>read</code> methods results in an
     * update on the message digest.  But when it is off, the message
     * digest is not updated.
     *
     * @param on true to turn the digest function on, false to turn
     * it off.
     */
    public void on(boolean on) {
        md.on(on);
    }

    public long totalBytes() {
        return md.totalBytes();
    }

    /**
     * return Prints a string representation of this digest input stream and its associated message digest object.
     */
    public String toString() {
        return md.toString();
    }

}