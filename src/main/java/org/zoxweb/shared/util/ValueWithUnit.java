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

/**
 * Default implementation of UnitValue interface.
 * @param <V>
 */
public class ValueWithUnit<V, U>
        implements ValueUnit<V, U> {

    private String separator = " ";
    private String name;

    private V value;
    private U unit;

    public ValueWithUnit() {

    }

    public ValueWithUnit(V value, U unit) {
        setValue(value);
        setUnit(unit);
    }


    public ValueWithUnit(V value, String separator, U unit) {
        setValue(value);
        setUnit(unit);
        setSeparator(separator);
    }

    /**
     * Returns the value.
     * @return
     */
    @Override
    public V getValue() {
        return value;
    }

    /**
     * Sets the value.
     * @param value
     */
    @Override
    public void setValue(V value) {
        this.value = value;
    }

    /**
     * Returns the unit.
     * @return
     */
    @Override
    public U getUnit() {
        return unit;
    }

    /**
     * Sets the unit.
     * @param unit
     */
    @Override
    public void setUnit(U unit) {
        this.unit = unit;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String sep) {
        separator = sep;
    }


    public String toString() {
        return getValue() + getSeparator() + getUnit();
    }
}