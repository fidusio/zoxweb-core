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

import java.io.Serializable;

/**
 * A name-based key wrapper around a {@link GetName} with configurable case
 * sensitivity. It overrides {@link #equals(Object)} and {@link #hashCode()} so
 * it can be used as a map or set key that matches against strings, other
 * {@link GetName} instances, or other keys, honoring the case-insensitive flag.
 */
@SuppressWarnings("serial")
public class GetNameKey
        implements Serializable, GetName, SetCaseSensitive {

    private GetName getName;
    private boolean caseInsensitive;

    /**
     * The default constructor; the name holder must be set via
     * {@link #setGetName(GetName)} before use.
     */
    public GetNameKey() {

    }

    /**
     * Creates a key backed by the given name holder.
     * @param gn the name holder, neither it nor its name can be null
     * @param caseInsensitive true to match names ignoring case
     */
    public GetNameKey(GetName gn, boolean caseInsensitive) {
        setGetName(gn);
        setCaseInsensitive(caseInsensitive);
    }

    /**
     * Creates a key for the given name.
     * @param name the key name, can't be null
     * @param caseInsensitive true to match names ignoring case
     */
    public GetNameKey(String name, boolean caseInsensitive) {
        setGetName(new SetNamePortable(name));
        setCaseInsensitive(caseInsensitive);
    }

    /**
     * Compares by name with a String, a GetName, or any object equal to the
     * wrapped name holder, ignoring case if the key is case insensitive.
     * @param o object to compare to
     * @return true if o matches this key's name
     */
    public boolean equals(Object o) {
        if (o != null && getName() != null) {
            if (o == this || o.equals(getName)) {
                return true;
            }

            if (o instanceof String) {
                if (caseInsensitive) {
                    return getName().equalsIgnoreCase((String) o);
                } else {
                    return getName().equals((String) o);
                }
            }

            if (o instanceof GetName) {
                if (caseInsensitive) {
                    return getName().equalsIgnoreCase(((GetName) o).getName());
                } else {
                    return getName().equals(((GetName) o).getName());
                }
            }

            return o.equals(getName());
        }

        return false;
    }

    /**
     * Returns the name's hash code, lower-cased first if the key is case insensitive.
     * @return the hash code of the name
     */
    public int hashCode() {
        if (caseInsensitive) {
            return getName().toLowerCase().hashCode();
        }

        return getName().hashCode();
    }

    /**
     * Returns the wrapped name holder.
     * @return the underlying GetName
     */
    public GetName getGetName() {
        return getName;
    }

    /**
     * Sets the wrapped name holder.
     * @param getName the name holder, neither it nor its name can be null
     * @throws NullPointerException if getName or its name is null
     */
    public void setGetName(GetName getName) {
        if (getName == null || getName.getName() == null) {
            throw new NullPointerException("name " + getName + (getName != null ? getName.getName() : ""));
        }

        this.getName = getName;
    }

    /**
     * Checks if name matching ignores character case.
     * @return true if matching is case insensitive
     */
    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    /**
     * Sets the case sensitivity of name matching.
     * @param caseInsensitive true to match names ignoring case
     */
    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    /**
     * @see org.zoxweb.shared.util.GetName#getName()
     */
    @Override
    public String getName() {
        return getName != null ? getName.getName() : null;
    }

    /**
     * Returns the key name.
     */
    public String toString() {
        return getName();
    }

}