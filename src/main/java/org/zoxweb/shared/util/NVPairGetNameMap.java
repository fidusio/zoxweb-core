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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
@SuppressWarnings("serial")
public class NVPairGetNameMap
        extends NVBase<Map<GetName, GetNameValue<String>>>
        implements ArrayValues<GetNameValue<String>> {

    private boolean isFixed = false;

    public NVPairGetNameMap() {
    }


    public NVPairGetNameMap(String name, List<GetNameValue<String>> list) {
        super(name, new LinkedHashMap<GetName, GetNameValue<String>>());
        for (GetNameValue<String> gnv : list) {
            add(gnv);
        }
    }

    public NVPairGetNameMap(String name, Map<GetName, GetNameValue<String>> map) {
        super(name, map);
    }

    public GetNameValue<String> get(GetName getName) {
        if (getName != null && getName.getName() != null) {
            return get(getName.getName());
        }

        return null;
    }

    public synchronized GetNameValue<String> get(String name) {
        return value.get(new GetNameKey(name, true));
    }

    public synchronized GetNameValue<String> remove(String name) {
        return value.remove(new GetNameKey(name, true));
    }


    public synchronized GetNameValue<String> remove(GetName name) {
        return value.remove(new GetNameKey(name, true));
    }

    public synchronized GetNameValue<String> add(GetNameValue<String> nve) {
        return value.put(new GetNameKey(nve, true), nve);
    }

    public synchronized GetNameValue<String> remove(GetNameValue<String> nve) {
        return value.remove(new GetNameKey(nve, true));
    }

    public int size() {
        return value.size();
    }

    public GetNameValue<String>[] values() {
        return value.values().toArray(new NVPair[0]);
    }

    public <V> V[] valuesAs(V[] t) {
        return value.values().toArray(t);
    }

    /**
     * @see org.zoxweb.shared.util.ArrayValues#clear()
     */
    @Override
    public void clear() {
        value.clear();
    }

    /**
     * @see org.zoxweb.shared.util.ArrayValues#add(java.lang.Object[], boolean)
     */
    @Override
    public void add(GetNameValue<String>[] values, boolean clear) {
        if (clear) {
            clear();
        }

        if (values != null) {
            for (GetNameValue<String> nvp : values) {
                add(nvp);
            }
        }
    }


    public List<GetNameValue<String>> search(String... criteria) {
        return SharedUtil.search(values(), criteria[0]);
    }

    /**
     * @see org.zoxweb.shared.util.ArrayValues#isFixed()
     */
    @Override
    public boolean isFixed() {
        return isFixed;
    }

    /**
     * @see org.zoxweb.shared.util.ArrayValues#setFixed(boolean)
     */
    @Override
    public void setFixed(boolean isFixed) {
        this.isFixed = isFixed;
    }


}