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

import org.zoxweb.shared.filters.ValueFilter;

import java.io.Serializable;

/**
 * This class implements the NVConfig interface. This class 
 * initializes and returns the attributes of NVConfig which 
 * serves as a parent class for NVConfigEntityLocal class.
 * @author mzebib
 */
@SuppressWarnings("serial")
public class NVConfigPortable
        implements NVConfig, Serializable {
    /**
     * This method returns a string containing NVConfig name,
     * description, mandatory condition, editable condition,
     * meta type, and display name.
     */
    public String toString() {
        return "NVConfigLocal [name=" + name + ", description=" + description
                + ", mandatory=" + mandatory + ", editable=" + editable
                + ", metaType=" + metaType + ", displayName=" + displayName
                + "]";
    }

    private String name;
    private String description;
    private boolean mandatory;
    private boolean editable;
    protected Class<?> metaType;
    private String displayName;
    protected boolean isArray = false;
    private ValueFilter<?, ?> valueFilter;
    private boolean unique = false;
    private boolean hidden = false;
    private boolean referenceIDType = false;
    private boolean isStatic = true;
    protected transient boolean embedded = false;


    /**
     * Checks if the property is unique.
     * @return true if unique
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * Sets whether the property is unique.
     * @param unique
     */
    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    /**
     * The default constructor (Java Bean compliant).
     */
    public NVConfigPortable() {

    }

    /**
     * This constructor instantiates NVConfigLocal based on set values
     * for the following parameters:
     * @param name
     * @param description
     * @param displayName
     * @param man
     * @param edit
     * @param type
     * @param vf
     */
    public NVConfigPortable(String name, String description, String displayName, boolean man, boolean edit, boolean isUnique, boolean isHidden, boolean isRefID, Class<?> type, ValueFilter<?, ?> vf) {
        setName(name);
        setDescription(description);
        setMandatory(man);
        setEditable(edit);
        setMetaType(type);
        setUnique(isUnique);
        setHidden(isHidden);
        setDisplayName(displayName);
        setValueFilter(vf);
        setTypeReferenceID(isRefID);

        //This equality technique might be invalid.
        //The reason might be related to different classloader.
        //Must be tested on the server side and client side.
        if (DynamicEnumMap.class.equals(type)) {
            if (vf == null || !(vf instanceof DynamicEnumMap)) {
                throw new IllegalArgumentException("DynamicEnumMap type requires DynamicEnumMap object as a value filter.");
            }

        }
    }


    /**
     * Returns the name.
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the description.
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     * @param str
     */
    public void setDescription(String str) {
        this.description = str;
    }

    /**
     * Checks if attribute is mandatory.
     * @return true if mandatory
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * Sets mandatory condition of attribute.
     * @param mandatory
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * Checks if attribute is editable.
     * @return true if user editable
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Sets editable condition of attribute.
     * @param val
     */
    public void setEditable(boolean val) {
        editable = val;
    }

    /**
     * Returns the meta type.
     * @return class type
     */
    public Class<?> getMetaType() {
        return metaType;
    }

    /**
     * Sets the meta type.
     * @param metaType
     */
    public void setMetaType(Class<?> metaType) {
        this.metaType = Const.wrap(metaType);
    }

    /**
     * Returns the display name.
     * @return display name
     */
    public String getDisplayName() {
        if (displayName == null) {
            return getName();
        }

        return displayName;
    }

    /**
     * Sets the display name.
     * @param displayName
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the value filter.
     * @return value filter
     */
    public synchronized ValueFilter<?, ?> getValueFilter() {
        if (valueFilter != null && valueFilter instanceof DynamicEnumMap) {
            ValueFilter<String, String> dem = DynamicEnumMapManager.SINGLETON.lookup(valueFilter.toCanonicalID());

            if (dem != null) {
                return dem;
            }
        }

        return valueFilter;
    }

    /**
     * Sets the value filter.
     * @param vf
     */
    public synchronized void setValueFilter(@SuppressWarnings("rawtypes") ValueFilter vf) {
        valueFilter = vf;
    }

    /**
     * Checks if property is an array.
     * @return true if array
     */
    @Override
    public boolean isArray() {
        return (getMetaType().isArray() || isArray);
    }

    /**
     * Sets if property is an array.
     * @param array
     */
    @Override
    public void setArray(boolean array) {
        isArray = array;
    }

    /**
     * Returns the base meta type.
     * @return the base class if type is array
     */
    @Override
    public Class<?> getMetaTypeBase() {
        return getMetaType().isArray() ? getMetaType().getComponentType() : getMetaType();
    }

    /**
     * Checks if property is an enum.
     * @return true if enum
     */
    @Override
    public boolean isEnum() {
        if (getMetaTypeBase() == DynamicEnumMap.class) {
            return true;
        }

        return getMetaTypeBase().isEnum();
    }

    /**
     * Sets if property is hidden.
     * @param hidden
     */
    @Override
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * Checks if the property is hidden.
     * @return true if hidden
     */
    @Override
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @see org.zoxweb.shared.util.NVConfig#isTypeReferenceID()
     * @return
     */
    @Override
    public boolean isTypeReferenceID() {
        return referenceIDType;
    }

    /**
     * @see org.zoxweb.shared.util.NVConfig#setTypeReferenceID(boolean)
     * @param type
     */
    @Override
    public void setTypeReferenceID(boolean type) {
        referenceIDType = type;
    }

    /**
     * This a meta reference used for persistence
     *
     * @return true if the NVEntity needs to be embedded
     */
    @Override
    public boolean isEmbedded() {
        return embedded;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean staticVal) {
        isStatic = staticVal;
    }


}