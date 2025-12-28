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

//import java.util.ArrayList;

import java.util.List;

/**
 * This interface extends the NVConfig interface and methods of NVConfig
 * type are declared in this interface.
 *
 * @author mzebib
 */
public interface NVConfigEntity
        extends NVConfig, ReferenceID<String>, DomainID<String>, GlobalID<String>, CanonicalID {

    enum ArrayType {
        NOT_ARRAY,
        LIST,
        GET_NAME_MAP,
        REFERENCE_ID_MAP
    }

    /**
     * Returns the list of attributes of NVConfig type.
     *
     * @return list of meta attribute
     */
    List<NVConfig> getAttributes();

    /**
     * Sets the list of attributes of NVConfig type.
     *
     * @param attList is an array list of NVConfig type
     */
    void setAttributes(List<NVConfig> attList);

    /**
     * Returns the list of the specified attributes of
     * NVConfig type to be displayed.
     *
     * @return display attributes
     */
    List<NVConfig> getDisplayAttributes();

    /**
     * Sets the list of attributes of NVConfig type to be displayed.
     *
     * @param attList is an array list of NVConfig type
     */
    void setDisplayAttributes(List<NVConfig> attList);

    /**
     * Looks up the matching NVConfig by name.
     *
     * @param name is an array list of NVConfig type
     * @return nvconfig matching name
     */
    NVConfig lookup(String name);

    /**
     * Looks up the matching NVConfig by enum value.
     *
     * @param e is of generic enum type
     * @return nvconfig matching e
     */
    NVConfig lookup(Enum<?> e);

//	/**
//	 * Checks whether meta initialization is complete or not.
//	 * @return
//	 */
//	boolean isMetaInitComplete();
//	
//	/**
//	 * Sets the status of the meta initialization.
//	 * @param stat is of boolean type
//	 */
//	void setMetaInitComplete( boolean stat);

    /**
     * Sets the referenced NVConfigEntity.
     *
     * @param nvce
     */
    void setReferencedNVConfigEntity(NVConfigEntity nvce);

    /**
     * This method returns the referenced NVConfigEntity.
     *
     * @return the referenced NVConfigEntity.
     */
    NVConfigEntity getReferencedNVConfigEntity();

    boolean isReferenced();

    /**
     * Checks whether attribute validation is required.
     *
     * @return true if attribute validation is required.
     */
    boolean isAttributesValidationRequired();

    /**
     * Sets attribute validation requirement.
     *
     * @param attributesValidationRequired
     */
    void setAttributesValidationRequired(boolean attributesValidationRequired);

    ArrayType getArrayType();


    void setArrayType(ArrayType at);



    //void setEmbedded(boolean embedded);


}
