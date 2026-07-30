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
 * A general-purpose, heterogeneous name-value container — the jack of all trades of the
 * NV meta-model.
 * <p>
 * An NVGenericMap holds any mix of {@link GetNameValue} entries ({@link NVPair}, {@link NVInt},
 * {@link NVLong}, {@link NVBoolean}, {@link NVBlob}, {@link NVEntityReference}, nested
 * NVGenericMap instances, ...) keyed by their name. Names are matched
 * <b>case-insensitively</b>, and entries preserve <b>insertion order</b> (backed by a
 * {@link LinkedHashMap}).
 * <p>
 * Main capabilities:
 * <ul>
 *   <li>Typed value access with optional defaults: {@link #getValue(String, Object)},
 *       {@link #getValueAsLong(String, long)}, {@link #getValueAsBoolean(String, boolean)}.</li>
 *   <li>Dotted-path deep lookup across nested containers and entities:
 *       {@link #lookup(String)}, {@link #lookupValue(String)}, {@link #lookupContainer(String)}.</li>
 *   <li>Fluent population via the {@code build(...)} methods.</li>
 *   <li>Static utilities: {@link #copy(NVGenericMap, boolean)}, {@link #merge(NVGenericMap, NVGenericMap)}
 *       and deep comparison via {@link #areEquals(NVGenericMap, NVGenericMap)}.</li>
 * </ul>
 * Mutating methods ({@code add}/{@code remove}) are synchronized, but iteration over
 * {@link #values()} operates on a snapshot array and is not otherwise guarded.
 * <p>
 * Being itself an {@code NVBase}, an NVGenericMap can be stored as an attribute of an
 * {@link NVEntity} or nested inside another NVGenericMap, which is what enables the
 * dotted-path lookups.
 */
@SuppressWarnings("serial")
public class NVGenericMap
        extends NVBase<Map<GetName, GetNameValue<?>>>
        implements ArrayValues<GetNameValue<?>> {

    /**
     * Creates an unnamed, empty map.
     */
    public NVGenericMap() {
        this(null, new LinkedHashMap<GetName, GetNameValue<?>>());
    }

    /**
     * Creates an empty map with the given name.
     *
     * @param name the name of the map itself (used when nested inside another container)
     */
    public NVGenericMap(String name) {
        this(name, new LinkedHashMap<GetName, GetNameValue<?>>());
    }

    /**
     * Creates an empty map named after the given {@link GetName}.
     *
     * @param name the name provider, may be null for an unnamed map
     */
    public NVGenericMap(GetName name) {
        this(name != null ? name.getName() : null, new LinkedHashMap<GetName, GetNameValue<?>>());
    }

    /**
     * Creates a map with the given name and backing map.
     *
     * @param name the name of the map itself
     * @param map  the backing map instance; must use {@link GetNameKey}-compatible keys
     */
    public NVGenericMap(String name, Map<GetName, GetNameValue<?>> map) {
        super(name, map);
    }


    /**
     * Returns the entry whose name matches the given {@link GetName}.
     *
     * @param getName the name provider
     * @return the matching entry, or null if not found or getName/its name is null
     */
    public GetNameValue<?> get(GetName getName) {
        if (getName != null && getName.getName() != null) {
            return get(getName.getName());
        }

        return null;
    }


    /**
     * Returns the entry with the given name (case-insensitive match).
     *
     * @param name the entry name
     * @return the matching entry, or null if not found
     */
    public GetNameValue<?> get(String name) {
        return value.get(new GetNameKey(name, true));
    }

    /**
     * Returns the entry with the given name, cast to the expected {@link GetNameValue} subtype.
     *
     * @param name the entry name
     * @param <GNV> the expected entry type (e.g. {@code NVInt}, {@code NVGenericMap})
     * @return the matching entry, or null if not found
     * @throws ClassCastException if the entry is not of the expected type
     */
    public <GNV extends GetNameValue<?>> GNV getNV(String name) {
        return (GNV) get(name);
    }

    /**
     * Returns the entry named by the given {@link GetName}, cast to the expected subtype.
     *
     * @param getName the name provider
     * @param <GNV> the expected entry type
     * @return the matching entry, or null if not found
     * @throws ClassCastException if the entry is not of the expected type
     */
    public <GNV extends GetNameValue<?>> GNV getNV(GetName getName) {
        return (GNV) get(getName);
    }


    /**
     * Returns the value of the entry named by the given {@link GetName}.
     *
     * @param name the name provider
     * @param <V> the expected value type
     * @return the entry's value, or null if the entry does not exist
     */
    public <V> V getValue(GetName name) {
        return getValue(name.getName(), null);
    }

    /**
     * Returns the value of the entry with the given name.
     *
     * @param name the entry name
     * @param <V> the expected value type
     * @return the entry's value, or null if the entry does not exist
     */
    public <V> V getValue(String name) {
        return getValue(name, null);
    }


    /**
     * Returns the value of the named entry as a long, converting from any {@link Number}
     * or numeric {@link String}.
     *
     * @param name the name of the parameter
     * @return the long value
     * @throws RuntimeException NullPointerException if the entry is missing,
     *         ClassCastException/NumberFormatException if the value is not numeric
     */
    public long getValueAsLong(GetName name) {
        return getValueAsLong(name.getName());
    }

    /**
     * Returns the value of the named entry as a long, converting from any {@link Number}
     * or numeric {@link String}.
     *
     * @param name the name of the parameter
     * @return the long value
     * @throws RuntimeException NullPointerException if the entry is missing,
     *         ClassCastException/NumberFormatException if the value is not numeric
     */
    public long getValueAsLong(String name) {
        Object value = getValue(name);
        if (value instanceof Number)
            return ((Number) value).longValue();

        return Long.parseLong((String) value);
    }

    /**
     * Returns the value of the named entry as a long, or the default if the entry is missing.
     *
     * @param name the name of the parameter
     * @param defaultValue returned when the entry does not exist
     * @return the long value or defaultValue
     * @throws RuntimeException ClassCastException/NumberFormatException if the value exists
     *         but is not numeric
     */
    public long getValueAsLong(GetName name, long defaultValue)
            throws RuntimeException {
        return getValueAsLong(name.getName(), defaultValue);
    }



    /**
     * Returns the value of the named entry as a boolean. {@link Boolean} values are returned
     * directly; {@link String} values are interpreted via {@link Const.Bool#lookupValue(String)}
     * (e.g. "true", "yes", "on"); anything else yields the default.
     *
     * @param name the name of the parameter
     * @param defaultValue returned when the entry is missing or not interpretable as a boolean
     * @return the boolean value or defaultValue
     */
    public boolean getValueAsBoolean(GetName name, boolean defaultValue) {
        Object value = getValue(name);
        if (value instanceof Boolean)
            return ((Boolean) value).booleanValue();
        if(value instanceof String)
            return Const.Bool.lookupValue((String)value);
        return defaultValue;
    }

    /**
     * Returns the value of the named entry as a boolean. {@link Boolean} values are returned
     * directly; {@link String} values are interpreted via {@link Const.Bool#lookupValue(String)}
     * (e.g. "true", "yes", "on"); anything else yields the default.
     *
     * @param name the name of the parameter
     * @param defaultValue returned when the entry is missing or not interpretable as a boolean
     * @return the boolean value or defaultValue
     */
    public boolean getValueAsBoolean(String name, boolean defaultValue) {
        Object value = getValue(name);
        if (value instanceof Boolean)
            return ((Boolean) value).booleanValue();
        if(value instanceof String)
            return Const.Bool.lookupValue((String)value);
        return defaultValue;
    }

    /**
     * Returns the value of the named entry transformed through the given decoder.
     *
     * @param getName the name provider of the entry
     * @param decoder the decoder applied to the raw value
     * @param <V> the decoded result type
     * @return the decoded value
     */
    public <V> V decodedValue(GetName getName, DataDecoder<?, ?> decoder) {
        return (V)decoder.decode(getValue(getName));
    }

    /**
     * Returns the value of the named entry transformed through the given decoder.
     *
     * @param name the name of the entry
     * @param decoder the decoder applied to the raw value
     * @param <V> the decoded result type
     * @return the decoded value
     */
    public <V> V decodedValue(String name, DataDecoder<?, ?> decoder) {
        return (V)decoder.decode(getValue(name));
    }

    /**
     * Returns the value of the named entry as a long, or the default if the entry is missing.
     *
     * @param name the name of the parameter
     * @param defaultValue returned when the entry does not exist
     * @return the long value or defaultValue
     * @throws RuntimeException ClassCastException/NumberFormatException if the value exists
     *         but is not numeric
     */
    public long getValueAsLong(String name, long defaultValue)
            throws RuntimeException {
        Object value = getValue(name);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number)
            return ((Number) value).longValue();

        return Long.parseLong((String) value);
    }

    /**
     * Returns the value of the entry with the given name, or the default if the entry
     * does not exist.
     *
     * @param name the entry name
     * @param defaultValue returned when the entry does not exist
     * @param <V> the expected value type
     * @return the entry's value or defaultValue
     */
    @SuppressWarnings("unchecked")
    public <V> V getValue(String name, V defaultValue) {
        GetNameValue<?> ret = get(name);

        if (ret != null) {
            return (V) ret.getValue();
        }

        return defaultValue;
    }

    /**
     * Returns the value of the entry named by the given {@link GetName}, or the default
     * if the entry does not exist.
     *
     * @param name the name provider
     * @param defaultValue returned when the entry does not exist
     * @param <V> the expected value type
     * @return the entry's value or defaultValue
     */
    public <V> V getValue(GetName name, V defaultValue) {
        GetNameValue<?> ret = get(name);

        if (ret != null) {
            return (V) ret.getValue();
        }

        return defaultValue;
    }

    /**
     * Returns the number of entries in the map.
     *
     * @see org.zoxweb.shared.util.ArrayValues#size()
     */
    @Override
    public int size() {
        return value.size();
    }

    /**
     * Checks if the map has no entries.
     *
     * @return true if the map contains no entries
     */
    public boolean isEmpty() {
        return value.isEmpty();
    }

    /**
     * Returns all entries as an array, in insertion order.
     *
     * @see org.zoxweb.shared.util.ArrayValues#values()
     */
    @Override
    public GetNameValue<?>[] values() {
        return getValue().values().toArray(new GetNameValue[0]);
    }

    /**
     * Returns all entries in an array of the caller-supplied type, in insertion order.
     *
     * @param t the destination array (grown if too small)
     * @param <V> the array component type
     * @return the entries as an array of V
     */
    public <V> V[] valuesAs(V[] t) {
        return getValue().values().toArray(t);
    }


    /**
     * Returns the raw values (not the {@link GetNameValue} wrappers) of all entries,
     * in insertion order.
     *
     * @return array of each entry's value
     */
    public Object[] nvValues() {
        GetNameValue<?>[] allGNV = values();
        Object[] ret = new Object[allGNV.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = allGNV[i].getValue();
        }
        return ret;
    }

    /**
     * Adds an entry, keyed by its name (case-insensitive). An existing entry with the
     * same name is replaced.
     *
     * @param v to be added if null will not be added
     * @return v
     */
    @Override
    public synchronized GetNameValue<?> add(GetNameValue<?> v) {
        if (v != null)
            value.put(new GetNameKey(v, true), v);
        return v;
    }


    /**
     * Returns the names of all entries, in insertion order.
     *
     * @return array of entry names
     */
    public String[] getAllNames() {
        GetNameValue<?>[] allGNV = values();
        String[] ret = new String[allGNV.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = allGNV[i].getName();
        }
        return ret;
    }

    /**
     * Adds a string entry as an {@link NVPair} with an optional value filter.
     *
     * @param name the entry name
     * @param value the string value
     * @param ft the filter/validation type, may be null
     * @return the added NVPair
     */
    public GetNameValue<?> add(String name, String value, FilterType ft) {
        NVPair nvp = new NVPair(name, value, ft);
        return add(nvp);
    }

    /**
     * Adds a string entry as an {@link NVPair}.
     *
     * @param name the entry name
     * @param value the string value
     * @return the added NVPair
     */
    public GetNameValue<?> add(String name, String value) {
        return add(name, value, null);
    }

    /**
     * Adds a string entry as an {@link NVPair} named by the given {@link GetName}.
     *
     * @param name the name provider
     * @param value the string value
     * @return the added NVPair
     */
    public GetNameValue<?> add(GetName name, String value) {
        return add(name.getName(), value, null);
    }


    /**
     * Adds an {@link NVEntity} wrapped in an {@link NVEntityReference}, keyed by the
     * entity's own name.
     *
     * @param nve the entity to add
     * @return the added NVEntityReference
     */
    public synchronized GetNameValue<?> add(NVEntity nve) {
        return add(new NVEntityReference(nve));
    }


    /**
     * Adds an {@link NVEntity} wrapped in an {@link NVEntityReference} under the given name.
     *
     * @param name the entry name
     * @param nve the entity to add
     * @return the added NVEntityReference
     */
    public synchronized GetNameValue<?> add(String name, NVEntity nve) {
        return add(new NVEntityReference(name, nve));
    }


    /**
     * Removes the entry with the given name (case-insensitive match).
     *
     * @param name the entry name
     * @return the removed entry, or null if none matched
     */
    public synchronized GetNameValue<?> remove(String name) {
        return value.remove(new GetNameKey(name, true));
    }


    /**
     * Removes the entry named by the given {@link GetName}.
     *
     * @param name the name provider
     * @return the removed entry, or null if none matched
     */
    public synchronized GetNameValue<?> remove(GetName name) {
        return value.remove(new GetNameKey(name, true));
    }


    /**
     * Removes the entry matching the given entry's name.
     *
     * @param v the entry whose name identifies what to remove
     * @return the removed entry, or null if none matched
     */
    @Override
    public synchronized GetNameValue<?> remove(GetNameValue<?> v) {
        return value.remove(new GetNameKey(v, true));
    }

    /**
     * Removes all entries from the map.
     *
     * @see org.zoxweb.shared.util.ArrayValues#clear()
     */
    @Override
    public void clear() {
        value.clear();
    }

    /**
     * Adds all the given entries, optionally clearing the map first.
     *
     * @param vals the entries to add; null is a no-op
     * @param clear if true the map is emptied before adding
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
     * Searches entries by name; only the first criterion is used.
     *
     * @param criteria search criteria, criteria[0] is the name matched via
     *        {@link SharedUtil#search}
     * @return the matching entries
     * @see org.zoxweb.shared.util.ArrayValues#search(java.lang.String[])
     */
    @Override
    public List<GetNameValue<?>> search(String... criteria) {
        return SharedUtil.search(values(), criteria[0]);
    }

    /**
     * Always false: an NVGenericMap is never fixed-size.
     *
     * @see org.zoxweb.shared.util.ArrayValues#isFixed()
     */
    @Override
    public boolean isFixed() {
        return false;
    }

    /**
     * No-op: an NVGenericMap cannot be made fixed-size.
     *
     * @see org.zoxweb.shared.util.ArrayValues#setFixed(boolean)
     */
    @Override
    public void setFixed(boolean isFixed) {

    }


    /**
     * Returns this map viewed as {@code ArrayValues<GetNameValue<String>>}; useful with
     * APIs that consume string name-value collections. This is an unchecked view of the
     * same instance — safe only when all entries actually hold string values.
     *
     * @return this map, cast to a string-valued ArrayValues view
     */
    public ArrayValues<GetNameValue<String>> asArrayValuesString() {
        return (ArrayValues<GetNameValue<String>>) ((Object) this);
    }


    /**
     * Copies a map into a new NVGenericMap carrying the same name.
     *
     * @param from the source map
     * @param deep if true, primitive entries and nested NVGenericMaps are duplicated;
     *        if false, the new map shares the source's entry instances
     * @return the new copy
     */
    public static NVGenericMap copy(NVGenericMap from, boolean deep) {
        return copy(from, new NVGenericMap(from.getName()), deep);
    }

    /**
     * Copies all entries of one map into another.
     * <p>
     * With {@code deep} set, known primitive types ({@link NVPair}, {@link NVBoolean},
     * {@link NVInt}, {@link NVLong}, {@link NVFloat}, {@link NVDouble}) and nested
     * NVGenericMaps are duplicated; any other entry type is added by reference.
     * Without {@code deep}, all entries are added by reference.
     *
     * @param from the source map
     * @param to the destination map (existing same-named entries are replaced)
     * @param deep whether to duplicate entries instead of sharing them
     * @return to
     */
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


    /**
     * Copies only the named entries (by reference) from one map into another; names with
     * no matching entry are skipped.
     *
     * @param from the source map
     * @param to the destination map
     * @param paramNamesToCopy the names of the entries to copy
     * @return to
     */
    public static NVGenericMap copy(NVGenericMap from, NVGenericMap to, String ... paramNamesToCopy) {
        for(String  paramName : paramNamesToCopy) {
            GetNameValue<?> gnv = from.get(paramName);
            if (gnv != null) {
                to.add(gnv);
            }
        }
        return to;
    }


    /**
     * Deep-lookup of a value by {@link GetName}; see {@link #lookupValue(String)}.
     *
     * @param gn the name provider, may be null
     * @param <V> the expected value type
     * @return the resolved value, or null if gn is null or nothing matched
     */
    public <V> V lookupValue(GetName gn) {
        if (gn != null)
            return lookupValue(gn.getName());
        return null;
    }

    /**
     * Deep-lookup of an entry by {@link GetName}; see {@link #lookup(String)}.
     *
     * @param gn the name provider, may be null
     * @param <V> the expected entry type
     * @return the resolved entry, or null if gn is null or nothing matched
     */
    public <V extends GetNameValue<?>> V lookup(GetName gn) {
        if (gn != null)
            return lookup(gn.getName());
        return null;
    }

    /**
     * Deep-lookup of a value by fully qualified name; see {@link #lookup(String)}.
     *
     * @param fullyQualifiedName plain name or dotted path (e.g. "config.db.url")
     * @param <V> the expected value type
     * @return the resolved entry's value, or null if nothing matched
     */
    public <V> V lookupValue(String fullyQualifiedName) {
        GetNameValue<V> ret = lookup(fullyQualifiedName);
        if (ret != null)
            return ret.getValue();
        return null;
    }


    /**
     * Deep-lookup of an entry by fully qualified name.
     * <p>
     * A direct (case-insensitive) match is tried first. Failing that, a name containing
     * dots is walked segment by segment through nested containers: {@code NVGenericMap},
     * {@link NVEntity} and {@link NVPairGetNameMap} levels are traversed; any other type
     * mid-path ends the walk. E.g. {@code lookup("config.db.url")} resolves the "url"
     * entry of the "db" sub-map of the "config" sub-map.
     *
     * @param fullyQualifiedName plain name or dotted path
     * @param <V> the expected entry type
     * @return the resolved entry, or null if nothing matched
     */
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


    /**
     * Resolves the container that holds the entry designated by a fully qualified name —
     * the same walk as {@link #lookup(String)} but stopping one segment short. A direct
     * match returns this map itself.
     *
     * @param fullyQualifiedName plain name or dotted path
     * @param <V> the expected container type
     * @return the container of the addressed entry, or null if the path does not resolve
     */
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


    /**
     * Fluent version of {@link #add(GetNameValue)}.
     *
     * @param gnv the entry to add
     * @return this map, for chaining
     */
    public NVGenericMap build(GetNameValue<?> gnv) {
        add(gnv);
        return this;
    }

    /**
     * Fluent version of {@link #add(String, String, FilterType)}.
     *
     * @param name the entry name
     * @param value the string value
     * @param ft the filter/validation type, may be null
     * @return this map, for chaining
     */
    public NVGenericMap build(String name, String value, FilterType ft) {
        add(name, value, ft);
        return this;
    }

    /**
     * Fluent version of {@link #add(String, String)}.
     *
     * @param name the entry name
     * @param value the string value
     * @return this map, for chaining
     */
    public NVGenericMap build(String name, String value) {
        add(name, value);
        return this;
    }

    /**
     * Fluent version of {@link #add(NVEntity)}.
     *
     * @param entity the entity to add
     * @return this map, for chaining
     */
    public NVGenericMap build(NVEntity entity) {
        add(entity);
        return this;
    }

    /**
     * Fluent version of {@link #add(GetName, String)}.
     *
     * @param name the name provider
     * @param value the string value
     * @return this map, for chaining
     */
    public NVGenericMap build(GetName name, String value) {
        add(name, value);
        return this;
    }


    /**
     * Adds all entries of one map into another by reference; same-named entries in the
     * destination are replaced.
     *
     * @param from the source map
     * @param to the destination map
     * @return to
     */
    public static NVGenericMap merge(NVGenericMap from, NVGenericMap to) {
        for (GetNameValue<?> gnv : from.values()) {
            to.add(gnv);
        }
        return to;
    }


    /**
     * Resolves a nested NVGenericMap by {@link GetName}; see {@link #lookupSubNVMG(String, boolean)}.
     *
     * @param name the name provider
     * @param autoAdd if true, a missing sub-map is created and added
     * @return the nested map
     * @throws IllegalArgumentException if the entry is absent (with autoAdd false) or not
     *         an NVGenericMap
     */
    public NVGenericMap lookupSubNVMG(GetName name, boolean autoAdd) {
        return lookupSubNVMG(name.getName(), autoAdd);
    }


    /**
     * Resolves a nested NVGenericMap by name (supports the dotted-path syntax of
     * {@link #lookup(String)}), optionally creating it on demand.
     *
     * @param name the sub-map name or dotted path
     * @param autoAdd if true, a missing sub-map is created under the given name and added
     *        to this map
     * @return the nested map
     * @throws IllegalArgumentException if the entry is absent (with autoAdd false) or
     *         exists but is not an NVGenericMap
     */
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