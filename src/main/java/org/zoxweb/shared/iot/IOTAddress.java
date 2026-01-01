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

import org.zoxweb.shared.util.*;

/**
 * Represents an IoT device or entity address.
 * Stores address information including the address value, version, and address type
 * (e.g., MAC address, IP address, serial number).
 *
 * @author mnael
 * @see IOTBase
 */
public class IOTAddress
    extends IOTBase
{

    /**
     * Parameters for IOTAddress configuration.
     */
    public enum Param
            implements GetNVConfig
    {
        /** The address value (e.g., MAC, IP, serial) */
        ADDRESS(NVConfigManager.createNVConfig("address", "Address", "Address of the entity", true, true, String.class)),
        /** The version associated with this address */
        VERSION(NVConfigManager.createNVConfig("version", "The version of the device", "Version", false, true, String.class)),
        /** The type of address (e.g., MAC, IPv4, IPv6) */
        ADDRESS_TYPE(NVConfigManager.createNVConfig("address_type", "Type of address", "AddressType", false, true, String.class)),
        ;

        private final NVConfig nvc;

        Param(NVConfig nvc)
        {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig()
        {
            return nvc;
        }
    }

    /** NVConfigEntity definition for IOTAddress */
    public static final NVConfigEntity NVC_IOT_ADDRESS = new NVConfigEntityPortable("iot_address",
            null,
            "IOTAddress",
            true,
            false,
            false,
            false,
            IOTAddress.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            IOTBase.NVC_IOT_BASE);

    /**
     * Default constructor.
     */
    public IOTAddress()
    {
        super(NVC_IOT_ADDRESS, null);
    }
}
