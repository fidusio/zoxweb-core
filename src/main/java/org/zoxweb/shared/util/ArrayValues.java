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
 * The array values interface.
 *
 * @param <T>
 */
public interface ArrayValues<T> {

    T get(String str);

    T get(GetName getName);

    int size();

    T[] values();

    <V> V[] valuesAs(V[] v);

    T add(T v);

    T remove(T v);

    T remove(String str);

    T remove(GetName getName);

    void clear();

    void add(T[] vals, boolean clear);

    List<T> search(String... criteria);

    boolean isFixed();

    void setFixed(boolean isFixed);

}