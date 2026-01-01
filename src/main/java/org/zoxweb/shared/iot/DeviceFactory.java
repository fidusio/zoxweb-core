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
package org.zoxweb.shared.iot;

import org.zoxweb.shared.util.NVGenericMap;

/**
 * Factory interface for creating IoT device instances.
 * Implementations of this interface provide device-specific creation logic
 * based on configuration parameters.
 *
 * @author mnael
 * @see DeviceInfo
 */
public interface DeviceFactory
{
    /**
     * Creates a device info instance based on the provided parameters.
     * The parameters can be null; based on the implementation, it can return null
     * or a default device configuration.
     *
     * @param param the specifications of the required device as a generic map
     * @return the device info object, or null depending on implementation
     */
    DeviceInfo createDevice(NVGenericMap param);
}
