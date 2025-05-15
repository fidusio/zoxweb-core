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

import java.util.List;

/**
 *
 */
@SuppressWarnings("serial")
public class NVGetNameValueList
        extends NVBase<List<GetNameValue<String>>>
        implements ArrayValues<GetNameValue<String>> {


    public NVGetNameValueList() {

    }

    public NVGetNameValueList(String name, List<GetNameValue<String>> list) {
        super(name, list);
    }

    public GetNameValue<String> get(GetName getName) {
        if (getName != null && getName.getName() != null) {
            return get(getName.getName());
        }

        return null;
    }


    /**
     * @see org.zoxweb.shared.util.ArrayValues#get(java.lang.String)
     */
    @Override
    public GetNameValue<String> get(String name) {
        return SharedUtil.lookup(value, name);
    }

    /**
     * @see org.zoxweb.shared.util.ArrayValues#size()
     */
    @Override
    public int size() {
        return value.size();
    }

    /**
     * @see org.zoxweb.shared.util.ArrayValues#values()
     */
    @SuppressWarnings("unchecked")
    @Override
    public GetNameValue<String>[] values() {
        return value.toArray(new GetNameValue[0]);
    }

    public <V> V[] valuesAs(V[] t) {
        return value.toArray(t);
    }

    /**
     * @see org.zoxweb.shared.util.ArrayValues#add(java.lang.Object)
     */
    @Override
    public synchronized GetNameValue<String> add(GetNameValue<String> v) {
        value.add(v);
        return v;
    }

    public synchronized GetNameValue<String> remove(String name) {
        if (name != null) {
            for (int i = 0; i < value.size(); i++) {
                GetNameValue<String> gnv = value.get(i);
                if (gnv != null && SharedStringUtil.equals(gnv.getName(), name, true)) {
                    value.remove(i);
                    return gnv;
                }
            }
        }

        return null;
    }


    public synchronized GetNameValue<String> remove(GetName name) {
        return remove(name.getName());
    }

    /**
     * @see org.zoxweb.shared.util.ArrayValues#remove(java.lang.Object)
     */
    @Override
    public synchronized GetNameValue<String> remove(GetNameValue<String> v) {
        return value.remove(v) ? v : null;
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
    public void add(GetNameValue<String>[] vals, boolean clear) {

        if (clear) {
            clear();
        }

        if (vals != null) {
            for (GetNameValue<String> gnv : vals) {
                add(gnv);
            }
        }

    }

    /**
     * @see org.zoxweb.shared.util.ArrayValues#search(java.lang.String[])
     */
    @Override
    public List<GetNameValue<String>> search(String... criteria) {
        return SharedUtil.search(values(), criteria[0]);
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

}