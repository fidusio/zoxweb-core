package org.zoxweb.shared.iot;

import org.zoxweb.shared.data.DataConst;
import org.zoxweb.shared.util.*;

public class DeviceInfo
extends IOTBase
{
    public enum Param
            implements GetNVConfig
    {
        MANUFACTURER(NVConfigManager.createNVConfig("manufacturer", "Manufacturer of the device", "Manufacturer", true, true, String.class)),
        MODEL(NVConfigManager.createNVConfig("model", "The model of the device", "Model", false, true, String.class)),
        VERSION(NVConfigManager.createNVConfig("version", "The version of the device", "Version", false, true, String.class)),
        PROTOCOLS(NVConfigManager.createNVConfigEntity("protocols", "Device protocols", "Protocols", true, false, ProtocolInfo.NVC_IOT_PROTOCOL, NVConfigEntity.ArrayType.GET_NAME_MAP)),
        PORTS(NVConfigManager.createNVConfigEntity("ports", "Device ports", "Ports", true, false, PortInfo.NVC_PORT_INFO, NVConfigEntity.ArrayType.GET_NAME_MAP)),
        //PROTOCOLS(NVConfigManager.createNVConfig("protocols", "Supported protocols by the device", "Protocols", true, true, NVGenericMap.class)),
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
            Param.MODEL, Param.VERSION);

    public static final NVConfigEntity NVC_DEVICE_INFO = new NVConfigEntityLocal("device_info",
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


    public DeviceInfo()
    {
        super(NVC_DEVICE_INFO, CIDS);

    }

    public String getManufacturer()
    {
        return lookupValue(Param.MANUFACTURER);
    }

    public void setManufacturer(String manufacturer)
    {
        setValue(Param.MANUFACTURER, manufacturer);
    }

    public String getModel()
    {
        return lookupValue(Param.MODEL);
    }

    public void setModel(String model)
    {
        setValue(Param.MODEL, model);
    }

    public String getVersion()
    {
        return lookupValue(Param.VERSION);
    }

    public void setVersion(String version)
    {
        setValue(Param.VERSION, version);
    }

    public ArrayValues<ProtocolInfo> getProtocols()
    {
        return lookup(Param.PROTOCOLS);
    }
    public DeviceInfo addProtocol(ProtocolInfo protocolInfo)
    {
        getProtocols().add(protocolInfo);

        return this;
    }

    public void setProtocols(ProtocolInfo ...protocols)
    {
        ArrayValues<ProtocolInfo> protos =  getProtocols();
        for(ProtocolInfo proto: protocols)
        {
            protos.add(proto);
        }
    }


    public ArrayValues<PortInfo> getPorts()
    {
        return lookup(Param.PORTS);
    }

    public synchronized void setPorts(PortInfo ...ports)
    {
        for(PortInfo port :  ports)
        {
           addPort(port);
        }
    }

    public DeviceInfo addPort(PortInfo port)
    {

        getPorts().add(port.setTag(getName()));
        //port.setTag(getName());
        return this;
    }

}
