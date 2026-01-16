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
 * This class declares NVBase of enum type.
 * @author mzebib
 */
@SuppressWarnings("serial")
public class NVEnum
        extends NVBase<Enum<?>> {
    /**
     * The default constructor (Java Bean compliant).
     */
    public NVEnum() {
    }

    /**
     * This constructor instantiates NVEnum based on name and value.
     * @param name
     * @param value
     */
    public NVEnum(String name, Enum<?> value) {
        super(name, value);
    }

    public NVEnum(GetName name, Enum<?> value) {
        super(name, value);
    }

    public static NVEnum create(String name, Enum<?> value) {
        return new NVEnum(name, value);
    }
    public static NVEnum create(GetName name, Enum<?> value) {
        return new NVEnum(name, value);
    }

}
