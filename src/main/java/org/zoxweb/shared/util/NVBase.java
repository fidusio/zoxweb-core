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
package org.zoxweb.shared.util;

import java.io.Serializable;

/**
 * This is the base name value (NVBase) class that is the used by
 * subclasses within the package. This class contains methods that
 * set and retrieve the following variables: reference ID, name, and
 * value (of any data type based on NVBase).
 *
 * @param <V>
 * @author mzebib
 */
@SuppressWarnings("serial")
public class NVBase<V>
        implements Serializable, SetNameValue<V>, ReferenceID<String> {

    protected String referenceId;
    protected String name;
    protected V value;

    /**
     * This constructor maps GetNameValue to NVBase object.
     *
     * @param nv
     */
    public NVBase(GetNameValue<V> nv) {
        this(nv.getName(), nv.getValue());
    }

    /**
     * This constructor maps GetName to NVBase object and
     * generic value entered externally.
     *
     * @param gn
     * @param v
     */
    public NVBase(GetName gn, V v) {
        this(gn.getName(), v);
    }

    /**
     * This constructor instantiates NVBase based
     * on name and generic type value.
     *
     * @param name
     * @param value
     */
    public NVBase(String name, V value) {
        // Note value must be set first
        setValue(value);
        // name set next NOT FIRST
        setName(name);
    }

    /**
     * The default constructor.
     */
    public NVBase() {

    }

    /**
     * Returns the reference ID.
     */
    public String getReferenceID() {
        return referenceId;
    }

    /**
     * Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value.
     */
    public V getValue() {
        return value;
    }

    /**
     * Sets the reference ID.
     *
     * @param referenceId
     */
    public void setReferenceID(String referenceId) {
        this.referenceId = referenceId;
    }

    /**
     * Sets the name.
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the value.
     *
     * @param value
     */
    public void setValue(V value) {
        this.value = value;
    }

    /**
     * Returns a string containing reference ID, name and value.
     */
    public String toString() {
        return "{" + (referenceId != null ? referenceId + "," : "") + name + ":" + value + "}";
    }

}