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

import org.zoxweb.shared.filters.FilterType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * NVGenericMap is a data generic container the jack of all trades.
 */
@SuppressWarnings("serial")
public class NVGenericMap
        extends NVBase<Map<GetName, GetNameValue<?>>>
        implements ArrayValues<GetNameValue<?>> {
    public NVGenericMap() {
        this(null, new LinkedHashMap<GetName, GetNameValue<?>>());
    }

    public NVGenericMap(String name) {
        this(name, new LinkedHashMap<GetName, GetNameValue<?>>());
    }

    public NVGenericMap(String name, Map<GetName, GetNameValue<?>> map) {
        super(name, map);
    }


    public GetNameValue<?> get(GetName getName) {
        if (getName != null && getName.getName() != null) {
            return get(getName.getName());
        }

        return null;
    }


    public GetNameValue<?> get(String name) {
        return value.get(new GetNameKey(name, true));
    }

    public <GNV extends GetNameValue<?>> GNV getNV(String name) {
        return (GNV) get(name);
    }

    public <GNV extends GetNameValue<?>> GNV getNV(GetName getName) {
        return (GNV) get(getName);
    }


    public <V> V getValue(GetName name) {
        return getValue(name.getName(), null);
    }

    public <V> V getValue(String name) {
        return getValue(name, null);
    }


    /**
     * This method can throw exception ClassCastException or number formating exception
     *
     * @param name of hte parameter
     * @return long value
     * @throws RuntimeException it could NullPointer ClassCast ...
     */
    public long getValueAsLong(GetName name) {
        return getValueAsLong(name.getName());
    }

    /**
     * This method can throw exception ClassCastException or number formating exception
     *
     * @param name of hte parameter
     * @return long value
     * @throws RuntimeException it could NullPointer ClassCast ...
     */
    public long getValueAsLong(String name)
            throws RuntimeException {
        Object value = getValue(name);
        if (value instanceof Number)
            return ((Number)value).longValue();

        return Long.parseLong((String) value);
    }

    @SuppressWarnings("unchecked")
    public <V> V getValue(String name, V defaultValue) {
        GetNameValue<?> ret = get(name);

        if (ret != null) {
            return (V) ret.getValue();
        }

        return defaultValue;
    }

    public <V> V getValue(GetName name, V defaultValue) {
        GetNameValue<?> ret = get(name);

        if (ret != null) {
            return (V) ret.getValue();
        }

        return defaultValue;
    }

    /**
     * @see org.zoxweb.shared.util.ArrayValues#size()
     */
    @Override
    public int size() {
        return value.size();
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    /**
     * @see org.zoxweb.shared.util.ArrayValues#values()
     */
    @Override
    public GetNameValue<?>[] values() {
        return getValue().values().toArray(new GetNameValue[0]);
    }

    public <V> V[] valuesAs(V[] t) {
        return getValue().values().toArray(t);
    }


    public Object[] nvValues() {
        GetNameValue<?>[] allGNV = values();
        Object[] ret = new Object[allGNV.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = allGNV[i].getValue();
        }
        return ret;
    }

    /**
     * @param v to be added if null will not be added
     * @return v
     */
    @Override
    public synchronized GetNameValue<?> add(GetNameValue<?> v) {
        if (v != null)
            value.put(new GetNameKey(v, true), v);
        return v;
    }


    public String[] getAllNames() {
        GetNameValue<?>[] allGNV = values();
        String[] ret = new String[allGNV.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = allGNV[i].getName();
        }
        return ret;
    }

    public GetNameValue<?> add(String name, String value, FilterType ft) {
        NVPair nvp = new NVPair(name, value, ft);
        return add(nvp);
    }

    public GetNameValue<?> add(String name, String value) {
        return add(name, value, null);
    }

    public GetNameValue<?> add(GetName name, String value) {
        return add(name.getName(), value, null);
    }


    public synchronized GetNameValue<?> add(NVEntity nve) {
        return add(new NVEntityReference(nve));
    }


    public synchronized GetNameValue<?> add(String name, NVEntity nve) {
        return add(new NVEntityReference(name, nve));
    }


    public synchronized GetNameValue<?> remove(String name) {
        return value.remove(new GetNameKey(name, true));
    }


    public synchronized GetNameValue<?> remove(GetName name) {
        return value.remove(new GetNameKey(name, true));
    }


    @Override
    public synchronized GetNameValue<?> remove(GetNameValue<?> v) {
        return value.remove(new GetNameKey(v, true));
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
    public void add(GetNameValue<?>[] vals, boolean clear) {
        if (clear) {
            clear();
        }

        if (vals != null) {
            for (GetNameValue<?> gnv : vals) {
                add(gnv);
            }
        }

    }

    /**
     * @see org.zoxweb.shared.util.ArrayValues#search(java.lang.String[])
     */
    @Override
    public List<GetNameValue<?>> search(String... criteria) {
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


    public ArrayValues<GetNameValue<String>> asArrayValuesString() {
        return (ArrayValues<GetNameValue<String>>) ((Object) this);
    }


    public static NVGenericMap copy(NVGenericMap from, boolean deep) {
        return copy(from, new NVGenericMap(from.getName()), deep);
    }

    public static NVGenericMap copy(NVGenericMap from, NVGenericMap to, boolean deep) {
        for (GetNameValue<?> gnv : from.values()) {
            if (deep) {
                if (gnv instanceof NVPair) {
                    to.add(gnv.getName(), (String) gnv.getValue());
                } else if (gnv instanceof NVBoolean) {
                    to.add(new NVBoolean(gnv.getName(), ((NVBoolean) gnv).getValue()));
                } else if (gnv instanceof NVInt) {
                    to.add(new NVInt(gnv.getName(), ((NVInt) gnv).getValue()));
                } else if (gnv instanceof NVLong) {
                    to.add(new NVLong(gnv.getName(), ((NVLong) gnv).getValue()));
                } else if (gnv instanceof NVFloat) {
                    to.add(new NVFloat(gnv.getName(), ((NVFloat) gnv).getValue()));
                } else if (gnv instanceof NVDouble) {
                    to.add(new NVDouble(gnv.getName(), ((NVDouble) gnv).getValue()));
                }
                // TO DO must add the rest
                else if (gnv instanceof NVGenericMap) {
                    to.add(copy((NVGenericMap) gnv, new NVGenericMap(gnv.getName()), deep));
                } else
                    to.add(gnv);
            } else
                to.add(gnv);
        }

        return to;
    }


    public <V> V lookupValue(GetName gn) {
        if (gn != null)
            return lookupValue(gn.getName());
        return null;
    }

    public <V extends GetNameValue<?>> V lookup(GetName gn) {
        if (gn != null)
            return lookup(gn.getName());
        return null;
    }

    public <V> V lookupValue(String fullyQualifiedName) {
        GetNameValue<V> ret = lookup(fullyQualifiedName);
        if (ret != null)
            return ret.getValue();
        return null;
    }


    public <V extends GetNameValue<?>> V lookup(String fullyQualifiedName) {
        GetNameValue<?> ret = get(fullyQualifiedName);

        if (ret == null && fullyQualifiedName.indexOf('.') != -1) {
            String[] subNames = fullyQualifiedName.split("\\.");
            if (subNames.length > 1) {
                ret = this;
                for (int i = 0; i < subNames.length; i++) {
                    if (ret instanceof NVGenericMap)
                        ret = ((NVGenericMap) ret).get(subNames[i]);
                    else if (ret instanceof NVEntity)
                        ret = (((NVEntity) ret).lookup(subNames[i]));
                    else if (ret instanceof NVPairGetNameMap)
                        ret = ((NVPairGetNameMap) ret).get(subNames[i]);
                    else
                        ret = null;

                    if (ret == null)
                        break;
                }
            }
        }

        return (V) ret;
    }


    public <V extends GetNameValue<?>> V lookupContainer(String fullyQualifiedName) {
        GetNameValue<?> ret = get(fullyQualifiedName);
        if (ret != null) {
            return (V) this;
        } else if (fullyQualifiedName.indexOf('.') != -1) {
            String[] subNames = fullyQualifiedName.split("\\.");
            if (subNames.length > 1) {
                ret = this;
                for (int i = 0; i < (subNames.length - 1); i++) {

                    if (ret instanceof NVGenericMap)
                        ret = ((NVGenericMap) ret).get(subNames[i]);
                    else if (ret instanceof NVEntity)
                        ret = (((NVEntity) ret).lookup(subNames[i]));
                    else if (ret instanceof NVPairGetNameMap)
                        ret = ((NVPairGetNameMap) ret).get(subNames[i]);
                    else
                        ret = null;

                    if (ret == null)
                        break;
                }
            }
        }

        return (V) ret;
    }


    public NVGenericMap build(GetNameValue<?> gnv) {
        add(gnv);
        return this;
    }

    public NVGenericMap build(String name, String value, FilterType ft) {
        add(name, value, ft);
        return this;
    }

    public NVGenericMap build(String name, String value) {
        add(name, value);
        return this;
    }

    public NVGenericMap build(NVEntity entity) {
        add(entity);
        return this;
    }

    public NVGenericMap build(GetName name, String value) {
        add(name, value);
        return this;
    }


    public static NVGenericMap merge(NVGenericMap from, NVGenericMap to) {
        for (GetNameValue<?> gnv : from.values()) {
            to.add(gnv);
        }
        return to;
    }


    public NVGenericMap lookupSubNVMG(GetName name, boolean autoAdd) {
        return lookupSubNVMG(name.getName(), autoAdd);
    }


    public synchronized NVGenericMap lookupSubNVMG(String name, boolean autoAdd) {
        GetNameValue<?> subNVGM = lookup(name);
        if (subNVGM == null && autoAdd) {
            subNVGM = new NVGenericMap(name);
            build(subNVGM);
        }
        if (!(subNVGM instanceof NVGenericMap)) {
            throw new IllegalArgumentException("Result not an NMGenericMap or null");
        }
        return (NVGenericMap) subNVGM;
    }


    /**
     * Performs a deep comparison of two NVGenericMap instances.
     *
     * @param one the first NVGenericMap to compare
     * @param two the second NVGenericMap to compare
     * @return true if both maps have equal content (deep comparison), false otherwise
     */
    public static boolean areEquals(NVGenericMap one, NVGenericMap two) {
        // Handle null cases
        if (one == two) {
            return true;
        }
        if (one == null || two == null) {
            return false;
        }

        // Compare sizes
        if (one.size() != two.size()) {
            return false;
        }

        // Compare each entry
        for (GetNameValue<?> gnvOne : one.values()) {
            GetNameValue<?> gnvTwo = two.get(gnvOne.getName());
            if (!areValuesEqual(gnvOne, gnvTwo)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compares two GetNameValue instances for deep equality.
     *
     * @param one the first value
     * @param two the second value
     * @return true if both values are deeply equal
     */
    private static boolean areValuesEqual(GetNameValue<?> one, GetNameValue<?> two) {
        if (one == two) {
            return true;
        }
        if (one == null || two == null) {
            return false;
        }

        // Names must match
        if (!Objects.equals(one.getName(), two.getName())) {
            return false;
        }

        // Must be same type
        if (one.getClass() != two.getClass()) {
            return false;
        }

        // Handle NVGenericMap recursively
        if (one instanceof NVGenericMap) {
            return areEquals((NVGenericMap) one, (NVGenericMap) two);
        }

        // Handle NVBlob (byte array comparison)
        if (one instanceof NVBlob) {
            byte[] bytesOne = ((NVBlob) one).getValue();
            byte[] bytesTwo = ((NVBlob) two).getValue();
            if (bytesOne == bytesTwo) {
                return true;
            }
            if (bytesOne == null || bytesTwo == null) {
                return false;
            }
            if (bytesOne.length != bytesTwo.length) {
                return false;
            }
            for (int i = 0; i < bytesOne.length; i++) {
                if (bytesOne[i] != bytesTwo[i]) {
                    return false;
                }
            }
            return true;
        }

        // Handle NVGenericMapList
        if (one instanceof NVGenericMapList) {
            List<NVGenericMap> listOne = ((NVGenericMapList) one).getValue();
            List<NVGenericMap> listTwo = ((NVGenericMapList) two).getValue();
            if (listOne == listTwo) {
                return true;
            }
            if (listOne == null || listTwo == null) {
                return false;
            }
            if (listOne.size() != listTwo.size()) {
                return false;
            }
            for (int i = 0; i < listOne.size(); i++) {
                if (!areEquals(listOne.get(i), listTwo.get(i))) {
                    return false;
                }
            }
            return true;
        }

        // Handle NVEntityReference
        if (one instanceof NVEntityReference) {
            NVEntity nveOne = ((NVEntityReference) one).getValue();
            NVEntity nveTwo = ((NVEntityReference) two).getValue();
            return areNVEntitiesEqual(nveOne, nveTwo);
        }

        // Handle ArrayValues (lists like NVPairList, NVStringList, NVLongList, etc.)
        if (one instanceof ArrayValues) {
            Object[] valsOne = ((ArrayValues<?>) one).values();
            Object[] valsTwo = ((ArrayValues<?>) two).values();
            if (valsOne == valsTwo) {
                return true;
            }
            if (valsOne == null || valsTwo == null) {
                return false;
            }
            if (valsOne.length != valsTwo.length) {
                return false;
            }
            for (int i = 0; i < valsOne.length; i++) {
                if (valsOne[i] instanceof GetNameValue && valsTwo[i] instanceof GetNameValue) {
                    if (!areValuesEqual((GetNameValue<?>) valsOne[i], (GetNameValue<?>) valsTwo[i])) {
                        return false;
                    }
                } else if (!Objects.equals(valsOne[i], valsTwo[i])) {
                    return false;
                }
            }
            return true;
        }

        // Handle primitive NV types (NVPair, NVLong, NVInt, NVFloat, NVDouble, NVBoolean, NVBigDecimal, NVEnum)
        Object valOne = one.getValue();
        Object valTwo = two.getValue();

        return Objects.equals(valOne, valTwo);
    }

    /**
     * Compares two NVEntity instances for deep equality.
     *
     * @param one the first NVEntity
     * @param two the second NVEntity
     * @return true if both entities are deeply equal
     */
    private static boolean areNVEntitiesEqual(NVEntity one, NVEntity two) {
        if (one == two) {
            return true;
        }
        if (one == null || two == null) {
            return false;
        }

        // Must be same type
        if (one.getClass() != two.getClass()) {
            return false;
        }

        // Compare all attributes
        if (one.getAttributes().size() != two.getAttributes().size()) {
            return false;
        }

        for (NVBase<?> attrOne : one.getAttributes().values()) {
            NVBase<?> attrTwo = two.lookup(attrOne.getName());
            if (attrTwo == null) {
                return false;
            }

            // Handle nested NVEntity
            if (attrOne instanceof NVEntityReference) {
                if (!(attrTwo instanceof NVEntityReference)) {
                    return false;
                }
                if (!areNVEntitiesEqual(((NVEntityReference) attrOne).getValue(),
                        ((NVEntityReference) attrTwo).getValue())) {
                    return false;
                }
            }
            // Handle NVGenericMap within NVEntity
            else if (attrOne instanceof NVGenericMap) {
                if (!(attrTwo instanceof NVGenericMap)) {
                    return false;
                }
                if (!areEquals((NVGenericMap) attrOne, (NVGenericMap) attrTwo)) {
                    return false;
                }
            }
            // Handle NVBlob
            else if (attrOne instanceof NVBlob) {
                if (!(attrTwo instanceof NVBlob)) {
                    return false;
                }
                byte[] bytesOne = ((NVBlob) attrOne).getValue();
                byte[] bytesTwo = ((NVBlob) attrTwo).getValue();
                if (bytesOne == null && bytesTwo == null) {
                    continue;
                }
                if (bytesOne == null || bytesTwo == null || bytesOne.length != bytesTwo.length) {
                    return false;
                }
                for (int i = 0; i < bytesOne.length; i++) {
                    if (bytesOne[i] != bytesTwo[i]) {
                        return false;
                    }
                }
            }
            // Handle other values
            else if (!Objects.equals(attrOne.getValue(), attrTwo.getValue())) {
                return false;
            }
        }

        return true;
    }


}