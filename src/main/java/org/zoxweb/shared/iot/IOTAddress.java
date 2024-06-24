package org.zoxweb.shared.iot;

import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.*;

public class IOTAddress
    extends IOTBase
{

    public enum Param
            implements GetNVConfig
    {
        ADDRESS(NVConfigManager.createNVConfig("address", "Address", "Address of the entity", true, true, String.class)),
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

    public static final NVConfigEntity NVC_IOT_ADDRESS = new NVConfigEntityLocal("iot_address",
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
            PropertyDAO.NVC_PROPERTY_DAO);


    public IOTAddress()
    {
        super(NVC_IOT_ADDRESS);
    }
}
