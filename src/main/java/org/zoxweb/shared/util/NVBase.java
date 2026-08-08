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

import java.io.Serializable;

/**
 * This is the base name value (NVBase) class that is used by
 * subclasses within the package. It holds a name, a value of the
 * generic type, and a GUID, with getters and setters for each.
 * NVBase instances are the typed field holders that back the
 * NVEntity meta-model.
 *
 * @param <V> the value type held by this name value pair
 * @author mzebib
 */
@SuppressWarnings("serial")
public abstract class NVBase<V>
        implements Serializable, SetNameValue<V>, ReferenceID<String> {

    protected String guid;
    protected String name;
    protected V value;

    /**
     * This constructor maps GetNameValue to NVBase object.
     *
     * @param gnv the GetNameValue object
     */
    protected NVBase(GetNameValue<V> gnv) {
        this(gnv.getName(), gnv.getValue());
    }

    /**
     * This constructor maps GetName to NVBase object and
     * generic value entered externally.
     *
     * @param gn GetName  object
     * @param v value
     */
    protected NVBase(GetName gn, V v) {
        this(gn.getName(), v);
    }

    /**
     * This constructor instantiates NVBase based
     * on name and generic type value.
     *
     * @param name of the object
     * @param value of the object
     */
    protected NVBase(String name, V value) {
        // Note value must be set first
        setValue(value);
        // name set next NOT FIRST
        setName(name);
        SharedMetaUtil.SINGLETON.incCreationCount();
    }

    /**
     * The default constructor.
     */
    protected NVBase() {
        SharedMetaUtil.SINGLETON.incCreationCount();
    }


    /**
     * Returns the name.
     * @return the name of the pair
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value.
     * @return the value of the pair
     */
    public V getValue() {
        return value;
    }


    /**
     * Sets the global identifier.
     * @param guid to be set
     */
    @Override
    public void setGUID(String guid) {
        this.guid = guid;
    }

    /**
     * Returns the global identifier.
     * @return the guid, null if never set
     */
    @Override
    public String getGUID() {
        return guid;
    }


    /**
     * Sets the name.
     *
     * @param name to be set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the value.
     *
     * @param value to be set
     */
    public void setValue(V value) {
        this.value = value;
    }

    /**
     * Returns a string containing the GUID (if set), name and value
     * as {guid,name:value}.
     */
    public String toString() {
        return "{" + (guid != null ? guid + "," : "") + name + ":" + value + "}";
    }

}