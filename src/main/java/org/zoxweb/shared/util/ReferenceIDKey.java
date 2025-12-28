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
 *
 */
@SuppressWarnings("serial")
public class ReferenceIDKey
        implements Serializable, ReferenceID<String> {
    private ReferenceID<String> refID;

    public ReferenceIDKey() {
    }

    public ReferenceIDKey(ReferenceID<String> refID) {
        SUS.checkIfNulls("Reference ID is null.", refID);

        if (refID.getReferenceID() == null) {
            throw new NullPointerException("Reference ID value null.");
        }

        this.refID = refID;
    }

    public ReferenceIDKey(String refID) {
        SUS.checkIfNulls("Reference ID is null.", refID);
        this.refID = new ReferenceIDPortable<String>(refID);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (obj != null && refID != null) {
            if (obj == this || obj.equals(refID)) {
                return true;
            } else if (obj instanceof String) {
                return obj.equals(refID.getReferenceID());
            } else if (obj instanceof ReferenceID) {
                return refID.getReferenceID().equals(((ReferenceID<String>) obj).getReferenceID());
            }

        }
        return false;
    }

    @Override
    public int hashCode() {
        return refID.getReferenceID().hashCode();
    }

    /**
     * @see org.zoxweb.shared.util.ReferenceID#getReferenceID()
     * @return
     */
    @Override
    public String getReferenceID() {
        return refID.getReferenceID();
    }

    /**
     * @see org.zoxweb.shared.util.ReferenceID#setReferenceID(java.lang.Object)
     * @param id
     */
    @Override
    public void setReferenceID(String id) {
        refID.setReferenceID(id);
    }


}