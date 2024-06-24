package org.zoxweb.shared.iot;

import org.zoxweb.shared.data.PropertyDAO;
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
            PropertyDAO.NVC_PROPERTY_DAO);


    public DeviceInfo()
    {
        super(NVC_DEVICE_INFO);

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

    public ProtocolInfo[] getProtocols()
    {
        NVGenericMap protos = lookupSubNVGM("protocols");
        ProtocolInfo[] ret = new ProtocolInfo[protos.size()];
        GetNameValue<?>[] values = protos.values();
        for(int i = 0; i < ret.length; i++)
        {
            ret[i] = (ProtocolInfo) values[i];
        }

        return ret;
    }
    public DeviceInfo addProtocol(ProtocolInfo protocolInfo)
    {
        NVGenericMap protos = lookupSubNVGM("protocols");
        protos.add(protocolInfo);
        return this;
    }

    public void setProtocols(ProtocolInfo ...protocols)
    {
        NVGenericMap protos = lookupSubNVGM("protocols");;
        for(ProtocolInfo proto: protocols)
        {
            protos.build(proto);
        }
    }
}
