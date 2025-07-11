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

import java.util.Objects;

/**
 * The name value pair getter definition interface.
 *
 * @param <V>
 * @author mnael
 */
public interface GetNameValue<V>
        extends GetName, GetValue<V> {

    final class GetNameValueImpl<V>
            implements GetNameValue<V> {
        private final String name;
        private final V value;

        private GetNameValueImpl(String name, V value) {
            this.name = name;
            this.value = value;
        }

        /**
         * @return the name of the object
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * Returns the value.
         *
         * @return typed value
         */
        @Override
        public V getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof GetNameValue)) return false;
            GetNameValue<?> that = (GetNameValue<?>) o;
            return Objects.equals(name, that.getName()) && Objects.equals(value, that.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }

        @Override
        public String toString() {
            return "{" + name + ":" + value + "}";
        }
    }

    /**
     * This should only be used in memory read only Objets
     *
     * @param name  of the object
     * @param value value of object
     * @param <V>   for type conversion and ease of use
     * @return the GetNameValue encapsulation
     */
    static <V> GetNameValue<V> create(String name, V value) {
        return new GetNameValueImpl<>(name, value);
    }

    /**
     * This should only be used in memory read only Objets
     *
     * @param name  of the object
     * @param value value of object
     * @param <V>   for type conversion and ease of use
     * @return the GetNameValue encapsulation
     */
    static <V> GetNameValue<V> create(GetName name, V value) {
        return new GetNameValueImpl<>(name.getName(), value);
    }
}