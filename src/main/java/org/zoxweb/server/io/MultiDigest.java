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

import org.zoxweb.shared.util.SharedStringUtil;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class MultiDigest
        implements MultiDigestInterface {

    private boolean on = true;
    private AtomicLong totalBytes = new AtomicLong();

    /**
     * The message digest associated with this stream.
     */
    protected MessageDigest[] digests;

    /**
     * Associates the specified message digest with this stream.
     * @param digests the message digest to be associated with this stream.
     */
    public void setMessageDigests(MessageDigest[] digests) {
        this.digests = digests;
    }

    /**
     * Returns the message digest associated with this stream.
     * @return the message digest associated with this stream.
     */
    public MessageDigest[] getMessageDigests() {
        return digests;
    }

    /**
     * Turns the digest function on or off. The default is on.  When
     * it is on, a call to one of the <code>write</code> methods results in an
     * update on the message digest.  But when it is off, the message
     * digest is not updated.
     * @param on true to turn the digest function on, false to turn it off.
     */
    public void on(boolean on) {
        this.on = on;
    }

    public long totalBytes() {
        return totalBytes.get();
    }

    /**
     *
     * @return Prints a string representation of this digest output stream and its associated message digest object.
     */
    public String toString() {
        return "Total bytes:" + totalBytes + ". Digests:" + Arrays.toString(digests);
    }

    public void update(byte b[], int off, int len) {
        if (on && b != null) {
            totalBytes.addAndGet(len);

            if (digests != null) {
                for (MessageDigest digest : digests) {
                    digest.update(b, off, len);
                }
            }
        }
    }


    public void update(byte b) {
        if (on) {
            totalBytes.incrementAndGet();
            if (digests != null) {
                for (MessageDigest digest : digests)
                    digest.update(b);
            }
        }
    }

    public void update(char b[], int off, int len) {
        String str = new String(b, off, len);
        update(SharedStringUtil.getBytes(str), 0, str.length());
    }

    public void update(char b) {
        char[] temp = new char[1];
        temp[0] = b;
        String str = new String(temp);
        update(SharedStringUtil.getBytes(str), 0, str.length());
    }


    public void update(String str) {
        update(SharedStringUtil.getBytes(str), 0, str.length());
    }
}
