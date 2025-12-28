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
        setReferenceID(val);
    }

    /**
     * @see org.zoxweb.shared.util.ReferenceID#getReferenceID()
     * @return
     */
    @Override
    public T getReferenceID() {
        return type;
    }

    /**
     * @see org.zoxweb.shared.util.ReferenceID#setReferenceID(java.lang.Object)
     * @param id
     */
    @Override
    public void setReferenceID(T id) {
        type = id;
    }

}
