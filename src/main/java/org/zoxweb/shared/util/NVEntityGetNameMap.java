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
public class NVEntityGetNameMap
        extends NVBase<Map<GetName, NVEntity>>
        implements ArrayValues<NVEntity> {


    public NVEntityGetNameMap() {
    }

    public NVEntityGetNameMap(String name) {
        super(name, new LinkedHashMap<GetName, NVEntity>());
    }

    public NVEntity get(GetName getName) {
        if (getName != null && getName.getName() != null) {
            return get(getName.getName());
        }

        return null;
    }

    public synchronized NVEntity get(String name) {
        return value.get(new GetNameKey(name, true));
    }

    public synchronized NVEntity remove(String name) {
        return value.remove(new GetNameKey(name, true));
    }

    public synchronized NVEntity remove(GetName name) {
        return value.remove(new GetNameKey(name, true));
    }


    public synchronized NVEntity add(NVEntity nve) {
        return value.put(new GetNameKey((GetName) nve, true), nve);
    }

    public synchronized NVEntity remove(NVEntity nve) {
        return value.remove(new GetNameKey((GetName) nve, true));
    }

    public int size() {
        return value.size();
    }

    public NVEntity[] values() {
        return value.values().toArray(new NVEntity[0]);
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
    public void add(NVEntity[] vals, boolean clear) {
        if (clear) {
            clear();
        }
        if (vals != null) {
            for (NVEntity nve : vals) {
                add(nve);
            }
        }

    }


    /**
     * @see org.zoxweb.shared.util.ArrayValues#search(java.lang.String[])
     */
    @Override
    public List<NVEntity> search(String... criteria) {
        // TODO Auto-generated method stub
        return SharedUtil.search(values(), criteria);
    }


    /**
     * @see org.zoxweb.shared.util.ArrayValues#isFixed()
     */
    @Override
    public boolean isFixed() {
        // TODO Auto-generated method stub
        return false;
    }


    /**
     * @see org.zoxweb.shared.util.ArrayValues#setFixed(boolean)
     */
    @Override
    public void setFixed(boolean isFixed) {
        // TODO Auto-generated method stub
    }


}
