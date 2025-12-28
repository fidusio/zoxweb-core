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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;


/**
 * [Please state the purpose for this class or method because it will help the team for future maintenance ...].
 *
 */
public class MultiDigestOutputStream
        extends FilterOutputStream
        implements MultiDigestInterface {

//    private boolean on = true;
//    private AtomicLong totalBytes = new AtomicLong();
//
//    /**
//     * The message digest associated with this stream.
//     */
//    protected MessageDigest[] digests;


    private MultiDigest md = new MultiDigest();

    /**
     * Creates a digest output stream, using the specified output stream
     * and message digest.
     *
     * @param stream the output stream.
     *
     * @param digests the message digest to associate with this stream.
     */
    public MultiDigestOutputStream(OutputStream stream, MessageDigest... digests) {
        super(stream);
        //setMessageDigests(digest);
        md.setMessageDigests(digests);
    }

    /**
     * Returns the message digest associated with this stream.
     *
     * @return the message digest associated with this stream.

     */
    public MessageDigest[] getMessageDigests() {
        return md.getMessageDigests();
    }

    /**
     * Associates the specified message digest with this stream.
     *
     * @param digests the message digest to be associated with this stream.

     */
    public void setMessageDigests(MessageDigest[] digests) {
        md.setMessageDigests(digests);
    }

    /**
     * Updates the message digest (if the digest function is on) using
     * the specified byte, and in any case writes the byte
     * to the output stream. That is, if the digest function is on
     * (see {@link #on(boolean) on}), this method calls
     * <code>update</code> on the message digest associated with this
     * stream, passing it the byte <code>b</code>. This method then
     * writes the byte to the output stream, blocking until the byte
     * is actually written.
     *
     * @param b the byte to be used for updating and writing to the
     * output stream.
     *
     * @exception IOException if an I/O error occurs.
     *
     * @see MessageDigest#update(byte)
     */
    public void write(int b) throws IOException {
        out.write(b);
        md.update((byte) b);

    }

    /**
     * Updates the message digest (if the digest function is on) using
     * the specified subarray, and in any case writes the subarray to
     * the output stream. That is, if the digest function is on (see
     * {@link #on(boolean) on}), this method calls <code>update</code>
     * on the message digest associated with this stream, passing it
     * the subarray specifications. This method then writes the subarray
     * bytes to the output stream, blocking until the bytes are actually
     * written.
     *
     * @param b the array containing the subarray to be used for updating
     * and writing to the output stream.
     *
     * @param off the offset into <code>b</code> of the first byte to
     * be updated and written.
     *
     * @param len the number of bytes of data to be updated and written
     * from <code>b</code>, starting at offset <code>off</code>.
     *
     * @exception IOException if an I/O error occurs.
     *
     * @see MessageDigest#update(byte[], int, int)
     */
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        md.update(b, off, len);
    }

    /**
     * Turns the digest function on or off. The default is on.  When
     * it is on, a call to one of the <code>write</code> methods results in an
     * update on the message digest.  But when it is off, the message
     * digest is not updated.
     *
     * @param on true to turn the digest function on, false to turn it
     * off.
     */
    public void on(boolean on) {
        md.on(on);
    }

    public long totalBytes() {
        return md.totalBytes();
    }

    /**
     * Prints a string representation of this digest output stream and
     * its associated message digest object.
     */
    public String toString() {
        return md.toString();
    }
}
