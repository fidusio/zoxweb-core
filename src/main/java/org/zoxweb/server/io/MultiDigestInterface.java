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

import java.security.MessageDigest;

/**
 * [Please state the purpose for this class or method because it will help the team for future maintenance ...].
 *
 */
public interface MultiDigestInterface
        extends StreamStats {

    /**
     * Associates the specified message digest with this stream.
     *
     * @param digests the message digest to be associated with this stream.)
     */
    public void setMessageDigests(MessageDigest[] digests);

    /**
     * Returns the message digest associated with this stream.
     *
     * @return the message digest associated with this stream.
     */
    public MessageDigest[] getMessageDigests();

    /**
     * Turns the digest function on or off. The default is on.  When
     * it is on, a call to one of the <code>write</code> methods results in an
     * update on the message digest.  But when it is off, the message
     * digest is not updated.
     *
     * @param on true to turn the digest function on, false to turn it off.
     */
    public void on(boolean on);


}