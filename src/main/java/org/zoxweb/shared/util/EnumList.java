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
 * This class defines the enum class name and list.
 * @author mzebib
 */
@SuppressWarnings("serial")
public class EnumList
        implements Serializable {

    private String enumClassName;
    private Enum<?>[] enumList;

    /**
     * The default constructor.
     */
    public EnumList() {

    }

    /**
     * This constructor initializes EnumList object based on name and list.
     * @param name
     * @param list
     */
    public EnumList(String name, Enum<?>[] list) {
        setEnumClassName(name);
        setEnumList(list);
    }

    /**
     * Returns the enum class name.
     * @return enaum class name
     */
    public final String getEnumClassName() {
        return enumClassName;
    }

    /**
     * Sets the enum class name.
     * @param enumClassName
     */
    public void setEnumClassName(String enumClassName) {
        this.enumClassName = enumClassName;
    }

    /**
     * Returns an enum list.
     * @return all enums
     */
    public final Enum<?>[] getEnumList() {
        return enumList;
    }

    /**
     * Sets an enum list.
     * @param enumList
     */
    public void setEnumList(Enum<?>[] enumList) {
        this.enumList = enumList;
    }

}