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
import org.zoxweb.shared.util.DataEncoder;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.Validator;

import java.io.Serializable;


/**
 * The filter interface is used to validate and check property values
 *
 * @param <I> Input value
 * @param <O> Output filtered value
 * @author mnael
 */
public interface ValueFilter<I, O>
        extends Serializable, CanonicalID, DataEncoder<I, O>, Validator<I> {


    /**
     * Adapts a {@link DataEncoder} into a {@link ValueFilter}.
     * <p>
     * If {@code encoder} already implements {@link ValueFilter} it is returned as is. Otherwise a wrapper is
     * created whose {@link #validate(Object)} rejects a null input with a {@link NullPointerException} and
     * delegates every other input to {@link DataEncoder#encode(Object)}; the encoder is responsible for
     * throwing {@link IllegalArgumentException} when the input is invalid. The wrapper's
     * {@link #isValid(Object)} and {@link #toCanonicalID()} are the interface defaults.
     * </p>
     * <p>
     * The wrapper holds a reference to {@code encoder}, so it is only Java-serializable when the encoder itself
     * is {@link Serializable}; lambda based encoders such as {@link DataEncoder#StringLower} are not.
     * </p>
     *
     * @param <I>     input value type
     * @param <O>     output filtered value type
     * @param encoder the encoder to adapt, must not be null
     * @return {@code encoder} itself if it is already a {@link ValueFilter}, otherwise a filter wrapping it
     * @throws NullPointerException if encoder is null
     */
    @SuppressWarnings("unchecked")
    static <I, O> ValueFilter<I, O> createValueFilter(DataEncoder<I, O> encoder) {
        SUS.checkIfNull("encoder null", encoder);
        if (encoder instanceof ValueFilter)
            return (ValueFilter<I, O>) encoder;

        return new ValueFilter<I,O>() {

            /**
             * Validate the object
             *
             * @param in value to be validated
             * @return validated acceptable value
             * @throws NullPointerException     if in is null
             * @throws IllegalArgumentException if in is invalid
             */
            @Override
            public O validate(I in) throws NullPointerException, IllegalArgumentException {
                SUS.checkIfNull("input value null", in);
                return encoder.encode(in);
            }
        };
    }


    /**
     * Encodes by validating: a {@link ValueFilter} is a {@link DataEncoder} whose output is the validated input.
     *
     * @param input value to be validated
     * @return the validated value, see {@link #validate(Object)}
     * @throws NullPointerException     if input is null
     * @throws IllegalArgumentException if input is invalid
     */
    default O encode(I input) {
        return validate(input);
    }

    /**
     * Validate the object
     *
     * @param in value to be validated
     * @return validated acceptable value
     * @throws NullPointerException     if in is null
     * @throws IllegalArgumentException if in is invalid
     */
    O validate(I in)
            throws NullPointerException, IllegalArgumentException;

    /**
     * Check if the value is valid
     *
     * @param in value to be checked
     * @return true if in value valid
     */
    default boolean isValid(I in) {
        try {
            validate(in);
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    /**
     * Canonical identifier of this filter, by default the fully qualified class name.
     *
     * @return the canonical identifier
     */
    default String toCanonicalID() {
        return getClass().getName();
    }
}