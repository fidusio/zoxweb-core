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
package org.zoxweb.shared.protocol;

import org.zoxweb.shared.util.GetName;

public enum ProtoMarker
        implements GetName {
    START,
    END,
    HEADER_START,
    HEADER_END,
    CONTENT_START,
    CONTENT_END,
//    BOUNDARY_TAG("boundary-tag"),
    BOUNDARY_START_TAG("boundary-start"),
    BOUNDARY_FINAL_TAG("boundary-end"),
    BOUNDARY_END_TAG("boundary-end"),
    BOUNDARY_CONTENT_END_TAG("boundary-content-end"),
    SUB_CONTENT_START_INDEX("sub-content-start-index"),
    SUB_CONTENT_END_INDEX("sub-content-end-index"),
    IS_FILE("is-file"),

    LAST_CHUNK("last-chunk"),
    TRANSFER_COMPLETED("transfer-completed"),
    ;

    private final String name;

    ProtoMarker(String name) {
        this.name = name.toLowerCase();
    }

    ProtoMarker() {
        this.name = name().toLowerCase();
    }

    @Override
    public String getName() {
        return name;
    }

    public String toString() {
        return getName();
    }

}