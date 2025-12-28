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
package org.zoxweb.server.api.provider.notification.smtp;

import org.zoxweb.shared.filters.ValueFilter;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SharedStringUtil;

@SuppressWarnings("serial")
public class SMTPMessageIDFilter
        implements ValueFilter<String[], String> {

    /**
     * This variable declares that only one instance of this class can be
     * created.
     */
    public static final SMTPMessageIDFilter SINGLETON = new SMTPMessageIDFilter();


    /**
     * The default constructor is declared private to prevent
     * outside instantiation of this class.
     */
    private SMTPMessageIDFilter() {

    }

    //Pending Issue: Support more than recipient email!
    public String validate(String[] v)
            throws NullPointerException, IllegalArgumentException {
        SUS.checkIfNulls("Null parameter", (Object) v);

        if (v.length == 0) {
            throw new IllegalArgumentException("Message id empty");
        }

        String ret = SharedStringUtil.valueAfterLeftToken(v[0], "<");
        ret = SharedStringUtil.valueBeforeRightToken(ret, ">");

        return ret;
    }

    /**
     * Checks if given input is valid.
     * @param v
     * @return
     */
    public boolean isValid(String[] v) {
        try {
            validate(v);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * Returns canonical ID.
     * @return
     */
    public String toCanonicalID() {
        return SMTPMessageIDFilter.class.getName();
    }

}