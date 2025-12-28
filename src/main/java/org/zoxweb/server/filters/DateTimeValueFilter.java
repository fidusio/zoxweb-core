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
package org.zoxweb.server.filters;

import org.zoxweb.server.util.DateUtil;
import org.zoxweb.shared.filters.ValueFilter;

import java.text.ParseException;
import java.text.SimpleDateFormat;


@SuppressWarnings("serial")
public class DateTimeValueFilter
        implements ValueFilter<String, Long> {

    public final SimpleDateFormat SDF;

    public DateTimeValueFilter(String pattern) {
        SDF = new SimpleDateFormat(pattern);
    }


    public DateTimeValueFilter(String pattern, String timezone) {
        SDF = DateUtil.createSDF(pattern, timezone);
    }

    /**
     * @see org.zoxweb.shared.util.CanonicalID#toCanonicalID()
     * @return
     */
    public String toCanonicalID() {
        return null;
    }

    /**
     * @see org.zoxweb.shared.filters.ValueFilter#validate(java.lang.Object)
     * @param in value to be validated
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public Long validate(String in)
            throws NullPointerException, IllegalArgumentException {
        try {
            return SDF.parse(in).getTime();
        } catch (ParseException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * @see org.zoxweb.shared.filters.ValueFilter#isValid(java.lang.Object)
     * @param in value to be checked
     * @return
     */
    public boolean isValid(String in) {
        try {
            validate(in);
            return true;
        } catch (Exception e) {

        }
        return false;
    }

}