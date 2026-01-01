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

/**
 * Enumeration of protocol parsing markers.
 * Used to identify different sections and boundaries during
 * protocol message parsing, particularly for multipart content.
 *
 * @author mnael
 * @see GetName
 */
public enum ProtoMarker
        implements GetName {
    /** Start of message marker */
    START,
    /** End of message marker */
    END,
    /** Start of header section */
    HEADER_START,
    /** End of header section */
    HEADER_END,
    /** Start of content section */
    CONTENT_START,
    /** End of content section */
    CONTENT_END,
    /** Multipart boundary start marker */
    BOUNDARY_START_TAG("boundary-start"),
    /** Multipart boundary final marker */
    BOUNDARY_FINAL_TAG("boundary-end"),
    /** Multipart boundary end marker */
    BOUNDARY_END_TAG("boundary-end"),
    /** Multipart boundary content end marker */
    BOUNDARY_CONTENT_END_TAG("boundary-content-end"),
    /** Start index for sub-content */
    SUB_CONTENT_START_INDEX("sub-content-start-index"),
    /** End index for sub-content */
    SUB_CONTENT_END_INDEX("sub-content-end-index"),
    /** Indicates content is a file */
    IS_FILE("is-file"),
    /** Marks the last chunk in chunked transfer */
    LAST_CHUNK("last-chunk"),
    /** Indicates transfer is completed */
    TRANSFER_COMPLETED("transfer-completed"),
    ;

    private final String name;

    /**
     * Constructor with custom name.
     *
     * @param name the marker name
     */
    ProtoMarker(String name) {
        this.name = name.toLowerCase();
    }

    /**
     * Default constructor using enum constant name.
     */
    ProtoMarker() {
        this.name = name().toLowerCase();
    }

    /**
     * Returns the marker name in lowercase.
     *
     * @return the marker name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the marker name.
     *
     * @return the marker name
     */
    public String toString() {
        return getName();
    }

}