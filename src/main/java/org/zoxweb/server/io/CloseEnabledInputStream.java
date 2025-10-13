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

/**
 *
 */
public class CloseEnabledInputStream
        extends FilterInputStream {

    protected volatile boolean autoCloseable;

    public CloseEnabledInputStream(InputStream in) {
        this(in, true);
    }

    /**
     *
     * @param in
     * @param autoClose
     */
    public CloseEnabledInputStream(InputStream in, boolean autoClose) {
        super(in);
        this.autoCloseable = autoClose;
    }

    /**
     *
     * @return
     */
    public boolean isCloseEnabled() {
        return autoCloseable;
    }

    /**
     *
     * @throws IOException
     */
    public void close()
            throws IOException {
        if (isCloseEnabled()) {
            super.close();
        }
    }

}