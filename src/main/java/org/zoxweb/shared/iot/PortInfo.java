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
 * Represents port or pin information for an IoT device.
 * Stores details about physical or logical ports/pins including
 * port number, type, supported speeds, and available functions.
 *
 * @author mnael
 * @see IOTBase
 * @see DeviceInfo
 */
public class PortInfo
extends IOTBase
{
    /**
     * Parameters for PortInfo configuration.
     */
    public enum Param
            implements GetNVConfig
    {
        /** The port or pin number on the device */
        PORT(NVConfigManager.createNVConfig("port", "Port/Pin number on the device", "Port/PIN", true, true, int.class)),
        /** The type of port (e.g., GPIO, UART, SPI, I2C) */
        TYPE(NVConfigManager.createNVConfig("type", "Port/Pin type", "Type", false, true, String.class)),
        /** A tag for associating this port with a device */
        TAG(NVConfigManager.createNVConfig("port_tag", "Port tag", "Tag", false, true, String.class)),
        /** Supported communication speeds for this port */
        SPEEDS(NVConfigManager.createNVConfig("speeds", "Port speeds", "Speeds", false, true, NVStringList.class)),
        /** Available functions that this port can perform */
        FUNCTIONS(NVConfigManager.createNVConfig("functions", "Port functions", "Functions", false, true, NVStringList.class)),
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

    /** NVConfigEntity definition for PortInfo */
    public static final NVConfigEntity NVC_PORT_INFO = new NVConfigEntityPortable("port_info",
            null,
            "PortInfo",
            true,
            false,
            false,
            false,
            PortInfo.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            IOTBase.NVC_IOT_BASE);

    private static final CanonicalIDSetter CIDS = new CanonicalIDSetter('-', Param.TAG, Param.PORT, DataConst.DataParam.NAME, Param.TYPE);

    /**
     * Default constructor.
     */
    public PortInfo()
    {
        super(NVC_PORT_INFO, CIDS);
    }

    /**
     * Constructor with name and description.
     *
     * @param name the port name
     * @param description the port description
     */
    public PortInfo(String name, String description)
    {
        this();
        setName(name);
        setDescription(description);
    }


    /**
     * Returns the port/pin number.
     *
     * @return the port number
     */
    public int getPort()
    {
        return lookupValue(Param.PORT);
    }

    /**
     * Sets the port/pin number.
     *
     * @param port the port number to set
     * @return this instance for method chaining
     */
    public PortInfo setPort(int port)
    {
        setValue(Param.PORT, port);
        return this;
    }

    /**
     * Returns the port type.
     *
     * @return the type string (e.g., GPIO, UART, SPI, I2C)
     */
    public String getType()
    {
        return lookupValue(Param.TYPE);
    }

    /**
     * Sets the port type.
     *
     * @param type the type to set
     * @return this instance for method chaining
     */
    public PortInfo setType(String type)
    {
        setValue(Param.TYPE, type);
        return this;
    }

    /**
     * Returns all supported speeds for this port.
     *
     * @return array of speed strings
     */
    public String[] getSpeeds()
    {
        return ((NVStringList)lookup(Param.SPEEDS)).getValues();
    }

    /**
     * Sets the supported speeds for this port.
     *
     * @param speeds the speeds to set
     * @return this instance for method chaining
     */
    public synchronized PortInfo setSpeeds(String ...speeds)
    {
        NVStringList speedsAV = lookup(Param.SPEEDS);
        for(String speed: speeds)
            speedsAV.add(speed);
        return this;
    }

    /**
     * Adds a supported speed to this port.
     *
     * @param speed the speed to add
     * @return this instance for method chaining
     */
    public PortInfo addSpeed(String speed)
    {
        ((NVStringList)lookup(Param.SPEEDS)).add(speed);
        return this;
    }

    /**
     * Returns all available functions for this port.
     *
     * @return array of function strings
     */
    public String[] getFunctions()
    {
        return ((NVStringList)lookup(Param.FUNCTIONS)).getValues();
    }

    /**
     * Sets the available functions for this port.
     *
     * @param functions the functions to set
     * @return this instance for method chaining
     */
    public synchronized PortInfo setFunctions(String ...functions)
    {
        NVStringList functionsNVSL = lookup(Param.FUNCTIONS);
        for(String function: functions)
            functionsNVSL.add(function);

        return this;
    }

    /**
     * Adds a function to this port.
     *
     * @param function the function to add
     * @return this instance for method chaining
     */
    public PortInfo addFunction(String function)
    {
        ((NVStringList)lookup(Param.FUNCTIONS)).add(function);
        return this;
    }

    /**
     * Returns the port tag.
     * The tag is typically used to associate this port with a device.
     *
     * @return the tag string
     */
    public String getTag()
    {
        return lookupValue(Param.TAG);
    }

    /**
     * Sets the port tag.
     *
     * @param tag the tag to set
     * @return this instance for method chaining
     */
    public PortInfo setTag(String tag)
    {
        setValue(Param.TAG, tag);
        return this;
    }





}