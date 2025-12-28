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
 * This interface declares methods to set and retrieve the
 * domain ID.
 * @author mzebib
 *
 * @param <T>
 */
public interface DomainID<T> {
    /**
     * Gets the domain ID.
     * @return typed domain id
     */
    T getDomainID();

    /**
     * Sets the domain ID.
     * @param domainID
     */
    void setDomainID(T domainID);

}