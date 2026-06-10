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
 *
 * @param <T>
 */
@SuppressWarnings("serial")
public class ReferenceIDPortable<T>
        implements ReferenceID<T> {
    private T type;

    public ReferenceIDPortable() {
    }

    public ReferenceIDPortable(T val) {
        setGUID(val);
    }

    /**
     * @return the GUID
     */
    @Override
    public T getGUID() {
        return type;
    }

    /**
     * @param id as GUID
     */
    @Override
    public void setGUID(T id) {
        type = id;
    }

}
