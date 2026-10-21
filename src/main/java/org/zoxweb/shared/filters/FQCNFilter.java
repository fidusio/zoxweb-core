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
package org.zoxweb.shared.filters;

import org.zoxweb.shared.util.SUS;

/**
 * A fully qualified Java class name: one or more Java identifiers separated by dots, such as
 * {@code org.zoxweb.shared.data.PropertyImpl} or {@code com.acme.Outer$Inner}. Used for the entity
 * classes an app registers as its own ({@code AppIDDefault.getResourceTypes()}, 2026-10-20). The
 * validated value is the trimmed name; the class is not loaded, so a name may be registered on a
 * node that does not carry the class.
 */
@SuppressWarnings("serial")
public class FQCNFilter
        implements ValueFilter<String, String> {

    public static final FQCNFilter SINGLETON = new FQCNFilter();

    private FQCNFilter() {
    }

    @Override
    public String validate(String in)
            throws NullPointerException, IllegalArgumentException {
        SUS.checkIfNulls("Null class name", in);
        if (isValid(in)) {
            return in.trim();
        }
        throw new IllegalArgumentException("Invalid class name " + in);
    }

    @Override
    public boolean isValid(String in) {
        if (SUS.isEmpty(in)) {
            return false;
        }
        in = in.trim();
        boolean start = true;
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c == '.') {
                if (start) {
                    return false; // empty segment: leading, trailing or doubled dot
                }
                start = true;
                continue;
            }
            boolean ok = start ? (Character.isLetter(c) || c == '_' || c == '$')
                               : (Character.isLetterOrDigit(c) || c == '_' || c == '$');
            if (!ok) {
                return false;
            }
            start = false;
        }
        return !start;
    }
}
