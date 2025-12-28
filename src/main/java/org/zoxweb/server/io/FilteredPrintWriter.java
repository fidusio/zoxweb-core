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

import org.zoxweb.server.logging.LogWrapper;

import java.io.PrintWriter;
import java.io.Writer;
import java.security.MessageDigest;

/**
 *
 */
public class FilteredPrintWriter
        extends PrintWriter {
    public static final LogWrapper log = new LogWrapper(FilteredPrintWriter.class);

    private MultiDigest mdw = new MultiDigest();

    /**
     * [Optional - Please state the purpose of this constructor; especially if multiple constructors exist.].
     *
     * @param out
     * @param digests
     */
    public FilteredPrintWriter(Writer out, MessageDigest... digests) {
        super(out);
        mdw.setMessageDigests(digests);
    }


    public void write(char buf[], int off, int len) {
        super.write(buf, off, len);
        mdw.update(buf, off, len);
    }

    public void close() {
        super.close();
        log.getLogger().info("stat:" + mdw);
    }


}
