package org.zoxweb.shared.iot;

import com.sun.org.apache.xerces.internal.xs.StringList;
import org.zoxweb.shared.data.PropertyDAO;
import org.zoxweb.shared.util.*;

public class PortInfo
extends IOTBase
{
    public enum Param
            implements GetNVConfig
    {
        PORT(NVConfigManager.createNVConfig("port", "Port/Pin number on the device", "Port/PIN", true, true, int.class)),
        TYPE(NVConfigManager.createNVConfig("type", "Port/Pin type", "Type", false, true, String.class)),
        SPEEDS(NVConfigManager.createNVConfig("speeds", "Port speeds", "Speeds", false, true, StringList.class)),
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

    public static final NVConfigEntity NVC_PORT_INFO = new NVConfigEntityLocal("port_info",
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
            PropertyDAO.NVC_PROPERTY_DAO);


    public PortInfo()
    {
        super(NVC_PORT_INFO);
    }


    public int getPort()
    {
        return (int)lookupValue(Param.PORT);
    }

    public void setPort(int port)
    {
        setValue(Param.PORT, port);
    }
}
