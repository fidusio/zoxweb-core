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
package org.zoxweb.shared.db;

import org.zoxweb.shared.util.GetName;
import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.SUS;
import org.zoxweb.shared.util.SetName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Set-membership query criterion: matches when the named attribute's value is (or, when
 * {@link #isNot() negated}, is not) one of the given values — SQL {@code IN (...)} /
 * {@code NOT IN (...)}, MongoDB {@code $in} / {@code $nin}. Replaces chained
 * {@code = OR = OR =} sequences, which additionally require {@link QueryGroup grouping}
 * the moment they are combined with an AND condition.
 *
 * @param <V> type of the values to match
 */
@SuppressWarnings("serial")
public class QueryMatchIn<V>
        implements SetName, QueryMarker {

    private String name;
    private List<V> values = new ArrayList<V>();
    private boolean not;

    /**
     * The default constructor (serialization).
     */
    public QueryMatchIn() {
    }

    @SafeVarargs
    public QueryMatchIn(String name, V... values) {
        this(false, name, values);
    }

    @SafeVarargs
    public QueryMatchIn(boolean not, String name, V... values) {
        setName(name);
        setNot(not);
        if (values != null) {
            this.values.addAll(Arrays.asList(values));
        }
    }

    public QueryMatchIn(String name, List<V> values) {
        setName(name);
        setValues(values);
    }

    @SafeVarargs
    public QueryMatchIn(GetName name, V... values) {
        this(false, name.getName(), values);
    }

    @SafeVarargs
    public QueryMatchIn(GetNVConfig gnv, V... values) {
        this(false, gnv.getNVConfig().getName(), values);
    }

    /**
     * Returns the attribute name.
     * @return name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the attribute name.
     * @param name
     */
    @Override
    public void setName(String name) {
        if (SUS.isEmpty(name)) {
            throw new NullPointerException("Name is null.");
        }

        this.name = name;
    }

    /**
     * Returns the values to match against.
     * @return values
     */
    public List<V> getValues() {
        return values;
    }

    /**
     * Sets the values to match against.
     * @param values
     */
    public void setValues(List<V> values) {
        this.values = values != null ? values : new ArrayList<V>();
    }

    /**
     * Returns true when the criterion is negated ({@code NOT IN}).
     * @return true if negated
     */
    public boolean isNot() {
        return not;
    }

    /**
     * Sets the negation flag ({@code NOT IN}).
     * @param not
     */
    public void setNot(boolean not) {
        this.not = not;
    }

    @Override
    public String toString() {
        return getName() + (not ? ":NOT_IN:" : ":IN:") + values;
    }
}
