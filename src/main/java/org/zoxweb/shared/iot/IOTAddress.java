package org.zoxweb.shared.iot;

import org.zoxweb.shared.util.*;

public class IOTAddress
    extends IOTBase
{

    public enum Param
            implements GetNVConfig
    {
        ADDRESS(NVConfigManager.createNVConfig("address", "Address", "Address of the entity", true, true, String.class)),
        VERSION(NVConfigManager.createNVConfig("version", "The version of the device", "Version", false, true, String.class)),
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


    public IOTAddress()
    {
        super(NVC_IOT_ADDRESS, null);
    }
}
