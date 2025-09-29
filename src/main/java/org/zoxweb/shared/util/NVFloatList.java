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

import java.util.ArrayList;
import java.util.List;

/**
 * This class declare NVBase as a list of float type.
 * @author mzebib
 *
 */
@SuppressWarnings("serial")
public class NVFloatList
        extends NVBase<List<Float>> {

    /**
     * Default constructor used for Java Bean Compiler.
     */
    public NVFloatList() {
    }

    public NVFloatList(String name) {
        super(name, new ArrayList<Float>());
    }

    /**
     * This constructor instantiates NVFLoatList based on name and value.
     * @param name
     * @param value
     */
    public NVFloatList(String name, List<Float> value) {
        super(name, value);
    }

}