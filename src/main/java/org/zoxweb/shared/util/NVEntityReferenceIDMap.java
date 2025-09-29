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


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [Please state the purpose for this class or method because it will help the team for future maintenance ...].
 *
 */
@SuppressWarnings("serial")
public class NVEntityReferenceIDMap
        extends NVBase<Map<ReferenceID<String>, NVEntity>>
        implements ArrayValues<NVEntity> {


    public NVEntityReferenceIDMap() {

    }

    public NVEntityReferenceIDMap(String name) {
        super(name, new LinkedHashMap<ReferenceID<String>, NVEntity>());
    }

    public NVEntity get(GetName getName) {
        if (getName != null && getName.getName() != null) {
            return get(getName.getName());
        }

        return null;
    }

    public synchronized NVEntity get(String refID) {
        NVEntity ret = value.get(new ReferenceIDKey(refID));

        if (ret == null) {
            List<NVEntity> list = search(refID);

            if (list != null && list.size() == 1) {
                ret = list.get(0);
            }
        }


        return ret;
    }

    public synchronized NVEntity remove(String refID) {
        return value.remove(new ReferenceIDKey(refID));
    }

    public synchronized NVEntity add(NVEntity nve) {
        return value.put(new ReferenceIDKey(nve), nve);
    }

    public synchronized NVEntity remove(NVEntity nve) {
        return value.remove(new ReferenceIDKey(nve));
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

    @Override
    public List<NVEntity> search(String... criteria) {
        return SharedUtil.search(values(), criteria);
    }

    /**
     * @see org.zoxweb.shared.util.ArrayValues#isFixed()
     */
    @Override
    public boolean isFixed() {
        return false;
    }

    /**
     * @see org.zoxweb.shared.util.ArrayValues#setFixed(boolean)
     */
    @Override
    public void setFixed(boolean isFixed) {

    }

    @Override
    public NVEntity remove(GetName getName) {
        // TODO Auto-generated method stub
        throw new IllegalArgumentException("Method not supported.");
    }

}