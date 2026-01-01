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

import org.zoxweb.shared.data.DataConst;
import org.zoxweb.shared.util.*;

/**
 * Represents protocol information for an IoT device.
 * Stores details about communication protocols supported by a device,
 * including the protocol name, version, and type.
 *
 * @author mnael
 * @see IOTBase
 * @see DeviceInfo
 */
public class ProtocolInfo
extends IOTBase
{

    /**
     * Parameters for ProtocolInfo configuration.
     */
    public enum Param
            implements GetNVConfig
    {
        /** The version of the protocol */
        VERSION(NVConfigManager.createNVConfig("version", "The version of the protocol", "Version", false, true, String.class)),
        /** The type of the protocol (e.g., MQTT, HTTP, CoAP) */
        TYPE(NVConfigManager.createNVConfig("type", "The type of the protocol", "Type", false, true, String.class)),
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

    private static final CanonicalIDSetter CIDS = new CanonicalIDSetter('-', DataConst.DataParam.NAME, Param.VERSION);

    /** NVConfigEntity definition for ProtocolInfo */
    public static final NVConfigEntity NVC_IOT_PROTOCOL = new NVConfigEntityPortable("iot_protocol",
            null,
            "ProtocolInfo",
            true,
            false,
            false,
            false,
            ProtocolInfo.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            IOTBase.NVC_IOT_BASE);

    /**
     * Default constructor.
     */
    public ProtocolInfo()
    {
        super(NVC_IOT_PROTOCOL, CIDS);
    }

    /**
     * Returns the protocol version.
     *
     * @return the version string
     */
    public String getVersion()
    {
        return lookupValue(Param.VERSION);
    }

    /**
     * Sets the protocol version.
     *
     * @param version the version to set
     */
    public void setVersion(String version)
    {
        setValue(Param.VERSION, version);
    }

    /**
     * Returns the protocol type.
     *
     * @return the type string (e.g., MQTT, HTTP, CoAP)
     */
    public String getType()
    {
        return lookupValue(Param.TYPE);
    }

    /**
     * Sets the protocol type.
     *
     * @param type the type to set
     */
    public void setType(String type)
    {
        setValue(Param.TYPE, type);
    }
}
