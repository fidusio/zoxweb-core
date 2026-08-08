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

/**
 * Callback contract to monitor the lifecycle of objects: implementations are
 * notified when an object is created and when it is terminated.
 * @param <T> the type of object being monitored
 */
public interface LifeCycleMonitor<T> {
    /**
     * Invoked when the object is created.
     * @param t the created object
     * @return true if the creation notification was accepted
     */
    boolean created(T t);

    /**
     * Invoked when the object is terminated.
     * @param t the terminated object
     * @return true if the termination notification was accepted
     */
    boolean terminated(T t);

}