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
 * This class declares NVBase as list of integer type.
 *
 * @author mzebib
 */
@SuppressWarnings("serial")
public class NVIntList
        extends NVBase<List<Integer>> {
    /**
     * The default constructor (Java Bean compliant).
     */
    public NVIntList() {

    }

    public NVIntList(String name) {
        super(name, new ArrayList<Integer>());
    }

    /**
     * This constructor instantiates NVIntList based on name and value.
     *
     * @param name
     * @param value
     */
    public NVIntList(String name, List<Integer> value) {
        super(name, value);
    }


    public int[] getValues() {
        Integer[] ret = getValue().toArray(new Integer[0]);
        int[] retArray = new int[ret.length];
        for (int i = 0; i < ret.length; i++) {
            retArray[i] = ret[i];
        }

        return retArray;
    }

    public void setValues(int[] vals) {
        List<Integer> values = getValue();
        values.clear();
        for (int i : vals) {
            values.add(i);
        }
    }
}
