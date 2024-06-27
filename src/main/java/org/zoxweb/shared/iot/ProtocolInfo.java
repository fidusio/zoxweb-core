package org.zoxweb.shared.iot;

import org.zoxweb.shared.data.DataConst;
import org.zoxweb.shared.util.*;

public class ProtocolInfo
extends IOTBase
{


    public enum Param
            implements GetNVConfig
    {
        VERSION(NVConfigManager.createNVConfig("version", "The version of the protocol", "Version", false, true, String.class)),
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
    public static final NVConfigEntity NVC_IOT_PROTOCOL = new NVConfigEntityLocal("iot_protocol",
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

    public ProtocolInfo()
    {
        super(NVC_IOT_PROTOCOL, CIDS);
    }


    public String getVersion()
    {
        return lookupValue(Param.VERSION);
    }

    public void setVersion(String version)
    {
        setValue(Param.VERSION, version);
    }


    public String getType()
    {
        return lookupValue(Param.TYPE);
    }

    public void setType(String type)
    {
        setValue(Param.TYPE, type);
    }
}
