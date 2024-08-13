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

/**
 * Created by mnael on 3/31/2017.
 * This interface is mainly used to associate a global id like a uuid with an object.
 * The global id is unique globally across APPS and systems it can be used as a uniqueexchange identifier
 */
public interface GlobalID<T>
    extends GlobalIDBase<T>
{
    /**
     * @param gid the global identifier of the object uuid is string format.
     */
    void setGUID(T gid);
}