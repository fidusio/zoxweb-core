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
 * The reference ID is a unique object identifier with a object set, it could be in different contexts:
 * <ol>
 * <li> As database object primary key
 * <li> As a unique identifier within a set
 * <li> As a cross platform object identifier as processes or systems
 * </ol>
 * @author mnael
 *
 */
public interface ReferenceID<T>
        extends Serializable {
    /**
     * Get reference unique ID.
     * @return the object reference unique identifier
     */
    T getReferenceID();

    /**
     * Set reference unique ID.
     * @param id
     */
    void setReferenceID(T id);

}