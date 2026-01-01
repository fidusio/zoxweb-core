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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents detailed information about an IoT device.
 * Contains device metadata including manufacturer, model, version, form factor,
 * supported protocols, and available ports.
 *
 * @author mnael
 * @see IOTBase
 * @see ProtocolInfo
 * @see PortInfo
 */
public class DeviceInfo
extends IOTBase
{
    /**
     * Parameters for DeviceInfo configuration.
     */
    public enum Param
            implements GetNVConfig
    {
        /** The device manufacturer name */
        MANUFACTURER(NVConfigManager.createNVConfig("manufacturer", "Manufacturer of the device", "Manufacturer", true, true, String.class)),
        /** The device model identifier */
        MODEL(NVConfigManager.createNVConfig("model", "The model of the device", "Model", false, true, String.class)),
        /** The device version/revision */
        VERSION(NVConfigManager.createNVConfig("version", "The version of the device", "Version", false, true, String.class)),
        /** Supported communication protocols */
        PROTOCOLS(NVConfigManager.createNVConfigEntity("protocols", "Device protocols", "Protocols", true, false, ProtocolInfo.NVC_IOT_PROTOCOL, NVConfigEntity.ArrayType.GET_NAME_MAP)),
        /** Available device ports */
        PORTS(NVConfigManager.createNVConfigEntity("ports", "Device ports", "Ports", true, false, PortInfo.NVC_PORT_INFO, NVConfigEntity.ArrayType.GET_NAME_MAP)),
        /** The physical form factor of the device */
        FORM(NVConfigManager.createNVConfig("form_factor", "Form of the device", "FormFactor", true, true, String.class)),
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

    private static final CanonicalIDSetter CIDS = new CanonicalIDSetter('-',DataConst.DataParam.NAME,
            Param.MODEL, Param.FORM, Param.VERSION);

    /** NVConfigEntity definition for DeviceInfo */
    public static final NVConfigEntity NVC_DEVICE_INFO = new NVConfigEntityPortable("device_info",
            "IOT device information",
            "DeviceInfo",
            true,
            false,
            false,
            false,
            DeviceInfo.class,
            SharedUtil.extractNVConfigs(Param.values()),
            null,
            false,
            IOTBase.NVC_IOT_BASE);

    /**
     * Default constructor.
     */
    public DeviceInfo()
    {
        super(NVC_DEVICE_INFO, CIDS);

    }

    /**
     * Returns the device manufacturer.
     *
     * @return the manufacturer name
     */
    public String getManufacturer()
    {
        return lookupValue(Param.MANUFACTURER);
    }

    /**
     * Sets the device manufacturer.
     *
     * @param manufacturer the manufacturer name
     * @return this instance for method chaining
     */
    public DeviceInfo setManufacturer(String manufacturer)
    {
        setValue(Param.MANUFACTURER, manufacturer);
        return this;
    }

    /**
     * Returns the device model identifier.
     *
     * @return the model name/number
     */
    public String getModel()
    {
        return lookupValue(Param.MODEL);
    }

    /**
     * Sets the device model identifier.
     *
     * @param model the model name/number
     * @return this instance for method chaining
     */
    public DeviceInfo setModel(String model)
    {
        setValue(Param.MODEL, model);
        return this;
    }

    /**
     * Returns the device form factor.
     *
     * @return the form factor description
     */
    public String getForm()
    {
        return lookupValue(Param.FORM);
    }

    /**
     * Sets the device form factor.
     *
     * @param form the form factor description
     * @return this instance for method chaining
     */
    public DeviceInfo setForm(String form)
    {
        setValue(Param.FORM, form);
        return this;
    }

    /**
     * Returns the device version.
     *
     * @return the version string
     */
    public String getVersion()
    {
        return lookupValue(Param.VERSION);
    }

    /**
     * Sets the device version.
     *
     * @param version the version string
     * @return this instance for method chaining
     */
    public DeviceInfo setVersion(String version)
    {
        setValue(Param.VERSION, version);
        return this;
    }

    /**
     * Returns all protocols supported by this device.
     *
     * @return array of ProtocolInfo objects
     */
    public ProtocolInfo[] getProtocols()
    {
        return ((ArrayValues<NVEntity>)lookup(Param.PROTOCOLS)).valuesAs(new ProtocolInfo[0]);
    }

    /**
     * Adds a protocol to this device.
     *
     * @param protocolInfo the protocol to add
     * @return this instance for method chaining
     */
    public DeviceInfo addProtocol(ProtocolInfo protocolInfo)
    {
        ((ArrayValues<NVEntity>)lookup(Param.PROTOCOLS)).add(protocolInfo);

        return this;
    }

    /**
     * Sets the protocols supported by this device.
     *
     * @param protocols the protocols to set
     * @return this instance for method chaining
     */
    public DeviceInfo setProtocols(ProtocolInfo ...protocols)
    {
        ArrayValues<NVEntity> protos =  lookup(Param.PROTOCOLS);
        for(ProtocolInfo proto: protocols)
        {
            protos.add(proto);
        }

        return this;
    }

    /**
     * Returns all ports available on this device.
     *
     * @return array of PortInfo objects
     */
    public PortInfo[] getPorts()
    {
        return ((ArrayValues<NVEntity>)lookup(Param.PORTS)).valuesAs(new PortInfo[0]);
    }

    /**
     * Sets the ports available on this device.
     *
     * @param ports the ports to set
     * @return this instance for method chaining
     */
    public synchronized DeviceInfo setPorts(PortInfo ...ports)
    {
        ArrayValues<NVEntity> portsAV = lookup(Param.PORTS);
        for(PortInfo port :  ports)
        {
            portsAV.add(port);
        }
        return this;
    }

    /**
     * Adds a port to this device.
     * The port's tag is automatically set to the device name.
     *
     * @param port the port to add
     * @return this instance for method chaining
     */
    public DeviceInfo addPort(PortInfo port)
    {

        ((ArrayValues<NVEntity>)lookup(Param.PORTS)).add(port.setTag(getName()));
        //port.setTag(getName());
        return this;
    }

    /**
     * Looks up ports matching the given criteria using case-insensitive contains matching.
     *
     * @param matchCriteria the search criteria
     * @return set of matching PortInfo objects
     */
    public Set<PortInfo> lookupPorts(String matchCriteria)
    {
        return findMatchingPorts(Const.RegEx.CONTAINS_NO_CASE.toRegEx(matchCriteria, true));
    }

    /**
     * Finds ports matching the given regex criteria.
     * Matches against port functions and port names.
     *
     * @param matchCriteria the regex pattern to match
     * @return set of matching PortInfo objects
     */
    public Set<PortInfo> findMatchingPorts(String matchCriteria)
    {
        Set<PortInfo> ret = new LinkedHashSet<>();
        for(PortInfo port : getPorts())
        {
            for(String function : port.getFunctions())
            {
                if (function.matches(matchCriteria) || port.getName().matches(matchCriteria))
                    ret.add(port);
            }
        }

        return ret;
    }

}
