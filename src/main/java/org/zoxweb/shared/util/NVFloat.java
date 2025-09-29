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

/**
 * This class declares NVBase of float type.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class NVFloat
        extends NVBase<Float> {
    /**
     * The default constructor (Java Bean compliant).
     */
    public NVFloat() {
    }

    /**
     * This constructor instantiates NVFloat based on name and value.
     * @param name
     * @param value
     */
    public NVFloat(String name, float value) {
        super(name, value);
    }

    public NVFloat(GetName name, float value) {
        super(name, value);
    }
}
