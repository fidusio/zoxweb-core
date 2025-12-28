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

import org.zoxweb.shared.util.CanonicalID;
import org.zoxweb.shared.util.Validator;

import java.io.Serializable;


/**
 * The filter interface is used to validate and check property values
 * @author mnael
 *
 * @param <I> Input value
 * @param <O> Output filtered value
 */
public interface ValueFilter<I, O>
        extends Serializable, CanonicalID, Validator<I> {

    /**
     * Validate the object
     * @param in value to be validated
     * @return validated acceptable value
     * @throws NullPointerException if in is null
     * @throws IllegalArgumentException if in is invalid
     */
    O validate(I in)
            throws NullPointerException, IllegalArgumentException;

    /**
     * Check if the value is valid
     * @param in value to be checked
     * @return true if in value valid
     */
    boolean isValid(I in);
}