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
package org.zoxweb.shared.util;

import java.util.List;

/**
 * Contract for a collection of values addressable by name, exposed as an
 * array. It abstracts the container behind array-typed NVBase holders
 * (e.g. NVEntity references, name value lists) with add/remove/lookup by
 * name or by element.
 *
 * @param <T> the element type of the collection
 */
public interface ArrayValues<T> {

    /**
     * Looks up an element by name.
     * @param str the name of the element
     * @return the matching element, null if not found
     */
    T get(String str);

    /**
     * Looks up an element by the name of the given GetName.
     * @param getName holder of the name to look up
     * @return the matching element, null if not found
     */
    T get(GetName getName);

    /**
     * Returns the number of elements.
     * @return the element count
     */
    int size();

    /**
     * Returns all the elements as an array.
     * @return array of all elements
     */
    T[] values();

    /**
     * Returns all the elements as an array of the requested type.
     * @param v array defining the return type, refilled or reallocated as needed
     * @param <V> the requested array element type
     * @return array of all elements as V
     */
    <V> V[] valuesAs(V[] v);

    /**
     * Adds an element.
     * @param v the element to add
     * @return the added element
     */
    T add(T v);

    /**
     * Removes an element.
     * @param v the element to remove
     * @return the removed element, null if not found
     */
    T remove(T v);

    /**
     * Removes an element by name.
     * @param str the name of the element to remove
     * @return the removed element, null if not found
     */
    T remove(String str);

    /**
     * Removes an element by the name of the given GetName.
     * @param getName holder of the name to remove
     * @return the removed element, null if not found
     */
    T remove(GetName getName);

    /**
     * Removes all the elements.
     */
    void clear();

    /**
     * Adds an array of elements in bulk.
     * @param vals the elements to add
     * @param clear if true the collection is cleared first
     */
    void add(T[] vals, boolean clear);

    /**
     * Searches the elements matching the given criteria.
     * @param criteria implementation specific search tokens
     * @return list of matching elements
     */
    List<T> search(String... criteria);

    /**
     * Checks if the collection is fixed (not modifiable).
     * @return true if fixed
     */
    boolean isFixed();

    /**
     * Sets the fixed status of the collection.
     * @param isFixed true to mark the collection fixed
     */
    void setFixed(boolean isFixed);

}